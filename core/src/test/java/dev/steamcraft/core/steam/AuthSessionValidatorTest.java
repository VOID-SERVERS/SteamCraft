package dev.steamcraft.core.steam;

import com.codedisaster.steamworks.SteamAuth;
import dev.steamcraft.core.auth.TicketValidator.TicketResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class AuthSessionValidatorTest {
	static final long PLAYER = 76561198000000001L;

	final SteamThread thread = new SteamThread("test-steam");
	final List<String> revoked = new ArrayList<>();
	final AuthSessionValidator validator = new AuthSessionValidator(thread, new AuthSessionValidator.Backend() {
		@Override
		public SteamAuth.BeginAuthSessionResult begin(ByteBuffer ticket, long steamId) {
			return SteamAuth.BeginAuthSessionResult.OK;
		}

		@Override
		public void end(long steamId) {
		}
	}, 5, CompletableFuture.completedFuture(null));

	@AfterEach
	void tearDown() {
		thread.stop(null);
	}

	TicketResult login() throws Exception {
		CompletableFuture<TicketResult> result = validator.validate(PLAYER, new byte[]{1});
		thread.submit(() -> null).get(5, TimeUnit.SECONDS);
		validator.onResponse(PLAYER, SteamAuth.AuthSessionResponse.OK);
		return result.get(5, TimeUnit.SECONDS);
	}

	@Test
	void laterResponsesRevokeTheAcceptedSession() throws Exception {
		validator.setRevocationListener((steamId, reason) -> revoked.add(steamId + ": " + reason));
		assertTrue(login().ok);
		assertTrue(revoked.isEmpty());

		validator.onResponse(PLAYER, SteamAuth.AuthSessionResponse.UserNotConnectedToSteam);
		assertEquals(1, revoked.size());
		assertTrue(revoked.get(0).startsWith(PLAYER + ": You went offline in Steam"));
	}

	@Test
	void aRejectedLoginIsNotAlsoReportedAsRevoked() throws Exception {
		validator.setRevocationListener((steamId, reason) -> revoked.add(reason));
		CompletableFuture<TicketResult> result = validator.validate(PLAYER, new byte[]{1});
		thread.submit(() -> null).get(5, TimeUnit.SECONDS);
		validator.onResponse(PLAYER, SteamAuth.AuthSessionResponse.AuthTicketInvalid);
		assertFalse(result.get(5, TimeUnit.SECONDS).ok);
		assertTrue(revoked.isEmpty());
	}

	@Test
	void everyRevocationHasAReadableMessage() {
		for (SteamAuth.AuthSessionResponse response : SteamAuth.AuthSessionResponse.values()) {
			assertFalse(AuthSessionValidator.revocationMessage(response).isEmpty());
		}
		assertEquals("Your Steam account signed in somewhere else.",
				AuthSessionValidator.revocationMessage(SteamAuth.AuthSessionResponse.LoggedInElseWhere));
	}
}
