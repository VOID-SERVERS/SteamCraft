package dev.steamcraft.core.access;

import dev.steamcraft.core.identity.SteamIds;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public final class CoopOperators extends JsonListStore<CoopOperators.Operator> {
	public static final class Operator {
		public String steamId;
		public String name;
	}

	private final Map<Long, Operator> bySteamId = new LinkedHashMap<>();

	public CoopOperators(Path file) {
		super(file, Operator.class);
		for (Operator operator : read()) bySteamId.put(Long.parseUnsignedLong(operator.steamId), operator);
	}

	@Override
	boolean isValid(Operator operator) {
		return parseSteamId(operator.steamId) != null;
	}

	public synchronized boolean contains(long steamId) {
		return bySteamId.containsKey(steamId);
	}

	public synchronized boolean add(long steamId, String name) {
		if (bySteamId.containsKey(steamId)) return false;
		Operator operator = new Operator();
		operator.steamId = SteamIds.format(steamId);
		operator.name = name;
		bySteamId.put(steamId, operator);
		write(new ArrayList<>(bySteamId.values()));
		return true;
	}

	public synchronized boolean remove(long steamId) {
		if (bySteamId.remove(steamId) == null) return false;
		write(new ArrayList<>(bySteamId.values()));
		return true;
	}

	public synchronized Map<Long, String> all() {
		Map<Long, String> operators = new LinkedHashMap<>();
		for (Map.Entry<Long, Operator> entry : bySteamId.entrySet()) operators.put(entry.getKey(), entry.getValue().name);
		return operators;
	}
}
