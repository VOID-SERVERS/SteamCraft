package dev.steamcraft.core.auth;

import dev.steamcraft.core.access.NameClaims;
import dev.steamcraft.core.identity.PlayerUuids;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class LoginGateTest {
	static final long ALICE_STEAM = 76561198000000001L;
	static final long MALLORY_STEAM = 76561198000000066L;
	static final byte[] GOOD_TICKET = {1, 2, 3};

	final List<Long> endedSessions = new ArrayList<>();

	@TempDir
	Path dir;
	NameClaims names;
	LoginGate gate;

	final TicketValidator validator = new TicketValidator() {
		@Override
		public CompletableFuture<TicketResult> validate(long steamId, byte[] ticket) {
			return CompletableFuture.completedFuture(Arrays.equals(ticket, GOOD_TICKET) ? TicketResult.success() : TicketResult.fail("AuthTicketInvalid"));
		}

		@Override
		public void endSession(long steamId) {
			endedSessions.add(steamId);
		}

		@Override
		public void setRevocationListener(RevocationListener listener) {
		}
	};

	@BeforeEach
	void setUp() {
		names = new NameClaims(dir.resolve("names.json"));
		gate = new LoginGate(names, validator);
	}

	static LoginGate.Attempt attempt(String name, AuthProtocol.Response answer) {
		LoginGate.Attempt attempt = new LoginGate.Attempt();
		attempt.requestedName = name;
		attempt.answer = answer;
		return attempt;
	}

	static AuthProtocol.Response ticket(long steamId, byte[] ticket) {
		return new AuthProtocol.Response(AuthProtocol.Status.OK, steamId, ticket);
	}

	LoginGate.Outcome decide(LoginGate.Attempt attempt) {
		return gate.decide(attempt).join();
	}

	LoginGate.Outcome join(String name, long steamId) {
		LoginGate.Outcome outcome = decide(attempt(name, ticket(steamId, GOOD_TICKET)));
		if (outcome.allowed) assertTrue(gate.joined(outcome.name, outcome.steamId));
		return outcome;
	}

	@Test
	void validTicketGivesTheSteamProfile() {
		LoginGate.Outcome outcome = decide(attempt("Alice", ticket(ALICE_STEAM, GOOD_TICKET)));
		assertTrue(outcome.allowed);
		assertEquals(PlayerUuids.fromSteam(ALICE_STEAM), outcome.uuid);
		assertEquals("Alice", outcome.name);
		assertEquals(ALICE_STEAM, outcome.steamId);
	}

	@Test
	void theNameIsClaimedOnlyOnJoining() {
		assertTrue(decide(attempt("Alice", ticket(MALLORY_STEAM, GOOD_TICKET))).allowed);
		assertNull(names.owner("Alice"), "a login refused later, e.g. by the whitelist, claims nothing");
		assertNull(names.nameOf(MALLORY_STEAM));

		join("Alice", ALICE_STEAM);
		assertEquals(ALICE_STEAM, names.owner("alice"));
		assertEquals("Alice", names.nameOf(ALICE_STEAM));
	}

	@Test
	void ofTwoLoginsUnderOneFreeNameTheFirstToJoinKeepsIt() {
		assertTrue(decide(attempt("Alice", ticket(ALICE_STEAM, GOOD_TICKET))).allowed);
		assertTrue(decide(attempt("Alice", ticket(MALLORY_STEAM, GOOD_TICKET))).allowed);
		assertTrue(gate.joined("Alice", ALICE_STEAM));
		assertFalse(gate.joined("Alice", MALLORY_STEAM));
		assertEquals(ALICE_STEAM, names.owner("Alice"));
	}

	@Test
	void clientsWithoutTheModAreToldToInstallIt() {
		LoginGate.Outcome outcome = decide(attempt("Bob", null));
		assertFalse(outcome.allowed);
		assertEquals(LoginGate.INSTALL_MOD, outcome.denyReason);
	}

	@Test
	void missingOrBadSteamIsRefused() {
		assertFalse(decide(attempt("Alice", AuthProtocol.Response.failure(AuthProtocol.Status.NO_STEAM))).allowed);
		assertFalse(decide(attempt("Alice", AuthProtocol.Response.failure(AuthProtocol.Status.ERROR))).allowed);
		assertFalse(decide(attempt("Alice", ticket(ALICE_STEAM, new byte[]{9}))).allowed);
		assertNull(names.owner("Alice"), "a failed login claims nothing");
	}

	@Test
	void namesCannotBeTakenFromAnotherSteamAccount() {
		assertTrue(join("Alice", ALICE_STEAM).allowed);
		LoginGate.Outcome mallory = decide(attempt("alice", ticket(MALLORY_STEAM, GOOD_TICKET)));
		assertFalse(mallory.allowed);
		assertTrue(mallory.denyReason.contains("taken"));
		assertEquals(Collections.singletonList(MALLORY_STEAM), endedSessions, "the refused Steam session is closed");
	}

	@Test
	void renameKeepsTheUuidAndReleasesTheOldName() {
		join("Alice", ALICE_STEAM);
		LoginGate.Outcome renamed = join("Alicia", ALICE_STEAM);
		assertEquals(PlayerUuids.fromSteam(ALICE_STEAM), renamed.uuid);
		assertNull(names.owner("Alice"));
		assertTrue(join("Alice", MALLORY_STEAM).allowed);
	}

	@Test
	void tunnelIdentityNeedsNoTicketButMustMatchOne() {
		LoginGate.Attempt tunnelled = attempt("Alice", null);
		tunnelled.tunnelSteamId = ALICE_STEAM;
		LoginGate.Outcome ok = decide(tunnelled);
		assertTrue(ok.allowed);
		assertEquals(ALICE_STEAM, ok.steamId);

		LoginGate.Attempt mismatch = attempt("Alice", ticket(MALLORY_STEAM, GOOD_TICKET));
		mismatch.tunnelSteamId = ALICE_STEAM;
		assertFalse(decide(mismatch).allowed);
	}

	@Test
	void withoutSteamOnTheServerNobodyGetsIn() {
		gate = new LoginGate(names, null);
		LoginGate.Outcome outcome = decide(attempt("Alice", ticket(ALICE_STEAM, GOOD_TICKET)));
		assertFalse(outcome.allowed);
		assertTrue(outcome.denyReason.contains("cannot reach Steam"));
	}
}
