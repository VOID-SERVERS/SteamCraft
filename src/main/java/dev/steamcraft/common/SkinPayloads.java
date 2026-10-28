package dev.steamcraft.common;

import dev.steamcraft.core.skin.SkinData;
import dev.steamcraft.core.skin.SkinImages;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public final class SkinPayloads {
	private SkinPayloads() {
	}

	public record Upload(SkinData skin) implements CustomPacketPayload {
		public static final Type<Upload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("steamcraft", "skin"));
		public static final StreamCodec<FriendlyByteBuf, Upload> CODEC = StreamCodec.of((buf, upload) -> write(buf, upload.skin), buf -> new Upload(read(buf)));

		@Override
		public Type<Upload> type() {
			return TYPE;
		}
	}

	public record PlayerSkin(UUID player, SkinData skin) implements CustomPacketPayload {
		public static final Type<PlayerSkin> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("steamcraft", "player_skin"));
		public static final StreamCodec<FriendlyByteBuf, PlayerSkin> CODEC = StreamCodec.of((buf, update) -> {
			buf.writeUUID(update.player);
			write(buf, update.skin);
		}, buf -> new PlayerSkin(buf.readUUID(), read(buf)));

		@Override
		public Type<PlayerSkin> type() {
			return TYPE;
		}
	}

	private static void write(FriendlyByteBuf buf, SkinData skin) {
		buf.writeBoolean(skin.slim);
		writeImage(buf, skin.skin);
		writeImage(buf, skin.cape);
	}

	private static SkinData read(FriendlyByteBuf buf) {
		boolean slim = buf.readBoolean();
		return new SkinData(slim, readImage(buf), readImage(buf));
	}

	private static void writeImage(FriendlyByteBuf buf, byte[] png) {
		buf.writeBoolean(png != null);
		if (png != null) buf.writeByteArray(png);
	}

	private static byte[] readImage(FriendlyByteBuf buf) {
		return buf.readBoolean() ? buf.readByteArray(SkinImages.MAX_PNG_BYTES) : null;
	}
}
