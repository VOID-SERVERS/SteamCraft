package dev.steamcraft.common.client;

import com.mojang.blaze3d.platform.NativeImage;
import dev.steamcraft.common.client.gui.Gfx;
import dev.steamcraft.core.Log;
import dev.steamcraft.core.skin.SkinData;
import dev.steamcraft.core.skin.SkinImages;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ClientSkins {
	private static final ResourceLocation DEFAULT_ELYTRA = ResourceLocation.withDefaultNamespace("textures/entity/elytra.png");
	private static final Map<UUID, Entry> ENTRIES = new HashMap<>();
	private static int nextTexture;

	private ClientSkins() {
	}

	private static final class Entry {
		final ResourceLocation skin;
		final boolean slim;
		final ResourceLocation cape;
		final ResourceLocation elytra;
		PlayerSkin base;
		PlayerSkin result;

		Entry(ResourceLocation skin, boolean slim, ResourceLocation cape, ResourceLocation elytra) {
			this.skin = skin;
			this.slim = slim;
			this.cape = cape;
			this.elytra = elytra;
		}
	}

	public static void received(UUID player, SkinData skin) {
		set(Minecraft.getInstance(), player, skin);
	}

	public static void disconnected() {
		Minecraft client = Minecraft.getInstance();
		client.execute(() -> clear(client));
	}

	public static PlayerSkin apply(UUID player, PlayerSkin vanilla) {
		Entry entry = ENTRIES.get(player);
		if (entry == null) return vanilla;
		if (entry.base != vanilla) {
			entry.base = vanilla;
			entry.result = Skins.override(vanilla, entry.skin, entry.slim, entry.cape, entry.elytra);
		}
		return entry.result;
	}

	private static void set(Minecraft client, UUID player, SkinData skin) {
		release(client, ENTRIES.remove(player));
		ResourceLocation skinTexture = load(client, skin.skin);
		ResourceLocation capeTexture = load(client, skin.cape);
		ResourceLocation elytra = capeTexture == null ? null : SkinImages.hasElytra(skin.cape) ? capeTexture : DEFAULT_ELYTRA;
		if (skinTexture != null || capeTexture != null) ENTRIES.put(player, new Entry(skinTexture, skin.slim, capeTexture, elytra));
	}

	private static ResourceLocation load(Minecraft client, byte[] png) {
		if (png == null) return null;
		NativeImage image;
		try {
			image = NativeImage.read(png);
		} catch (IOException e) {
			Log.warn("Could not read a skin image from the server", e);
			return null;
		}
		ResourceLocation id = ResourceLocation.fromNamespaceAndPath("steamcraft", "skins/" + nextTexture++);
		client.getTextureManager().register(id, Gfx.texture(id.toString(), image));
		return id;
	}

	private static void clear(Minecraft client) {
		for (Entry entry : ENTRIES.values()) release(client, entry);
		ENTRIES.clear();
	}

	private static void release(Minecraft client, Entry entry) {
		if (entry == null) return;
		if (entry.skin != null) client.getTextureManager().release(entry.skin);
		if (entry.cape != null) client.getTextureManager().release(entry.cape);
	}
}
