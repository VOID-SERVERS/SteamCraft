package dev.steamcraft.core.skin;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SkinStoreTest {
	static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-000000000001");

	long now = 1_000_000;
	final SkinStore store = new SkinStore(() -> now);

	static SkinData skin(int marker) {
		return new SkinData(false, new byte[]{(byte) marker}, null);
	}

	@Test
	void uploadsInsideTheCooldownWaitAndOnlyTheNewestIsKept() {
		assertTrue(store.submit(ALICE, skin(1)));
		now += 500;
		assertFalse(store.submit(ALICE, skin(2)));
		assertFalse(store.submit(ALICE, skin(3)));
		assertTrue(store.due().isEmpty());

		now += SkinStore.COOLDOWN_MILLIS;
		Map<UUID, SkinData> due = store.due();
		assertEquals(1, due.size());
		assertEquals(3, due.get(ALICE).skin[0]);
		assertTrue(store.due().isEmpty());
		assertFalse(store.submit(ALICE, skin(4)), "handling a waiting upload restarts the cooldown");
	}

	@Test
	void aPlayerIsSentEveryonesSkinsOnce() {
		assertTrue(store.introduce(ALICE));
		assertFalse(store.introduce(ALICE));
		store.remove(ALICE);
		assertTrue(store.introduce(ALICE), "again after rejoining");
	}

	@Test
	void leavingForgetsEverything() {
		store.submit(ALICE, skin(1));
		store.publish(ALICE, skin(1));
		store.submit(ALICE, skin(2));
		store.remove(ALICE);
		assertTrue(store.published().isEmpty());
		now += SkinStore.COOLDOWN_MILLIS;
		assertTrue(store.due().isEmpty());
		assertTrue(store.submit(ALICE, skin(3)), "a rejoining player is not held back");
	}
}
