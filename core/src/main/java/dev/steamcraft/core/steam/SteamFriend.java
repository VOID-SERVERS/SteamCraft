package dev.steamcraft.core.steam;

import dev.steamcraft.core.config.CoopAccess;

public final class SteamFriend {
	public enum Status {
		ONLINE,
		AWAY,
		BUSY,
		OFFLINE
	}

	public final long steamId;
	public final String name;
	public final Status status;
	public final boolean inGame;
	public final SteamCraftPresence.Activity activity;
	public final boolean playingSteamCraft;
	public final CoopAccess hosting;

	public SteamFriend(long steamId, String name, Status status, boolean inGame, SteamCraftPresence.Activity activity) {
		this.steamId = steamId;
		this.name = name;
		this.status = status;
		this.inGame = inGame;
		this.activity = activity;
		this.playingSteamCraft = activity != null;
		this.hosting = activity != null ? activity.hosting : null;
	}

	public boolean isOnline() {
		return status != Status.OFFLINE;
	}
}
