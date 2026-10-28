package dev.steamcraft.core.access;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class InvitesTest {
	static final long ALICE = 76561198000000001L;

	final AtomicLong now = new AtomicLong(1_000_000);
	final Invites invites = new Invites(now::get);

	@Test
	void minutesLeftRoundUpAndRunOut() {
		assertEquals(0, invites.minutesLeft(ALICE));
		invites.invite(ALICE);
		assertEquals(Invites.VALID_MINUTES, invites.minutesLeft(ALICE));

		now.addAndGet(1);
		assertEquals(Invites.VALID_MINUTES, invites.minutesLeft(ALICE), "a part of a minute counts as one");

		now.addAndGet(TimeUnit.MINUTES.toMillis(Invites.VALID_MINUTES) - 2);
		assertEquals(1, invites.minutesLeft(ALICE));
		assertTrue(invites.isInvited(ALICE));

		now.addAndGet(1);
		assertEquals(0, invites.minutesLeft(ALICE));
		assertFalse(invites.isInvited(ALICE));
	}
}
