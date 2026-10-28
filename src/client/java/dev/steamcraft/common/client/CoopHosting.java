package dev.steamcraft.common.client;

import dev.steamcraft.common.SteamCraftServer;
import dev.steamcraft.common.SteamRuntime;
//? if <26.2
import dev.steamcraft.common.client.mixin.IntegratedServerAccessor;
import dev.steamcraft.core.config.CoopAccess;
import dev.steamcraft.core.identity.SteamIds;
import dev.steamcraft.core.steam.SteamClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
//? if >=26.2
/*import net.minecraft.server.MinecraftServer;*/
import net.minecraft.util.HttpUtil;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;

public final class CoopHosting {
	public enum Problem {
		NO_STEAM,
		NO_WORLD,
		LAN_FAILED
	}

	public static final class Failure extends Exception {
		public final Problem problem;

		Failure(Problem problem) {
			super(problem.name());
			this.problem = problem;
		}
	}

	private CoopHosting() {
	}

	public static SteamCraftServer hostedWorld() {
		return Minecraft.getInstance().getSingleplayerServer() == null ? null : SteamCraftServer.current();
	}

	public static boolean isOpen() {
		SteamCraftServer context = hostedWorld();
		return context != null && context.isHostingOverSteam();
	}

	public static void open(CoopAccess access, boolean cheats, GameType guestMode, Difficulty difficulty) throws Failure {
		IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
		SteamCraftServer context = hostedWorld();
		if (SteamRuntime.client() == null) throw new Failure(Problem.NO_STEAM);
		if (server == null || context == null) throw new Failure(Problem.NO_WORLD);

		if (!server.isPublished() && !publish(server, guestMode != null ? guestMode : server.getDefaultGameType())) {
			throw new Failure(Problem.LAN_FAILED);
		}
		if (guestMode != null) setGuestMode(server, guestMode);
		if (difficulty != null && difficulty != server.getWorldData().getDifficulty()) {
			server.execute(() -> server.setDifficulty(difficulty, false));
		}
		context.startTunnelHost(server.getPort(), access, cheats);
	}

	private static boolean publish(IntegratedServer server, GameType mode) {
		//? if >=26.3 {
		/*return server.publishServer(MinecraftServer.MultiplayerScope.LAN, false, HttpUtil.getAvailablePort());
		*///?} else if >=26.2 {
		/*return server.publishServer(MinecraftServer.MultiplayerScope.LAN, mode, false, HttpUtil.getAvailablePort());
		*///?} else {
		return server.publishServer(mode, false, HttpUtil.getAvailablePort());
		//?}
	}

	private static void setGuestMode(IntegratedServer server, GameType mode) {
		//? if >=26.3 {
		/*server.execute(() -> server.setDefaultGameType(mode));
		*///?} else if >=26.2 {
		/*server.setGameTypeForOtherPlayers(mode);
		*///?} else {
		((IntegratedServerAccessor) server).steamcraft$setPublishedGameType(mode);
		//?}
	}

	public static void close() {
		SteamCraftServer context = hostedWorld();
		if (context != null) context.closeTunnelToNewPlayers();
	}

	public static GameType guestMode() {
		IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
		if (server == null) return GameType.SURVIVAL;
		GameType forced = server.getForcedGameType();
		return forced != null ? forced : server.getDefaultGameType();
	}

	public static String nameOf(SteamCraftServer context, long steamId) {
		String name = context != null ? context.nameOf(steamId) : null;
		if (name == null) name = SteamPeople.personaName(steamId);
		return name != null ? name : SteamIds.format(steamId);
	}

	public static boolean isMe(long steamId) {
		SteamClient steam = SteamRuntime.client();
		return steam != null && steam.localSteamId() == steamId;
	}
}
