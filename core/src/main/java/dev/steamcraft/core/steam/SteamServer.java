package dev.steamcraft.core.steam;

import com.codedisaster.steamworks.SteamAuth;
import com.codedisaster.steamworks.SteamException;
import com.codedisaster.steamworks.SteamGameServer;
import com.codedisaster.steamworks.SteamGameServerAPI;
import com.codedisaster.steamworks.SteamGameServerCallback;
import com.codedisaster.steamworks.SteamID;
import com.codedisaster.steamworks.SteamResult;
import dev.steamcraft.core.Log;
import dev.steamcraft.core.auth.TicketValidator;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public final class SteamServer {
	private static final short QUERY_PORT_SHARED = (short) 0xFFFF;

	private final SteamThread thread = new SteamThread("steamcraft-steam-server");
	private final CompletableFuture<Void> loggedOn = new CompletableFuture<>();
	private SteamGameServer gameServer;
	private AuthSessionValidator validator;

	private SteamServer() {
	}

	public static SteamServer start(Path nativesDir, int appId, int gamePort, int ticketTimeoutSeconds) throws SteamUnavailableException {
		SteamNatives.writeAppId(appId);
		if (!SteamGameServerAPI.loadLibraries(new SteamNatives(nativesDir))) {
			throw new SteamUnavailableException("Steam natives are not available for this platform");
		}
		SteamServer server = new SteamServer();
		try {
			server.thread.call(() -> {
				server.init(gamePort, ticketTimeoutSeconds);
				return null;
			});
		} catch (IllegalStateException e) {
			server.thread.stop(null);
			throw new SteamUnavailableException(e.getCause() != null ? e.getCause().getMessage() : e.getMessage(), e);
		}
		return server;
	}

	private void init(int gamePort, int ticketTimeoutSeconds) throws SteamException {
		if (!SteamGameServerAPI.init(0, (short) gamePort, QUERY_PORT_SHARED, SteamGameServerAPI.ServerMode.Authentication, "1.0.0.0")) {
			throw new SteamException("SteamGameServer_Init failed (is steamclient available?)");
		}
		gameServer = new SteamGameServer(new Callback());
		gameServer.setProduct("steamcraft");
		gameServer.setModDir("steamcraft");
		gameServer.setGameDescription("Minecraft (SteamCraft)");
		gameServer.setDedicatedServer(true);
		gameServer.setAdvertiseServerActive(false);
		gameServer.logOnAnonymous();
		validator = new AuthSessionValidator(thread, new AuthSessionValidator.Backend() {
			@Override
			public SteamAuth.BeginAuthSessionResult begin(ByteBuffer ticket, long steamId) throws SteamException {
				return gameServer.beginAuthSession(ticket, SteamID.createFromNativeHandle(steamId));
			}

			@Override
			public void end(long steamId) {
				gameServer.endAuthSession(SteamID.createFromNativeHandle(steamId));
			}
		}, ticketTimeoutSeconds, loggedOn);
		thread.addTicker(SteamGameServerAPI::runCallbacks);
	}

	public TicketValidator validator() {
		return validator;
	}

	public boolean isLoggedOn() {
		return loggedOn.isDone();
	}

	public void shutdown() {
		thread.stop(() -> {
			gameServer.logOff();
			SteamGameServerAPI.shutdown();
		});
	}

	private final class Callback implements SteamGameServerCallback {
		@Override
		public void onValidateAuthTicketResponse(SteamID steamId, SteamAuth.AuthSessionResponse response, SteamID ownerSteamId) {
			validator.onResponse(SteamID.getNativeHandle(steamId), response);
		}

		@Override
		public void onSteamServersConnected() {
			Log.info("Steam game server logged on as " + Long.toUnsignedString(SteamID.getNativeHandle(gameServer.getSteamID())));
			loggedOn.complete(null);
		}

		@Override
		public void onSteamServerConnectFailure(SteamResult result, boolean stillRetrying) {
			Log.warn("Steam game server logon failed: " + result + (stillRetrying ? " (retrying)" : ""));
		}

		@Override
		public void onSteamServersDisconnected(SteamResult result) {
			Log.warn("Steam game server disconnected: " + result);
		}

		@Override
		public void onClientApprove(SteamID steamId, SteamID ownerSteamId) {
		}

		@Override
		public void onClientDeny(SteamID steamId, SteamGameServer.DenyReason reason, String optionalText) {
		}

		@Override
		public void onClientKick(SteamID steamId, SteamGameServer.DenyReason reason) {
		}

		@Override
		public void onClientGroupStatus(SteamID steamId, SteamID groupId, boolean member, boolean officer) {
		}

		@Override
		public void onAssociateWithClanResult(SteamResult result) {
		}

		@Override
		public void onComputeNewPlayerCompatibilityResult(SteamResult result, int playersThatDontLikeCandidate, int playersThatCandidateDoesntLike, int clanPlayersThatDontLikeCandidate, SteamID candidate) {
		}
	}
}
