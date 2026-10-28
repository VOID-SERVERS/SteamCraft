package dev.steamcraft.neoforge;

import dev.steamcraft.common.SteamLogin;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.login.custom.CustomQueryAnswerPayload;
import net.minecraft.network.protocol.login.custom.CustomQueryPayload;
import net.minecraft.resources.ResourceLocation;

public record RawQueryPayload(byte[] data) implements CustomQueryPayload, CustomQueryAnswerPayload {
	private static final int MAX_BYTES = 1048576;

	public static RawQueryPayload read(FriendlyByteBuf buf) {
		int size = buf.readableBytes();
		if (size > MAX_BYTES) throw new IllegalArgumentException("Payload may not be larger than " + MAX_BYTES + " bytes");
		byte[] data = new byte[size];
		buf.readBytes(data);
		return new RawQueryPayload(data);
	}

	@Override
	public ResourceLocation id() {
		return SteamLogin.CHANNEL;
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeBytes(data);
	}
}
