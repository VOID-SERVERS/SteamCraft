package dev.steamcraft.common;

import dev.steamcraft.core.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SteamCraft {
	public static final String MOD_ID = "steamcraft";
	private static final Logger LOGGER = LoggerFactory.getLogger("SteamCraft");

	private SteamCraft() {
	}

	public static void init(Platform platform) {
		Platform.set(platform);
		Log.setSink(new Log.Sink() {
			@Override
			public void info(String message) {
				LOGGER.info(message);
			}

			@Override
			public void warn(String message, Throwable error) {
				LOGGER.warn(message, error);
			}

			@Override
			public void debug(String message) {
				LOGGER.debug(message);
			}
		});
		SteamRuntime.config();
	}
}
