package dev.steamcraft.common;

import com.mojang.authlib.GameProfile;
import dev.steamcraft.core.Log;
import dev.steamcraft.core.auth.AuthProtocol;
import dev.steamcraft.core.auth.LoginGate;
import dev.steamcraft.core.identity.SteamIds;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;

import java.util.concurrent.CompletableFuture;

public final class SteamLogin {
	public static final ResourceLocation CHANNEL = ResourceLocation.fromNamespaceAndPath(AuthProtocol.NAMESPACE, AuthProtocol.PATH);

	private SteamLogin() {
	}

	public static byte[] start(SteamLoginHandler handler) {
		SteamCraftServer context = SteamCraftServer.current();
		if (context == null) return null;
		Connection connection = handler.steamcraft$connection();
		if (!context.isSteamLogin(connection)) return null;
		handler.steamcraft$setPending(true);
		return AuthProtocol.newRequest();
	}

	public static CompletableFuture<Void> answered(MinecraftServer server, SteamLoginHandler handler, byte[] response) {
		SteamCraftServer context = SteamCraftServer.current();
		if (context == null) return CompletableFuture.completedFuture(null);
		LoginGate.Attempt attempt = attempt(handler, context, response);
		return context.gate().decide(attempt)
				.exceptionally(error -> {
					Log.warn("Login decision failed", error);
					return null;
				})
				.thenAcceptAsync(outcome -> apply(server, handler, context, outcome), server);
	}

	public static void loginDisconnected(SteamLoginHandler handler) {
		SteamCraftServer context = SteamCraftServer.current();
		Long steamId = handler.steamcraft$steamId();
		if (context == null || steamId == null) return;
		GameProfile profile = handler.steamcraft$authenticatedProfile();
		if (profile != null) context.sessions().remove(Profiles.id(profile));
		if (context.validator() != null) context.validator().endSession(steamId);
	}

	public static void joined(ServerPlayer player) {
		SteamCraftServer context = SteamCraftServer.current();
		if (context == null) return;
		Long steamId = context.sessions().steamId(player.getUUID());
		if (steamId == null) return;
		String name = Profiles.name(player.getGameProfile());
		if (!context.gate().joined(name, steamId)) player.connection.disconnect(Component.literal(LoginGate.nameTaken(name)));
	}

	public static void left(ServerPlayer player) {
		SteamCraftServer context = SteamCraftServer.current();
		if (context == null) return;
		Long steamId = context.sessions().remove(player.getUUID());
		if (steamId != null && context.validator() != null) context.validator().endSession(steamId);
	}

	private static LoginGate.Attempt attempt(SteamLoginHandler handler, SteamCraftServer context, byte[] response) {
		LoginGate.Attempt attempt = new LoginGate.Attempt();
		attempt.requestedName = handler.steamcraft$requestedUsername() != null
				? handler.steamcraft$requestedUsername()
				: Profiles.name(handler.steamcraft$authenticatedProfile());
		attempt.tunnelSteamId = context.tunnelSteamId(handler.steamcraft$connection());
		if (response != null) {
			try {
				attempt.answer = AuthProtocol.decodeResponse(response);
			} catch (IllegalArgumentException e) {
				attempt.answer = AuthProtocol.Response.failure(AuthProtocol.Status.ERROR);
			}
		}
		return attempt;
	}

	private static void apply(MinecraftServer server, SteamLoginHandler handler, SteamCraftServer context, LoginGate.Outcome outcome) {
		String denyReason = outcome == null ? "SteamCraft could not check your login." : outcome.denyReason;
		if (denyReason == null) {
			ServerPlayer other = server.getPlayerList().getPlayerByName(outcome.name);
			if (other != null && !other.getUUID().equals(outcome.uuid)) {
				denyReason = "The name " + outcome.name + " is already in use on this server.";
				context.gate().endSession(outcome.steamId);
			}
		}
		if (denyReason != null) {
			handler.steamcraft$deny();
			((ServerLoginPacketListenerImpl) handler).disconnect(Component.literal(denyReason));
			return;
		}
		handler.steamcraft$replaceProfile(new GameProfile(outcome.uuid, outcome.name));
		handler.steamcraft$setSteamId(outcome.steamId);
		context.sessions().put(outcome.uuid, outcome.steamId);
		Log.info("Steam verified " + outcome.name + " as " + SteamIds.format(outcome.steamId) + " (UUID " + outcome.uuid + ")");
		handler.steamcraft$setPending(false);
	}
}
