package dev.steamcraft.common;

import dev.steamcraft.core.config.SteamCraftConfig;
import dev.steamcraft.core.steam.SteamClient;
import dev.steamcraft.core.tunnel.TunnelClients;

import java.nio.file.Path;

public final class SteamRuntime {
	private static volatile SteamCraftConfig config;
	private static volatile SteamClient client;
	private static volatile TunnelClients tunnels;

	private SteamRuntime() {
	}

	public static SteamCraftConfig config() {
		if (config == null) reloadConfig();
		return config;
	}

	public static synchronized void reloadConfig() {
		config = SteamCraftConfig.load(configFile());
	}

	public static void saveConfig() {
		config().save(configFile());
	}

	private static Path configFile() {
		return Platform.get().configDir().resolve("steamcraft.json");
	}

	public static Path nativesDir() {
		return Platform.get().gameDir().resolve(".steamcraft").resolve("natives").resolve("steamworks4j-1.10.0");
	}

	public static SteamClient client() {
		return client;
	}

	public static TunnelClients tunnels() {
		return tunnels;
	}

	public static void setClient(SteamClient steamClient) {
		client = steamClient;
		tunnels = steamClient == null ? null : new TunnelClients(steamClient);
	}
}
