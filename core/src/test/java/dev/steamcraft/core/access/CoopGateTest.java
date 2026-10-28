package dev.steamcraft.core.access;

import dev.steamcraft.core.config.CoopAccess;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class CoopGateTest {
	static final long FRIEND = 76561198000000001L;
	static final long STRANGER = 76561198000000002L;

	final AtomicLong now = new AtomicLong(1_000_000);
	final Invites invites = new Invites(now::get);
	CoopAccess access = CoopAccess.FRIENDS;
	final CoopGate gate = new CoopGate(() -> access, id -> id == FRIEND, invites);

	@Test
	void everyoneLetsStrangersIn() {
		access = CoopAccess.EVERYONE;
		assertNull(gate.rejectReason(STRANGER));
	}

	@Test
	void friendsOnly() {
		assertNull(gate.rejectReason(FRIEND));
		assertNotNull(gate.rejectReason(STRANGER));
		invites.invite(STRANGER);
		assertNotNull(gate.rejectReason(STRANGER), "an invite does not override friends-only");
	}

	@Test
	void inviteOnlyLetsInvitedPlayersInForTenMinutes() {
		access = CoopAccess.INVITE;
		assertNotNull(gate.rejectReason(FRIEND), "friends need an invite too");
		invites.invite(STRANGER);
		assertNull(gate.rejectReason(STRANGER));
		now.addAndGet(TimeUnit.MINUTES.toMillis(Invites.VALID_MINUTES) - 1);
		assertNull(gate.rejectReason(STRANGER));
		now.addAndGet(1);
		assertNotNull(gate.rejectReason(STRANGER), "expired");
	}

	@Test
	void reinvitingRenewsTheInvite() {
		access = CoopAccess.INVITE;
		invites.invite(STRANGER);
		now.addAndGet(TimeUnit.MINUTES.toMillis(9));
		invites.invite(STRANGER);
		now.addAndGet(TimeUnit.MINUTES.toMillis(9));
		assertNull(gate.rejectReason(STRANGER));
	}
}
