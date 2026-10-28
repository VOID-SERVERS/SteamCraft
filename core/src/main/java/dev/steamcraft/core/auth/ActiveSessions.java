package dev.steamcraft.core.auth;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ActiveSessions {
	private final Map<UUID, Long> steamIds = new ConcurrentHashMap<>();

	public void put(UUID uuid, long steamId) {
		steamIds.put(uuid, steamId);
	}

	public Long steamId(UUID uuid) {
		return steamIds.get(uuid);
	}

	public Long remove(UUID uuid) {
		return steamIds.remove(uuid);
	}
}
