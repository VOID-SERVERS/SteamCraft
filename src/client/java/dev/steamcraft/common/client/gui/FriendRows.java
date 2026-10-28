package dev.steamcraft.common.client.gui;

import dev.steamcraft.common.client.SteamPeople;
import dev.steamcraft.core.config.CoopAccess;
import dev.steamcraft.core.steam.SteamCraftPresence;
import dev.steamcraft.core.steam.SteamFriend;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

final class FriendRows {
	static final int GREEN = 0xFF8FC93A;
	static final int BLUE = 0xFF57CBDE;
	static final int DIM_BLUE = 0xFF3D7F8C;
	static final int GREY = 0xFF6E6E6E;
	static final int YELLOW = 0xFFE0C040;

	private FriendRows() {
	}

	static Component status(SteamFriend friend) {
		SteamCraftPresence.Activity activity = friend.activity;
		if (activity != null) {
			MutableComponent line = where(activity).copy();
			if (activity.mode != null) line.append(" · ").append(gameMode(activity.mode));
			if (activity.day > 0) line.append(" · ").append(Component.translatable("steamcraft.activity.day", activity.day));
			return line;
		}
		if (friend.inGame) return Component.translatable("steamcraft.friend.in_game");
		switch (friend.status) {
			case AWAY:
				return Component.translatable("steamcraft.friend.away");
			case BUSY:
				return Component.translatable("steamcraft.friend.busy");
			case OFFLINE:
				return Component.translatable("steamcraft.friend.offline");
			default:
				return Component.translatable("steamcraft.friend.online");
		}
	}

	static Component details(SteamFriend friend) {
		SteamCraftPresence.Activity activity = friend.activity;
		if (activity == null) return null;
		MutableComponent text = where(activity).copy();
		if (activity.worldName != null) line(text, "steamcraft.activity.name", Component.literal(activity.worldName));
		if (activity.mode != null) line(text, "steamcraft.activity.mode", gameMode(activity.mode));
		if (activity.dimension != null) line(text, "steamcraft.activity.dimension", dimension(activity.dimension));
		if (activity.day > 0) line(text, "steamcraft.activity.day.long", activity.day);
		if (activity.players > 0) line(text, "steamcraft.activity.players", activity.players);
		return text;
	}

	private static void line(MutableComponent text, String key, Object value) {
		text.append("\n").append(Component.translatable(key, value).withStyle(ChatFormatting.GRAY));
	}

	private static Component where(SteamCraftPresence.Activity activity) {
		if (activity.hosting != null) return Component.translatable("steamcraft.friend.hosting." + activity.hosting.id());
		switch (activity.where) {
			case OWN_WORLD:
				return Component.translatable("steamcraft.activity.own_world");
			case STEAM_GUEST: {
				String host = activity.host != 0 ? SteamPeople.personaName(activity.host) : null;
				return host != null ? Component.translatable("steamcraft.activity.guest_of", host) : Component.translatable("steamcraft.activity.guest");
			}
			case SERVER:
				return activity.worldName != null ? Component.translatable("steamcraft.activity.server_named", activity.worldName)
						: Component.translatable("steamcraft.activity.server");
			default:
				return Component.translatable("steamcraft.activity.menu");
		}
	}

	private static Component gameMode(String mode) {
		return Component.translatable("selectWorld.gameMode." + mode);
	}

	private static Component dimension(String id) {
		switch (id) {
			case "minecraft:overworld":
				return Component.translatable("steamcraft.dimension.overworld");
			case "minecraft:the_nether":
				return Component.translatable("steamcraft.dimension.the_nether");
			case "minecraft:the_end":
				return Component.translatable("steamcraft.dimension.the_end");
			default:
				return Component.literal(id);
		}
	}

	static int dot(SteamFriend friend) {
		if (!friend.isOnline()) return GREY;
		if (friend.playingSteamCraft || friend.inGame) return GREEN;
		return friend.status == SteamFriend.Status.ONLINE ? BLUE : DIM_BLUE;
	}

	static List<SteamFriend> sorted(List<SteamFriend> friends) {
		List<SteamFriend> sorted = new ArrayList<>(friends);
		sorted.sort(Comparator.comparingInt(FriendRows::rank).thenComparing(f -> f.name.toLowerCase(Locale.ROOT)));
		return sorted;
	}

	private static int rank(SteamFriend friend) {
		if (friend.hosting != null) return 0;
		if (friend.playingSteamCraft) return 1;
		if (!friend.isOnline()) return 5;
		if (friend.inGame) return 2;
		return friend.status == SteamFriend.Status.ONLINE ? 3 : 4;
	}

	static boolean matches(String search, String... names) {
		String query = search.trim().toLowerCase(Locale.ROOT);
		if (query.isEmpty()) return true;
		for (String name : names) {
			if (name != null && name.toLowerCase(Locale.ROOT).contains(query)) return true;
		}
		return false;
	}

	static Component access(CoopAccess access) {
		return Component.translatable("steamcraft.access." + access.id());
	}
}
