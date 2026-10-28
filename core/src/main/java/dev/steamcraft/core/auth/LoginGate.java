package dev.steamcraft.core.auth;

import dev.steamcraft.core.access.NameClaims;
import dev.steamcraft.core.identity.PlayerUuids;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class LoginGate {
	static final String INSTALL_MOD = "This server signs players in through Steam. Install the SteamCraft mod and start Steam to join.";

	public static final class Attempt {
		public String requestedName;
		public Long tunnelSteamId;
		public AuthProtocol.Response answer;
	}

	public static final class Outcome {
		public final boolean allowed;
		public final String denyReason;
		public final UUID uuid;
		public final String name;
		public final Long steamId;

		private Outcome(boolean allowed, String denyReason, UUID uuid, String name, Long steamId) {
			this.allowed = allowed;
			this.denyReason = denyReason;
			this.uuid = uuid;
			this.name = name;
			this.steamId = steamId;
		}

		static Outcome allow(String name, long steamId) {
			return new Outcome(true, null, PlayerUuids.fromSteam(steamId), name, steamId);
		}

		static Outcome deny(String reason) {
			return new Outcome(false, reason, null, null, null);
		}
	}

	private static final class SteamCheck {
		final Long steamId;
		final String failure;

		SteamCheck(Long steamId, String failure) {
			this.steamId = steamId;
			this.failure = failure;
		}
	}

	private final NameClaims names;
	private final TicketValidator validator;

	public LoginGate(NameClaims names, TicketValidator validator) {
		this.names = names;
		this.validator = validator;
	}

	public CompletableFuture<Outcome> decide(Attempt attempt) {
		return verifySteam(attempt).thenApply(check -> {
			if (check.steamId == null) return Outcome.deny(check.failure);
			String name = attempt.requestedName;
			if (!names.isFreeFor(name, check.steamId)) {
				endSession(check.steamId);
				return Outcome.deny(nameTaken(name));
			}
			return Outcome.allow(name, check.steamId);
		});
	}

	public boolean joined(String name, long steamId) {
		return names.claim(name, steamId) != NameClaims.Result.TAKEN;
	}

	public static String nameTaken(String name) {
		return "The name " + name + " is already taken by another Steam account.";
	}

	public void endSession(long steamId) {
		if (validator != null) validator.endSession(steamId);
	}

	private CompletableFuture<SteamCheck> verifySteam(Attempt attempt) {
		AuthProtocol.Response answer = attempt.answer;
		if (attempt.tunnelSteamId != null) {
			if (answer != null && answer.status == AuthProtocol.Status.OK && answer.steamId != attempt.tunnelSteamId) {
				return done(null, "Steam account mismatch between the connection and the login.");
			}
			return done(attempt.tunnelSteamId, null);
		}
		if (answer == null) return done(null, INSTALL_MOD);
		if (answer.status == AuthProtocol.Status.NO_STEAM) return done(null, "Steam is not running or you are not logged in to Steam.");
		if (answer.status != AuthProtocol.Status.OK) return done(null, "Your client could not create a Steam ticket.");
		if (validator == null) return done(null, "This server cannot reach Steam to verify your account.");
		long steamId = answer.steamId;
		return validator.validate(steamId, answer.ticket)
				.thenApply(result -> result.ok ? new SteamCheck(steamId, null) : new SteamCheck(null, "Steam rejected your login: " + result.failure))
				.exceptionally(e -> new SteamCheck(null, "Could not verify your Steam account."));
	}

	private static CompletableFuture<SteamCheck> done(Long steamId, String failure) {
		return CompletableFuture.completedFuture(new SteamCheck(steamId, failure));
	}
}
