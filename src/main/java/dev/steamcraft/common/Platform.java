package dev.steamcraft.common;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;

public interface Platform {
	Path configDir();

	Path gameDir();

	boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type);

	void send(ServerPlayer player, CustomPacketPayload payload);

	default void loginVerificationStarting(SteamLoginHandler handler) {
	}

	static Platform get() {
		Platform platform = Holder.platform;
		if (platform == null) throw new IllegalStateException("SteamCraft's loader module did not set its platform");
		return platform;
	}

	static void set(Platform platform) {
		Holder.platform = platform;
	}

	final class Holder {
		private static volatile Platform platform;

		private Holder() {
		}
	}
}
