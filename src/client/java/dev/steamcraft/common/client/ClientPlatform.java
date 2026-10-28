package dev.steamcraft.common.client;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public interface ClientPlatform {
	boolean canSendToServer(CustomPacketPayload.Type<?> type);

	void sendToServer(CustomPacketPayload payload);

	static ClientPlatform get() {
		ClientPlatform platform = Holder.platform;
		if (platform == null) throw new IllegalStateException("SteamCraft's loader module did not set its client platform");
		return platform;
	}

	static void set(ClientPlatform platform) {
		Holder.platform = platform;
	}

	final class Holder {
		private static volatile ClientPlatform platform;

		private Holder() {
		}
	}
}
