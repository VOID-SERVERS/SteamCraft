package dev.steamcraft.common;

import com.mojang.authlib.GameProfile;
import dev.steamcraft.core.Log;
import dev.steamcraft.core.access.CoopGate;
import dev.steamcraft.core.access.CoopOperators;
import dev.steamcraft.core.access.Invites;
import dev.steamcraft.core.access.Knocks;
import dev.steamcraft.core.access.NameClaims;
import dev.steamcraft.core.auth.ActiveSessions;
import dev.steamcraft.core.auth.LoginGate;
import dev.steamcraft.core.auth.TicketValidator;
import dev.steamcraft.core.config.CoopAccess;
import dev.steamcraft.core.config.SteamCraftConfig;
import dev.steamcraft.core.identity.PlayerUuids;
import dev.steamcraft.core.identity.SteamIds;
import dev.steamcraft.core.skin.SkinStore;
import dev.steamcraft.core.steam.SteamClient;
import dev.steamcraft.core.steam.SteamServer;
import dev.steamcraft.core.steam.SteamUnavailableException;
import dev.steamcraft.core.tunnel.PeerRegistry;
import dev.steamcraft.core.tunnel.TunnelHost;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.LongConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class SteamCraftServer {
	private static final String STEAM_DIR = "steamcraft";
	private static volatile SteamCraftServer current;

	private final MinecraftServer server;
	private final boolean steamMode;
	private final NameClaims names;
	private final LoginGate gate;
	private final ActiveSessions sessions = new ActiveSessions();
	private final PeerRegistry peers = new PeerRegistry();
	private final Invites invites = new Invites();
	private final Knocks knocks = new Knocks();
	private static volatile LongConsumer knockListener = steamId -> {
	};
	private final SkinStore skins = new SkinStore();
	private final SteamClient hostSteam;
	private final SteamServer steamServer;
	private final TicketValidator validator;
	private final CoopOperators operators;
	private TunnelHost tunnelHost;
	private volatile CoopAccess access;
	private volatile boolean cheats;

	private SteamCraftServer(MinecraftServer server) {
		this.server = server;
		SteamCraftConfig config = SteamRuntime.config();
		steamMode = isSteamMode(server);
		Path dir = server.isDedicatedServer() ? steamDir(server) : server.getWorldPath(LevelResource.ROOT).resolve(STEAM_DIR);
		names = new NameClaims(dir.resolve("names.json"));
		operators = server.isDedicatedServer() ? null : new CoopOperators(dir.resolve("ops.json"));
		hostSteam = server.isDedicatedServer() ? null : SteamRuntime.client();
		steamServer = steamMode ? startGameServer(server, config) : null;
		validator = hostSteam != null ? hostSteam.validator() : steamServer != null ? steamServer.validator() : null;
		gate = new LoginGate(names, validator);
		access = config.coop.access;
		if (validator != null) validator.setRevocationListener(this::onSteamSessionRevoked);

		if (!server.isDedicatedServer()) return;
		if (!steamMode) {
			Log.info("SteamCraft: online-mode is on, players sign in with Mojang as usual");
			return;
		}
		Log.info("SteamCraft: online-mode is off, players sign in through Steam. Whitelist, operators and bans are in "
				+ STEAM_DIR + "/ (whitelist.json, ops.json, banned-players.json)");
		if (validator == null) Log.warn("SteamCraft: Steam is unavailable, so every login will be refused");
		warnAboutOtherPlayerData();
	}

	public static boolean isSteamMode(MinecraftServer server) {
		return server.isDedicatedServer() && !server.usesAuthentication();
	}

	private static Path steamDir(MinecraftServer server) {
		return server.getServerDirectory().resolve(STEAM_DIR);
	}

	public static File steamListFile(MinecraftServer server, File vanilla) {
		Path dir = steamDir(server);
		try {
			Files.createDirectories(dir);
		} catch (IOException e) {
			throw new UncheckedIOException("Could not create " + dir, e);
		}
		return dir.resolve(vanilla.getName()).toFile();
	}

	private static SteamServer startGameServer(MinecraftServer server, SteamCraftConfig config) {
		try {
			return SteamServer.start(SteamRuntime.nativesDir(), config.steamAppId, server.getPort(), config.auth.ticketTimeoutSeconds);
		} catch (SteamUnavailableException | RuntimeException e) {
			Log.warn("Could not log on to Steam as a game server: " + e.getMessage()
					+ ". Install Steam or SteamCMD (steamclient) on this machine.", e);
			return null;
		}
	}

	private void warnAboutOtherPlayerData() {
		Path playerData = server.getWorldPath(LevelResource.PLAYER_DATA_DIR);
		if (!Files.isDirectory(playerData)) return;
		Set<String> steamFiles = names.steamIds().stream()
				.map(id -> PlayerUuids.fromSteam(id) + ".dat")
				.collect(Collectors.toSet());
		long others;
		try (Stream<Path> files = Files.list(playerData)) {
			others = files.map(f -> f.getFileName().toString())
					.filter(f -> f.endsWith(".dat") && !steamFiles.contains(f))
					.count();
		} catch (IOException e) {
			return;
		}
		if (others == 0) return;
		Log.warn("SteamCraft: " + playerData + " has " + others + " player file(s) from before Steam mode. Steam players get new"
				+ " UUIDs, so they start with an empty inventory. To move a player's data, stop the server and rename"
				+ " <old UUID>.dat to the UUID shown by /steamcraft whois <name>.");
	}

	private void onSteamSessionRevoked(long steamId, String reason) {
		server.execute(() -> {
			for (ServerPlayer player : new ArrayList<>(server.getPlayerList().getPlayers())) {
				Long playerSteamId = sessions.steamId(player.getUUID());
				if (playerSteamId == null || playerSteamId != steamId) continue;
				Log.info(Profiles.name(player.getGameProfile()) + " (Steam " + SteamIds.format(steamId) + ") disconnected by Steam: " + reason);
				player.connection.disconnect(Component.literal(reason));
			}
		});
	}

	public static SteamCraftServer current() {
		return current;
	}

	public static void start(MinecraftServer server) {
		current = new SteamCraftServer(server);
	}

	public static void stop() {
		SteamCraftServer context = current;
		current = null;
		if (context == null) return;
		if (context.validator != null) context.validator.setRevocationListener(null);
		context.stopTunnelHost();
		if (context.steamServer != null) context.steamServer.shutdown();
	}

	public MinecraftServer server() {
		return server;
	}

	public boolean steamMode() {
		return steamMode;
	}

	public boolean skinsEnabled() {
		return steamMode || !server.isDedicatedServer();
	}

	public SkinStore skins() {
		return skins;
	}

	public LoginGate gate() {
		return gate;
	}

	public ActiveSessions sessions() {
		return sessions;
	}

	public PeerRegistry peers() {
		return peers;
	}

	public TicketValidator validator() {
		return validator;
	}

	public Long tunnelSteamId(Connection connection) {
		return peers.lookup(connection.getRemoteAddress());
	}

	public boolean isSteamLogin(Connection connection) {
		if (connection.isMemoryConnection()) return false;
		return steamMode || tunnelSteamId(connection) != null;
	}

	public Long steamIdOf(GameProfile profile) {
		return sessions.steamId(Profiles.id(profile));
	}

	public Long steamIdOfName(String name) {
		return names.owner(name);
	}

	public Optional<GameProfile> steamProfile(String nameOrSteamId) {
		Long steamId = steamIdOfPlayer(nameOrSteamId);
		if (steamId == null) return Optional.empty();
		String name = names.nameOf(steamId);
		return Optional.of(new GameProfile(PlayerUuids.fromSteam(steamId), name != null ? name : nameOrSteamId));
	}

	public Long steamIdOfPlayer(String nameOrSteamId) {
		Long steamId = names.owner(nameOrSteamId);
		if (steamId == null) steamId = names.formerOwner(nameOrSteamId);
		if (steamId == null && nameOrSteamId.matches("\\d{17}")) {
			try {
				steamId = SteamIds.parse(nameOrSteamId);
			} catch (IllegalArgumentException e) {
				return null;
			}
		}
		return steamId;
	}

	public String nameOf(long steamId) {
		return names.nameOf(steamId);
	}

	public List<String> steamPlayerNames() {
		return names.steamIds().stream().map(names::nameOf).collect(Collectors.toList());
	}

	public String steamStatus() {
		if (hostSteam != null) return "host client";
		if (steamServer != null) return steamServer.isLoggedOn() ? "game server (logged on)" : "game server (logging on)";
		return steamMode ? "unavailable" : "not used";
	}

	public CoopAccess access() {
		return access;
	}

	public synchronized TunnelHost tunnelHost() {
		return tunnelHost;
	}

	public synchronized boolean isHostingOverSteam() {
		return tunnelHost != null && tunnelHost.isAccepting();
	}

	public boolean cheats() {
		return cheats;
	}

	public synchronized void startTunnelHost(int port, CoopAccess access, boolean cheats) {
		if (hostSteam == null) throw new IllegalStateException("Steam is not available");
		this.access = access;
		this.cheats = cheats;
		if (tunnelHost != null && tunnelHost.targetPort() == port) {
			tunnelHost.setAccepting(true);
		} else {
			stopTunnelHost();
			CoopGate coopGate = new CoopGate(this::access, hostSteam::isFriend, invites);
			tunnelHost = new TunnelHost(hostSteam, port, peer -> {
				String reason = coopGate.rejectReason(peer);
				if (reason != null && this.access == CoopAccess.INVITE && knocks.knock(peer)) knockListener.accept(peer);
				return reason;
			}, peers);
			tunnelHost.start();
		}
		refreshPermissions();
	}

	public static void setKnockListener(LongConsumer listener) {
		knockListener = listener;
	}

	public List<Long> knocks() {
		return knocks.recent();
	}

	public void dismissKnock(long steamId) {
		knocks.forget(steamId);
	}

	public void kick(UUID player) {
		server.execute(() -> {
			ServerPlayer target = server.getPlayerList().getPlayer(player);
			if (target == null || Profiles.isOwner(server, target.getGameProfile()) && steamIdOf(target.getGameProfile()) == null) return;
			target.connection.disconnect(Component.translatable("multiplayer.disconnect.kicked"));
		});
	}

	public Integer coopPermissionLevel(UUID player, boolean owner) {
		if (tunnelHost() == null) return null;
		return cheats && isCoopAdmin(player, owner) ? 4 : 0;
	}

	public static boolean allowsAdminCommandsWithoutCheats(CommandSourceStack source) {
		SteamCraftServer context = current;
		ServerPlayer player = source.getPlayer();
		if (context == null || player == null || context.server != source.getServer()) return false;
		return context.tunnelHost() != null && !context.cheats
				&& context.isCoopAdmin(player.getUUID(), Profiles.isOwner(context.server, player.getGameProfile()));
	}

	private boolean isCoopAdmin(UUID player, boolean owner) {
		Long steamId = sessions.steamId(player);
		if (steamId != null) return operators.contains(steamId);
		return owner;
	}

	public Map<Long, String> operators() {
		Map<Long, String> all = requireOperators().all();
		all.replaceAll((steamId, saved) -> {
			String name = names.nameOf(steamId);
			return name != null ? name : saved;
		});
		return all;
	}

	public boolean op(long steamId, String name) {
		if (!requireOperators().add(steamId, name)) return false;
		refreshPermissions();
		return true;
	}

	public boolean deop(long steamId) {
		if (!requireOperators().remove(steamId)) return false;
		refreshPermissions();
		return true;
	}

	private CoopOperators requireOperators() {
		if (operators == null) throw new IllegalStateException("Dedicated servers use vanilla /op");
		return operators;
	}

	private void refreshPermissions() {
		server.execute(() -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) server.getPlayerList().sendPlayerPermissionLevel(player);
		});
	}

	public boolean invite(long steamId) {
		if (hostSteam == null) return false;
		invites.invite(steamId);
		knocks.forget(steamId);
		return hostSteam.inviteToGame(steamId, SteamIds.toAddress(hostSteam.localSteamId()));
	}

	public int inviteMinutesLeft(long steamId) {
		return invites.minutesLeft(steamId);
	}

	public synchronized void closeTunnelToNewPlayers() {
		if (tunnelHost == null) return;
		tunnelHost.setAccepting(false);
	}

	public synchronized void stopTunnelHost() {
		if (tunnelHost == null) return;
		tunnelHost.stop();
		tunnelHost = null;
	}
}
