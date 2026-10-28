package dev.steamcraft.core.identity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SteamIdsTest {
	static final long ID = 76561199092349621L;

	@Test
	void parsesPlainAndPrefixedIds() {
		assertEquals(ID, SteamIds.parse("76561199092349621"));
		assertEquals(ID, SteamIds.parse("steam:76561199092349621"));
		assertEquals(ID, SteamIds.parse(" STEAM:76561199092349621 "));
	}

	@Test
	void rejectsNonIndividualIds() {
		assertThrows(IllegalArgumentException.class, () -> SteamIds.parse("12345"));
		assertThrows(IllegalArgumentException.class, () -> SteamIds.parse("90295799103841306"));
		assertThrows(IllegalArgumentException.class, () -> SteamIds.parse("abc"));
	}

	@Test
	void steamAddressRoundTrip() {
		assertEquals("76561199092349621.steam", SteamIds.toAddress(ID));
		assertEquals(ID, SteamIds.fromAddressHost("76561199092349621.steam"));
		assertEquals(ID, SteamIds.fromAddressHost("76561199092349621.STEAM"));
		assertNull(SteamIds.fromAddressHost("mc.example.com"));
		assertNull(SteamIds.fromAddressHost("1234.steam"));
	}

	@Test
	void steamUuidIsStableAndDistinctFromOffline() {
		assertEquals(PlayerUuids.fromSteam(ID), PlayerUuids.fromSteam(ID));
		assertNotEquals(PlayerUuids.fromSteam(ID), PlayerUuids.offline("76561199092349621"));
		assertEquals("b50ad385-829d-3141-a216-7e7d7539ba7f", PlayerUuids.offline("Notch").toString());
	}
}
