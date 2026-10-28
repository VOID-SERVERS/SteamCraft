package dev.steamcraft.core.skin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.LongSupplier;

public final class SkinStore {
	public static final long COOLDOWN_MILLIS = 2000;

	private final LongSupplier clock;
	private final Map<UUID, SkinData> published = new LinkedHashMap<>();
	private final Map<UUID, Long> lastHandled = new HashMap<>();
	private final Map<UUID, SkinData> waiting = new LinkedHashMap<>();
	private final Set<UUID> introduced = new HashSet<>();

	public SkinStore() {
		this(System::currentTimeMillis);
	}

	public SkinStore(LongSupplier clock) {
		this.clock = clock;
	}

	public synchronized boolean submit(UUID player, SkinData upload) {
		long now = clock.getAsLong();
		Long last = lastHandled.get(player);
		if (last != null && now - last < COOLDOWN_MILLIS) {
			waiting.put(player, upload);
			return false;
		}
		lastHandled.put(player, now);
		waiting.remove(player);
		return true;
	}

	public synchronized Map<UUID, SkinData> due() {
		Map<UUID, SkinData> due = new LinkedHashMap<>();
		long now = clock.getAsLong();
		for (Iterator<Map.Entry<UUID, SkinData>> it = waiting.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<UUID, SkinData> entry = it.next();
			if (now - lastHandled.get(entry.getKey()) < COOLDOWN_MILLIS) continue;
			lastHandled.put(entry.getKey(), now);
			due.put(entry.getKey(), entry.getValue());
			it.remove();
		}
		return due;
	}

	public synchronized void publish(UUID player, SkinData skin) {
		published.put(player, skin);
	}

	public synchronized Map<UUID, SkinData> published() {
		return new LinkedHashMap<>(published);
	}

	public synchronized boolean introduce(UUID player) {
		return introduced.add(player);
	}

	public synchronized void remove(UUID player) {
		introduced.remove(player);
		published.remove(player);
		lastHandled.remove(player);
		waiting.remove(player);
	}
}
