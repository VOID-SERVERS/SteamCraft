package dev.steamcraft.core.access;

import dev.steamcraft.core.identity.SteamIds;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class NameClaims extends JsonListStore<NameClaims.Claim> {
	public static final class Claim {
		public String name;
		public String steamId;
		public List<String> formerNames;
	}

	static final int MAX_FORMER_NAMES = 10;

	public enum Result {
		CLAIMED,
		ALREADY_OWNED,
		TAKEN
	}

	private final Map<String, Claim> byName = new LinkedHashMap<>();

	public NameClaims(Path file) {
		super(file, Claim.class);
		for (Claim claim : read()) byName.put(key(claim.name), claim);
	}

	@Override
	boolean isValid(Claim claim) {
		return claim.name != null && parseSteamId(claim.steamId) != null;
	}

	private static String key(String name) {
		return name.toLowerCase(Locale.ROOT);
	}

	public synchronized Long owner(String name) {
		Claim claim = byName.get(key(name));
		return claim == null ? null : Long.parseUnsignedLong(claim.steamId);
	}

	public synchronized boolean isFreeFor(String name, long steamId) {
		Long owner = owner(name);
		return owner == null || owner == steamId;
	}

	public synchronized String nameOf(long steamId) {
		String id = SteamIds.format(steamId);
		for (Claim claim : byName.values()) {
			if (claim.steamId.equals(id)) return claim.name;
		}
		return null;
	}

	public synchronized List<Long> steamIds() {
		List<Long> ids = new ArrayList<>();
		for (Claim claim : byName.values()) ids.add(Long.parseUnsignedLong(claim.steamId));
		return ids;
	}

	public synchronized Long formerOwner(String name) {
		if (byName.containsKey(key(name))) return null;
		Long found = null;
		for (Claim claim : byName.values()) {
			if (claim.formerNames == null || claim.formerNames.stream().noneMatch(n -> n.equalsIgnoreCase(name))) continue;
			if (found != null) return null;
			found = Long.parseUnsignedLong(claim.steamId);
		}
		return found;
	}

	public synchronized Result claim(String name, long steamId) {
		Claim current = byName.get(key(name));
		if (current != null) {
			if (Long.parseUnsignedLong(current.steamId) != steamId) return Result.TAKEN;
			if (current.name.equals(name)) return Result.ALREADY_OWNED;
		}
		String id = SteamIds.format(steamId);
		List<String> former = new ArrayList<>();
		for (Claim old : byName.values()) {
			if (!old.steamId.equals(id)) continue;
			if (old.formerNames != null) former.addAll(old.formerNames);
			former.add(old.name);
		}
		former.removeIf(n -> n.equalsIgnoreCase(name));
		while (former.size() > MAX_FORMER_NAMES) former.remove(0);
		byName.values().removeIf(c -> c.steamId.equals(id));
		Claim claim = new Claim();
		claim.name = name;
		claim.steamId = id;
		claim.formerNames = former.isEmpty() ? null : former;
		byName.put(key(name), claim);
		write(new ArrayList<>(byName.values()));
		return Result.CLAIMED;
	}
}
