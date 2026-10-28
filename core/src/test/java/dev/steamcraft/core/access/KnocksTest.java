package dev.steamcraft.core.access;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class KnocksTest {
	static final long ALICE = 76561198000000001L;
	static final long BOB = 76561198000000002L;

	final AtomicLong now = new AtomicLong(1_000_000);
	final Knocks knocks = new Knocks(now::get);

	@Test
	void oneAttemptIsAnnouncedOnce() {
		assertTrue(knocks.knock(ALICE));
		assertFalse(knocks.knock(ALICE), "the same attempt opens more connections");
		now.addAndGet(Knocks.ANNOUNCE_EVERY_MILLIS);
		assertTrue(knocks.knock(ALICE), "trying again a minute later is news");
	}

	@Test
	void recentListsNewestFirstAndExpires() {
		knocks.knock(ALICE);
		now.addAndGet(1000);
		knocks.knock(BOB);
		assertEquals(Arrays.asList(BOB, ALICE), knocks.recent());

		now.addAndGet(1000);
		knocks.knock(ALICE);
		assertEquals(Arrays.asList(ALICE, BOB), knocks.recent());

		now.addAndGet(Knocks.REMEMBER_MILLIS);
		assertEquals(Collections.emptyList(), knocks.recent());
	}

	@Test
	void forgottenPlayersAreAnnouncedAgain() {
		knocks.knock(ALICE);
		knocks.forget(ALICE);
		assertEquals(Collections.emptyList(), knocks.recent());
		assertTrue(knocks.knock(ALICE));
	}
}
