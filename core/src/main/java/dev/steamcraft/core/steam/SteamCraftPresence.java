package dev.steamcraft.core.steam;

import dev.steamcraft.core.config.CoopAccess;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public final class SteamCraftPresence {
	public static final String KEY = "steamcraft";
	static final String PLAYING = "playing";
	private static final String HOST_PREFIX = "host:";

	static final String WHERE = "sc_where";
	static final String MODE = "sc_mode";
	static final String PLAYERS = "sc_players";
	static final String DAY = "sc_day";
	static final String DIMENSION = "sc_dim";
	static final String WORLD_NAME = "sc_name";
	static final String HOST = "sc_host";
	static final String[] KEYS = {KEY, WHERE, MODE, PLAYERS, DAY, DIMENSION, WORLD_NAME, HOST};
	static final int MAX_NAME = 64;

	public enum Where {
		MENU,
		OWN_WORLD,
		STEAM_GUEST,
		SERVER
	}

	public static final class Activity {
		public final Where where;
		public final CoopAccess hosting;
		public final String mode;
		public final int players;
		public final long day;
		public final String dimension;
		public final String worldName;
		public final long host;

		public Activity(Where where, CoopAccess hosting, String mode, int players, long day, String dimension, String worldName, long host) {
			this.where = where;
			this.hosting = hosting;
			this.mode = mode;
			this.players = players;
			this.day = day;
			this.dimension = dimension;
			this.worldName = worldName;
			this.host = host;
		}

		public static Activity menu() {
			return new Activity(Where.MENU, null, null, 0, 0, null, null, 0);
		}
	}

	private SteamCraftPresence() {
	}

	public static Map<String, String> encode(Activity activity) {
		Map<String, String> values = new LinkedHashMap<>();
		values.put(KEY, activity.hosting != null ? hosting(activity.hosting) : PLAYING);
		values.put(WHERE, activity.where.name().toLowerCase(Locale.ROOT));
		values.put(MODE, activity.mode != null ? activity.mode : "");
		values.put(PLAYERS, activity.players > 0 ? Integer.toString(activity.players) : "");
		values.put(DAY, activity.day > 0 ? Long.toString(activity.day) : "");
		values.put(DIMENSION, activity.dimension != null ? activity.dimension : "");
		values.put(WORLD_NAME, activity.worldName != null ? cut(activity.worldName) : "");
		values.put(HOST, activity.host != 0 ? Long.toUnsignedString(activity.host) : "");
		return values;
	}

	public static Activity decode(Function<String, String> value) {
		String state = value.apply(KEY);
		if (!isSteamCraft(state)) return null;
		Where where = Where.MENU;
		String whereValue = text(value.apply(WHERE));
		if (whereValue != null) {
			try {
				where = Where.valueOf(whereValue.toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException e) {
			}
		}
		return new Activity(where, hosting(state), text(value.apply(MODE)), (int) number(value.apply(PLAYERS)),
				number(value.apply(DAY)), text(value.apply(DIMENSION)), text(value.apply(WORLD_NAME)), steamId(value.apply(HOST)));
	}

	private static String cut(String name) {
		return name.length() <= MAX_NAME ? name : name.substring(0, MAX_NAME - 3) + "...";
	}

	private static String text(String value) {
		return value == null || value.isEmpty() ? null : value;
	}

	private static long number(String value) {
		if (value == null || value.isEmpty()) return 0;
		try {
			return Math.max(0, Long.parseLong(value));
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static long steamId(String value) {
		if (value == null || value.isEmpty()) return 0;
		try {
			return Long.parseUnsignedLong(value);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	static String hosting(CoopAccess access) {
		return HOST_PREFIX + access.id();
	}

	static boolean isSteamCraft(String value) {
		return value != null && (value.equals(PLAYING) || value.startsWith(HOST_PREFIX));
	}

	static CoopAccess hosting(String value) {
		if (value == null || !value.startsWith(HOST_PREFIX)) return null;
		try {
			return CoopAccess.byId(value.substring(HOST_PREFIX.length()));
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
