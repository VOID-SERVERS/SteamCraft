package dev.steamcraft.core.identity;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class PlayerUuids {
	private PlayerUuids() {
	}

	public static UUID fromSteam(long steamId) {
		return UUID.nameUUIDFromBytes(("SteamCraft:steam:" + SteamIds.format(steamId)).getBytes(StandardCharsets.UTF_8));
	}

	public static UUID offline(String name) {
		return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
	}
}
