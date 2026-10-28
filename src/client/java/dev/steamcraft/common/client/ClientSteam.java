package dev.steamcraft.common.client;

import dev.steamcraft.common.SteamCraftServer;
import dev.steamcraft.common.SteamRuntime;
import dev.steamcraft.common.client.gui.KnockToast;
import dev.steamcraft.core.Log;
import dev.steamcraft.core.auth.AuthProtocol;
import dev.steamcraft.core.config.SteamCraftConfig;
import dev.steamcraft.core.steam.SteamClient;
import dev.steamcraft.core.steam.SteamUnavailableException;
import net.minecraft.client.Minecraft;

import java.util.concurrent.CompletableFuture;

public final class ClientSteam {
	private ClientSteam() {
	}

	public static void start() {
		SteamCraftConfig config = SteamRuntime.config();
		try {
			SteamClient steam = SteamClient.start(SteamRuntime.nativesDir(), config.steamAppId, config.auth.ticketTimeoutSeconds);
			SteamRuntime.setClient(steam);
			steam.setJoinRequestListener(SteamJoin::fromSteamFriendsList);
			Minecraft minecraft = Minecraft.getInstance();
			SteamCraftServer.setKnockListener(steamId -> minecraft.execute(() -> KnockToast.show(steamId)));
		} catch (SteamUnavailableException | RuntimeException | LinkageError e) {
			Log.warn("Steam is unavailable, SteamCraft client features are off: " + e.getMessage());
		}
	}

	public static CompletableFuture<byte[]> answerAuthQuery() {
		SteamClient steam = SteamRuntime.client();
		if (steam == null) return CompletableFuture.completedFuture(AuthProtocol.encodeResponse(AuthProtocol.Response.failure(AuthProtocol.Status.NO_STEAM)));
		return steam.requestTicket().handle((ticket, error) -> {
			if (error != null) {
				Log.warn("Could not create a Steam auth ticket", error);
				return AuthProtocol.encodeResponse(AuthProtocol.Response.failure(AuthProtocol.Status.ERROR));
			}
			return AuthProtocol.encodeResponse(new AuthProtocol.Response(AuthProtocol.Status.OK, steam.localSteamId(), ticket));
		});
	}

	public static void cancelTickets() {
		SteamClient steam = SteamRuntime.client();
		if (steam != null) steam.cancelTickets();
	}

	public static void stop() {
		SteamClient steam = SteamRuntime.client();
		if (steam == null) return;
		if (SteamRuntime.tunnels() != null) SteamRuntime.tunnels().closeAll();
		steam.shutdown();
	}
}
