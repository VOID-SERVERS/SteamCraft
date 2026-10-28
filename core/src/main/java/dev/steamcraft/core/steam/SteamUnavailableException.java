package dev.steamcraft.core.steam;

public final class SteamUnavailableException extends Exception {
	public SteamUnavailableException(String message) {
		super(message);
	}

	public SteamUnavailableException(String message, Throwable cause) {
		super(message, cause);
	}
}
