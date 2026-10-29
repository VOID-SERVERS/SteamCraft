package dev.steamcraft.common.client;

import dev.steamcraft.common.SteamCraftServer;
import dev.steamcraft.common.Profiles;
import dev.steamcraft.common.SteamRuntime;
import dev.steamcraft.core.config.CoopAccess;
import dev.steamcraft.core.identity.SteamIds;
import dev.steamcraft.core.steam.SteamClient;
import dev.steamcraft.core.steam.SteamCraftPresence;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.level.GameType;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class PresencePublisher {
	private static final int CHECK_EVERY_TICKS = 20;

	private static int ticks;
	private static Map<String, String> published;
	private static String publishedConnect;

	private PresencePublisher() {
	}

	public static void tick() {
		if (++ticks < CHECK_EVERY_TICKS) return;
		ticks = 0;
		SteamClient steam = SteamRuntime.client();
		if (steam == null) return;
		Minecraft minecraft = Minecraft.getInstance();
		SteamCraftPresence.Activity activity = activity(minecraft);
		Map<String, String> values = SteamCraftPresence.encode(activity);
		String connect = activity.hosting != null && activity.hosting != CoopAccess.INVITE ? SteamIds.toAddress(steam.localSteamId()) : null;
		if (values.equals(published) && Objects.equals(connect, publishedConnect)) return;
		published = values;
		publishedConnect = connect;
		steam.setPresence(activity, connect);
	}

	private static SteamCraftPresence.Activity activity(Minecraft minecraft) {
		ClientPacketListener connection = minecraft.getConnection();
		if (minecraft.level == null || minecraft.player == null || connection == null) return SteamCraftPresence.Activity.menu();
		boolean showName = SteamRuntime.config().presence.showWorldName;
		IntegratedServer own = minecraft.getSingleplayerServer();
		ServerData server = minecraft.getCurrentServer();
		Long host = server != null ? hostOf(server.ip) : null;

		SteamCraftPresence.Where where;
		String name = null;
		CoopAccess hosting = null;
		if (own != null) {
			where = SteamCraftPresence.Where.OWN_WORLD;
			if (showName) name = own.getWorldData().getLevelName();
			SteamCraftServer context = SteamCraftServer.current();
			if (context != null && context.isHostingOverSteam()) hosting = context.access();
		} else if (host != null) {
			where = SteamCraftPresence.Where.STEAM_GUEST;
		} else {
			where = SteamCraftPresence.Where.SERVER;
			if (showName && server != null) name = server.ip;
		}
		GameType mode = minecraft.gameMode != null ? minecraft.gameMode.getPlayerMode() : null;
		return new SteamCraftPresence.Activity(where, hosting,
				mode != null ? mode.getName().toLowerCase(Locale.ROOT) : null,
				connection.getOnlinePlayers().size(),
				minecraft.level.getDayTime() / 24000L + 1,
				//? if >=1.21.11 {
				/*minecraft.level.dimension().identifier().toString(),
				*///?} else {
				minecraft.level.dimension().location().toString(),
				//?}
				name,
				host != null ? host : 0,
				hosting != null ? Profiles.name(minecraft.player.getGameProfile()) : null);
	}

	public static Long currentHost() {
		ServerData server = Minecraft.getInstance().getCurrentServer();
		return server != null ? hostOf(server.ip) : null;
	}

	private static Long hostOf(String address) {
		if (address == null) return null;
		int colon = address.lastIndexOf(':');
		return SteamIds.fromAddressHost(colon > 0 ? address.substring(0, colon) : address);
	}
}
