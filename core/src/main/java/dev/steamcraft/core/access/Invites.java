package dev.steamcraft.core.access;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

public final class Invites {
	public static final int VALID_MINUTES = 10;
	private static final long VALID_MILLIS = TimeUnit.MINUTES.toMillis(VALID_MINUTES);

	private final Map<Long, Long> expiry = new ConcurrentHashMap<>();
	private final LongSupplier clock;

	public Invites() {
		this(System::currentTimeMillis);
	}

	public Invites(LongSupplier clock) {
		this.clock = clock;
	}

	public void invite(long steamId) {
		expiry.put(steamId, clock.getAsLong() + VALID_MILLIS);
	}

	public boolean isInvited(long steamId) {
		return minutesLeft(steamId) > 0;
	}

	public int minutesLeft(long steamId) {
		long now = clock.getAsLong();
		expiry.values().removeIf(until -> until <= now);
		Long until = expiry.get(steamId);
		if (until == null) return 0;
		return (int) ((until - now + VALID_MILLIS / VALID_MINUTES - 1) / (VALID_MILLIS / VALID_MINUTES));
	}
}
