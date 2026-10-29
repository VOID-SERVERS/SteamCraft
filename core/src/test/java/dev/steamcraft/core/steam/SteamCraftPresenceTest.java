package dev.steamcraft.core.steam;

import dev.steamcraft.core.config.CoopAccess;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SteamCraftPresenceTest {
	static final long ALICE = 76561198000000001L;

	static SteamCraftPresence.Activity roundTrip(SteamCraftPresence.Activity activity) {
		Map<String, String> values = SteamCraftPresence.encode(activity);
		return SteamCraftPresence.decode(values::get);
	}

	@Test
	void everyFieldSurvivesTheTrip() {
		SteamCraftPresence.Activity sent = new SteamCraftPresence.Activity(SteamCraftPresence.Where.STEAM_GUEST, CoopAccess.INVITE,
				"survival", 3, 34, "minecraft:the_nether", "New World", ALICE, "Alice_MC");
		SteamCraftPresence.Activity read = roundTrip(sent);
		assertEquals(SteamCraftPresence.Where.STEAM_GUEST, read.where);
		assertEquals(CoopAccess.INVITE, read.hosting);
		assertEquals("survival", read.mode);
		assertEquals(3, read.players);
		assertEquals(34, read.day);
		assertEquals("minecraft:the_nether", read.dimension);
		assertEquals("New World", read.worldName);
		assertEquals(ALICE, read.host);
		assertEquals("Alice_MC", read.player);
	}

	@Test
	void hostingIsReadForEveryAccess() {
		for (CoopAccess access : CoopAccess.values()) {
			SteamCraftPresence.Activity read = roundTrip(new SteamCraftPresence.Activity(SteamCraftPresence.Where.OWN_WORLD, access,
					null, 0, 0, null, null, 0));
			assertEquals(access, read.hosting);
		}
	}

	@Test
	void theMenuClearsEverythingElse() {
		Map<String, String> values = SteamCraftPresence.encode(SteamCraftPresence.Activity.menu());
		assertEquals(SteamCraftPresence.KEYS.length, values.size(), "every key is set, so old values do not linger");
		SteamCraftPresence.Activity read = SteamCraftPresence.decode(values::get);
		assertEquals(SteamCraftPresence.Where.MENU, read.where);
		assertNull(read.hosting);
		assertNull(read.mode);
		assertNull(read.worldName);
		assertEquals(0, read.players);
		assertEquals(0, read.host);
	}

	@Test
	void longNamesAreCut() {
		StringBuilder name = new StringBuilder();
		for (int i = 0; i < 200; i++) name.append('w');
		SteamCraftPresence.Activity read = roundTrip(new SteamCraftPresence.Activity(SteamCraftPresence.Where.OWN_WORLD, null,
				null, 0, 0, null, name.toString(), 0));
		assertEquals(SteamCraftPresence.MAX_NAME, read.worldName.length());
	}

	@Test
	void otherGamesAndGarbageAreTolerated() {
		assertNull(SteamCraftPresence.decode(key -> null), "not SteamCraft");
		assertNull(SteamCraftPresence.decode(key -> key.equals(SteamCraftPresence.KEY) ? "Playing Spacewar" : null));

		Map<String, String> values = new HashMap<>();
		values.put(SteamCraftPresence.KEY, "host:guild");
		values.put(SteamCraftPresence.WHERE, "on_the_moon");
		values.put(SteamCraftPresence.PLAYERS, "many");
		values.put(SteamCraftPresence.DAY, "-5");
		SteamCraftPresence.Activity read = SteamCraftPresence.decode(values::get);
		assertNotNull(read, "a newer SteamCraft still counts");
		assertNull(read.hosting);
		assertEquals(SteamCraftPresence.Where.MENU, read.where);
		assertEquals(0, read.players);
		assertEquals(0, read.day);
	}
}
