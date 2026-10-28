package dev.steamcraft.core.identity;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SteamIds {
	private static final long INDIVIDUAL_PREFIX = 0x0110000100000000L;
	private static final Pattern STEAM_HOST = Pattern.compile("^(\\d{17})\\.steam\\.?$");
	public static final String ADDRESS_SUFFIX = ".steam";

	private SteamIds() {
	}

	public static boolean isIndividual(long steamId) {
		return (steamId & 0xFFFFFFFF00000000L) == INDIVIDUAL_PREFIX && (steamId & 0xFFFFFFFFL) != 0;
	}

	public static long parse(String text) {
		String value = text.trim();
		if (value.regionMatches(true, 0, "steam:", 0, 6)) value = value.substring(6);
		long id;
		try {
			id = Long.parseLong(value);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Not a SteamID64: " + text);
		}
		if (!isIndividual(id)) throw new IllegalArgumentException("Not an individual SteamID64: " + text);
		return id;
	}

	public static String format(long steamId) {
		return Long.toUnsignedString(steamId);
	}

	public static String toAddress(long steamId) {
		return format(steamId) + ADDRESS_SUFFIX;
	}

	public static Long fromAddressHost(String host) {
		Matcher matcher = STEAM_HOST.matcher(host.toLowerCase(Locale.ROOT));
		if (!matcher.matches()) return null;
		long id = Long.parseLong(matcher.group(1));
		return isIndividual(id) ? id : null;
	}
}
