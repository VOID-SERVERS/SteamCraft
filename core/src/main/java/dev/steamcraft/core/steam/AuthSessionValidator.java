package dev.steamcraft.core.steam;

import com.codedisaster.steamworks.SteamAuth;
import com.codedisaster.steamworks.SteamException;
import dev.steamcraft.core.Timeouts;
import dev.steamcraft.core.auth.TicketValidator;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

final class AuthSessionValidator implements TicketValidator {
	interface Backend {
		SteamAuth.BeginAuthSessionResult begin(ByteBuffer ticket, long steamId) throws SteamException;

		void end(long steamId);
	}

	private final SteamThread thread;
	private final Backend backend;
	private final int timeoutSeconds;
	private final CompletableFuture<Void> ready;
	private final Map<Long, CompletableFuture<TicketResult>> pending = new ConcurrentHashMap<>();
	private volatile RevocationListener revocationListener;

	AuthSessionValidator(SteamThread thread, Backend backend, int timeoutSeconds, CompletableFuture<Void> ready) {
		this.thread = thread;
		this.backend = backend;
		this.timeoutSeconds = timeoutSeconds;
		this.ready = ready;
	}

	@Override
	public CompletableFuture<TicketResult> validate(long steamId, byte[] ticket) {
		CompletableFuture<TicketResult> result = new CompletableFuture<>();
		Timeouts.completeOnTimeout(result, timeoutSeconds, TimeUnit.SECONDS, () -> TicketResult.fail("timed out"));
		result.whenComplete((r, e) -> pending.remove(steamId, result));
		ready.thenRunAsync(() -> begin(steamId, ticket, result), thread);
		return result;
	}

	private void begin(long steamId, byte[] ticket, CompletableFuture<TicketResult> result) {
		if (result.isDone()) return;
		ByteBuffer buffer = ByteBuffer.allocateDirect(ticket.length);
		buffer.put(ticket).flip();
		try {
			backend.end(steamId);
			CompletableFuture<TicketResult> previous = pending.put(steamId, result);
			if (previous != null) previous.complete(TicketResult.fail("superseded by a newer login"));
			SteamAuth.BeginAuthSessionResult begin = backend.begin(buffer, steamId);
			if (begin != SteamAuth.BeginAuthSessionResult.OK) result.complete(TicketResult.fail(begin.name()));
		} catch (SteamException e) {
			result.complete(TicketResult.fail(e.getMessage()));
		}
	}

	void onResponse(long steamId, SteamAuth.AuthSessionResponse response) {
		CompletableFuture<TicketResult> result = pending.get(steamId);
		if (result != null) {
			result.complete(response == SteamAuth.AuthSessionResponse.OK ? TicketResult.success() : TicketResult.fail(response.name()));
			return;
		}
		RevocationListener listener = revocationListener;
		if (response != SteamAuth.AuthSessionResponse.OK && listener != null) listener.onRevoked(steamId, revocationMessage(response));
	}

	static String revocationMessage(SteamAuth.AuthSessionResponse response) {
		switch (response) {
			case AuthTicketCanceled:
				return "Your Steam login was cancelled. Rejoin to keep playing.";
			case UserNotConnectedToSteam:
				return "You went offline in Steam. Rejoin once Steam is back online.";
			case LoggedInElseWhere:
				return "Your Steam account signed in somewhere else.";
			case VACBanned:
				return "Your Steam account is VAC banned.";
			case PublisherIssuedBan:
				return "Your Steam account is banned by the publisher.";
			case NoLicenseOrExpired:
				return "Your Steam account no longer has access to this game.";
			default:
				return "Steam ended your session (" + response.name() + ").";
		}
	}

	@Override
	public void setRevocationListener(RevocationListener listener) {
		revocationListener = listener;
	}

	@Override
	public void endSession(long steamId) {
		thread.execute(() -> backend.end(steamId));
	}
}
