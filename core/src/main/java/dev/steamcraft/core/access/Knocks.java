package dev.steamcraft.core.access;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

public final class Knocks {
	static final long ANNOUNCE_EVERY_MILLIS = TimeUnit.MINUTES.toMillis(1);
	static final long REMEMBER_MILLIS = TimeUnit.MINUTES.toMillis(Invites.VALID_MINUTES);

	private final Map<Long, Long> lastKnock = new LinkedHashMap<>();
	private final Map<Long, Long> lastAnnounced = new LinkedHashMap<>();
	private final LongSupplier clock;

	public Knocks() {
		this(System::currentTimeMillis);
	}

	public Knocks(LongSupplier clock) {
		this.clock = clock;
	}

	public synchronized boolean knock(long steamId) {
		long now = clock.getAsLong();
		expire(now);
		lastKnock.remove(steamId);
		lastKnock.put(steamId, now);
		Long announced = lastAnnounced.get(steamId);
		if (announced != null && now - announced < ANNOUNCE_EVERY_MILLIS) return false;
		lastAnnounced.put(steamId, now);
		return true;
	}

	public synchronized List<Long> recent() {
		expire(clock.getAsLong());
		List<Long> ids = new ArrayList<>(lastKnock.keySet());
		Collections.reverse(ids);
		return ids;
	}

	public synchronized void forget(long steamId) {
		lastKnock.remove(steamId);
		lastAnnounced.remove(steamId);
	}

	private void expire(long now) {
		lastKnock.values().removeIf(at -> now - at >= REMEMBER_MILLIS);
		lastAnnounced.values().removeIf(at -> now - at >= ANNOUNCE_EVERY_MILLIS);
	}
}
