package dev.steamcraft.common.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.steamcraft.core.access.Invites;
import dev.steamcraft.core.config.CoopAccess;
import dev.steamcraft.core.identity.SteamIds;
import dev.steamcraft.core.steam.SteamClient;
import dev.steamcraft.common.SteamCraftServer;
import dev.steamcraft.common.SteamRuntime;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class ClientCommands<S> {
	private final CommandFeedback<S> feedback;
	private final SkinCommands<S> skins;

	private ClientCommands(CommandFeedback<S> feedback) {
		this.feedback = feedback;
		this.skins = new SkinCommands<>(feedback);
	}

	public static <S> void register(CommandDispatcher<S> dispatcher, CommandFeedback<S> feedback) {
		dispatcher.register(new ClientCommands<>(feedback).steam());
	}

	private LiteralArgumentBuilder<S> steam() {
		return literal("steam")
				.then(literal("host")
						.executes(c -> host(c, SteamRuntime.config().coop.access, false))
						.then(literal("cheats").executes(c -> host(c, SteamRuntime.config().coop.access, true)))
						.then(argument("access", StringArgumentType.word())
								.suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(CoopAccess.values()).map(CoopAccess::id), b))
								.executes(c -> hostWithAccess(c, false))
								.then(literal("cheats").executes(c -> hostWithAccess(c, true)))))
				.then(literal("invite").then(argument("friend", StringArgumentType.greedyString())
						.suggests((c, b) -> SharedSuggestionProvider.suggest(friends().values(), b))
						.executes(this::invite)))
				.then(literal("unhost").executes(this::unhost))
				.then(literal("op").then(argument("player", StringArgumentType.greedyString())
						.suggests((c, b) -> SharedSuggestionProvider.suggest(operatorCandidates(), b))
						.executes(this::op)))
				.then(literal("deop").then(argument("player", StringArgumentType.greedyString())
						.suggests((c, b) -> SharedSuggestionProvider.suggest(operatorNames(), b))
						.executes(this::deop)))
				.then(literal("ops").executes(this::ops))
				.then(literal("id").executes(this::id))
				.then(skins.skin())
				.then(skins.cape());
	}

	private LiteralArgumentBuilder<S> literal(String name) {
		return LiteralArgumentBuilder.literal(name);
	}

	private <T> RequiredArgumentBuilder<S, T> argument(String name, ArgumentType<T> type) {
		return RequiredArgumentBuilder.argument(name, type);
	}

	private int hostWithAccess(CommandContext<S> c, boolean cheats) {
		CoopAccess access;
		try {
			access = CoopAccess.byId(StringArgumentType.getString(c, "access"));
		} catch (IllegalArgumentException e) {
			return fail(c, "Unknown access. Use everyone, friends or invite.");
		}
		return host(c, access, cheats);
	}

	private int host(CommandContext<S> c, CoopAccess access, boolean cheats) {
		try {
			CoopHosting.open(access, cheats, null, null);
		} catch (CoopHosting.Failure e) {
			switch (e.problem) {
				case NO_STEAM:
					return fail(c, "Steam is not running, or SteamCraft could not connect to it.");
				case NO_WORLD:
					return fail(c, "Open a singleplayer world first.");
				default:
					return fail(c, "Could not open the world to LAN.");
			}
		}

		feedback.info(c.getSource(), Component.literal("Your world is open through Steam to " + describe(access) + ".").withStyle(ChatFormatting.GREEN));
		if (access == CoopAccess.INVITE) {
			feedback.info(c.getSource(), Component.literal("Invite friends with /steam invite <name>; an invite is valid for "
					+ Invites.VALID_MINUTES + " minutes."));
		} else {
			feedback.info(c.getSource(), Component.literal("Friends can click \"Join game\" in Steam, or find your world under Multiplayer > SteamCraft."));
		}
		feedback.info(c.getSource(), Component.literal(cheats
				? "Cheats are on for you and your operators (/steam op <player>)."
				: "Cheats are off: you and your operators (/steam op <player>) can use /kick and /say. For cheats, add \"cheats\" to /steam host."));
		return 1;
	}

	private static String describe(CoopAccess access) {
		switch (access) {
			case EVERYONE:
				return "everyone";
			case FRIENDS:
				return "your Steam friends";
			case INVITE:
				return "invited players only";
			default:
				throw new IllegalStateException();
		}
	}

	private int invite(CommandContext<S> c) {
		SteamCraftServer context = SteamCraftServer.current();
		if (SteamRuntime.client() == null) return fail(c, "Steam is not running, or SteamCraft could not connect to it.");
		if (context == null || !context.isHostingOverSteam()) return fail(c, "Open your world with /steam host first.");
		String query = StringArgumentType.getString(c, "friend").trim();
		List<Map.Entry<Long, String>> matches = friends().entrySet().stream()
				.filter(e -> e.getValue().equalsIgnoreCase(query) || SteamIds.format(e.getKey()).equals(query))
				.collect(Collectors.toList());
		if (matches.isEmpty()) return fail(c, "No Steam friend called " + query + ".");
		if (matches.size() > 1) {
			return fail(c, "Several Steam friends are called " + query + ": "
					+ matches.stream().map(e -> SteamIds.format(e.getKey())).collect(Collectors.joining(", ")) + ". Invite one by SteamID64.");
		}
		Map.Entry<Long, String> friend = matches.get(0);
		if (!context.invite(friend.getKey())) return fail(c, "Steam could not send the invite to " + friend.getValue() + ".");
		feedback.info(c.getSource(), Component.literal("Invited " + friend.getValue() + ". The invite is valid for "
				+ Invites.VALID_MINUTES + " minutes.").withStyle(ChatFormatting.GREEN));
		return 1;
	}

	private static Map<Long, String> friends() {
		SteamClient steam = SteamRuntime.client();
		return steam == null ? Collections.emptyMap() : steam.friends();
	}

	private int unhost(CommandContext<S> c) {
		SteamCraftServer context = SteamCraftServer.current();
		if (context == null || !context.isHostingOverSteam()) return fail(c, "Your world is not open through Steam.");
		context.closeTunnelToNewPlayers();
		feedback.info(c.getSource(), Component.literal("Steam hosting stopped. Players already in the world stay connected."));
		return 1;
	}

	private int op(CommandContext<S> c) {
		SteamCraftServer context = CoopHosting.hostedWorld();
		if (context == null) return fail(c, "Open your singleplayer world first: operators belong to it.");
		String query = StringArgumentType.getString(c, "player").trim();
		List<Long> matches = findSteamPlayer(context, query);
		if (matches.isEmpty()) return fail(c, "No Steam player or Steam friend called " + query + ".");
		if (matches.size() > 1) {
			return fail(c, "Several Steam friends are called " + query + ": "
					+ matches.stream().map(SteamIds::format).collect(Collectors.joining(", ")) + ". Choose one by SteamID64.");
		}
		long steamId = matches.get(0);
		if (CoopHosting.isMe(steamId)) return fail(c, "You are the host: you already have these rights.");
		String name = CoopHosting.nameOf(context, steamId);
		if (!context.op(steamId, name)) return fail(c, name + " is already an operator of this world.");
		feedback.info(c.getSource(), Component.literal(name + " is now an operator of this world. " + rightsNow(context)).withStyle(ChatFormatting.GREEN));
		return 1;
	}

	private int deop(CommandContext<S> c) {
		SteamCraftServer context = CoopHosting.hostedWorld();
		if (context == null) return fail(c, "Open your singleplayer world first: operators belong to it.");
		String query = StringArgumentType.getString(c, "player").trim();
		Long steamId = context.operators().entrySet().stream()
				.filter(e -> query.equalsIgnoreCase(e.getValue()) || SteamIds.format(e.getKey()).equals(query))
				.map(Map.Entry::getKey)
				.findFirst()
				.orElse(null);
		if (steamId == null) return fail(c, query + " is not an operator of this world.");
		String name = CoopHosting.nameOf(context, steamId);
		context.deop(steamId);
		feedback.info(c.getSource(), Component.literal(name + " is no longer an operator of this world."));
		return 1;
	}

	private int ops(CommandContext<S> c) {
		SteamCraftServer context = CoopHosting.hostedWorld();
		if (context == null) return fail(c, "Open your singleplayer world first: operators belong to it.");
		Map<Long, String> operators = context.operators();
		if (operators.isEmpty()) {
			feedback.info(c.getSource(), Component.literal("This world has no operators. Add one with /steam op <player>."));
			return 1;
		}
		MutableComponent list = Component.literal("Operators of this world: ");
		boolean first = true;
		for (Map.Entry<Long, String> operator : operators.entrySet()) {
			if (!first) list.append(", ");
			first = false;
			String steamId = SteamIds.format(operator.getKey());
			list.append(Component.literal(operator.getValue() != null ? operator.getValue() : steamId).withStyle(style -> style
					.withHoverEvent(ChatEvents.showText(Component.literal("SteamID64: " + steamId)))));
		}
		feedback.info(c.getSource(), list);
		feedback.info(c.getSource(), Component.literal(rightsNow(context)));
		return 1;
	}

	private static List<Long> findSteamPlayer(SteamCraftServer context, String query) {
		Long known = context.steamIdOfPlayer(query);
		if (known != null) return Collections.singletonList(known);
		return friends().entrySet().stream()
				.filter(e -> e.getValue().equalsIgnoreCase(query))
				.map(Map.Entry::getKey)
				.collect(Collectors.toList());
	}

	private static String rightsNow(SteamCraftServer context) {
		if (context.tunnelHost() == null) return "Rights apply once you open the world with /steam host.";
		return context.cheats() ? "Operators have cheats, like you." : "Operators can use /kick and /say, like you; cheats are off.";
	}

	private static List<String> operatorCandidates() {
		SteamCraftServer context = CoopHosting.hostedWorld();
		if (context == null) return Collections.emptyList();
		Set<String> names = new LinkedHashSet<>(context.steamPlayerNames());
		names.addAll(friends().values());
		names.removeAll(context.operators().values());
		return new ArrayList<>(names);
	}

	private static Collection<String> operatorNames() {
		SteamCraftServer context = CoopHosting.hostedWorld();
		return context == null ? Collections.emptyList() : context.operators().values();
	}

	private int id(CommandContext<S> c) {
		SteamClient steam = SteamRuntime.client();
		if (steam == null) return fail(c, "Steam is not running, or SteamCraft could not connect to it.");
		feedback.info(c.getSource(), Component.literal("Steam: " + steam.personaName() + " ")
				.append(copyable(SteamIds.format(steam.localSteamId()))));
		return 1;
	}

	private static MutableComponent copyable(String text) {
		return Component.literal(text).withStyle(style -> style
				.withColor(ChatFormatting.AQUA)
				.withUnderlined(true)
				.withClickEvent(ChatEvents.copy(text))
				.withHoverEvent(ChatEvents.showText(Component.literal("Click to copy"))));
	}

	private int fail(CommandContext<S> c, String message) {
		feedback.error(c.getSource(), Component.literal(message));
		return 0;
	}
}
