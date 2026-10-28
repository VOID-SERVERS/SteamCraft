package dev.steamcraft.core.config;

import java.util.Locale;

public enum CoopAccess {
	EVERYONE,
	FRIENDS,
	INVITE;

	public String id() {
		return name().toLowerCase(Locale.ROOT);
	}

	public static CoopAccess byId(String id) {
		return valueOf(id.toUpperCase(Locale.ROOT));
	}
}
