package dev.steamcraft.common;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.steamcraft.core.identity.PlayerUuids;
import dev.steamcraft.core.identity.SteamIds;
//? if >=1.21.11
/*import net.minecraft.commands.Commands;*/
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class ServerCommands {
	private ServerCommands() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(literal("steamcraft")
				.requires(ServerCommands::isAdmin)
				.then(literal("status").executes(ServerCommands::status))
				.then(literal("whois").then(argument("player", StringArgumentType.word())
						.suggests((c, b) -> SharedSuggestionProvider.suggest(c.getSource().getOnlinePlayerNames(), b))
						.executes(ServerCommands::whois))));
	}

	private static boolean isAdmin(CommandSourceStack source) {
		//? if >=1.21.11 {
		/*if (Commands.hasPermission(Commands.LEVEL_ADMINS).test(source)) return true;
		*///?} else {
		if (source.hasPermission(3)) return true;
		//?}
		ServerPlayer player = source.getPlayer();
		return player != null && Profiles.isOwner(source.getServer(), player.getGameProfile());
	}

	private static SteamCraftServer context() throws CommandSyntaxException {
		SteamCraftServer context = SteamCraftServer.current();
		if (context == null) throw new SimpleCommandExceptionType(Component.literal("SteamCraft is not running on this server.")).create();
		return context;
	}

	private static int status(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
		SteamCraftServer context = context();
		MinecraftServer server = c.getSource().getServer();
		StringBuilder text = new StringBuilder("SteamCraft status:\n sign-in: ");
		if (!server.isDedicatedServer()) {
			text.append("Mojang; guests from Steam sign in through Steam");
		} else if (context.steamMode()) {
			text.append("Steam (online-mode=false); lists in steamcraft/");
		} else {
			text.append("Mojang (online-mode=true), SteamCraft inactive");
		}
		text.append("\n Steam: ").append(context.steamStatus());
		if (!server.isDedicatedServer()) {
			String hosting = context.isHostingOverSteam() ? "open to " + context.access().id()
					: context.tunnelHost() != null ? "closed to new players" : "off";
			text.append("\n Steam co-op: ").append(hosting);
			if (context.tunnelHost() != null) text.append(context.cheats() ? ", cheats on" : ", cheats off");
			text.append("\n operators: ").append(context.operators().size());
		}
		reply(c, text.toString());
		return 1;
	}

	private static int whois(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
		SteamCraftServer context = context();
		String name = StringArgumentType.getString(c, "player");
		ServerPlayer player = c.getSource().getServer().getPlayerList().getPlayerByName(name);
		Long verified = player != null ? context.steamIdOf(player.getGameProfile()) : null;
		Long known = context.steamIdOfName(name);
		StringBuilder text = new StringBuilder(name).append(":");
		if (player != null) {
			text.append("\n UUID: ").append(player.getUUID()).append(" (online)");
		} else if (known != null) {
			text.append("\n UUID: ").append(PlayerUuids.fromSteam(known)).append(" (offline)");
		}
		if (verified != null) {
			text.append("\n Steam: ").append(SteamIds.format(verified)).append(" (signed in through Steam)");
		} else if (known != null) {
			text.append("\n Steam: ").append(SteamIds.format(known)).append(" (last played under this name)");
		} else {
			text.append("\n Steam: none");
		}
		reply(c, text.toString());
		return 1;
	}

	private static void reply(CommandContext<CommandSourceStack> c, String text) {
		c.getSource().sendSuccess(() -> Component.literal(text), false);
	}
}
