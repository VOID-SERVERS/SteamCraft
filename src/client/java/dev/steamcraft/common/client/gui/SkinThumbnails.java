package dev.steamcraft.common.client.gui;

import com.mojang.blaze3d.platform.NativeImage;
import dev.steamcraft.core.Log;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class SkinThumbnails {
	private record Entry(long modified, ResourceLocation texture, int width, int height) {
	}

	private static final Map<Path, Entry> ENTRIES = new HashMap<>();
	private static int nextId;

	private SkinThumbnails() {
	}

	static void prepare(List<Path> files) {
		Minecraft minecraft = Minecraft.getInstance();
		for (Path file : files) {
			long modified;
			try {
				modified = Files.getLastModifiedTime(file).toMillis();
			} catch (IOException e) {
				continue;
			}
			Entry known = ENTRIES.get(file);
			if (known != null && known.modified() == modified) continue;
			if (known != null && known.texture() != null) minecraft.getTextureManager().release(known.texture());
			ENTRIES.put(file, load(minecraft, file, modified));
		}
	}

	private static Entry load(Minecraft minecraft, Path file, long modified) {
		try {
			if (Files.size(file) > 1024 * 1024) return new Entry(modified, null, 0, 0);
			NativeImage image = NativeImage.read(Files.readAllBytes(file));
			int width = image.getWidth();
			int height = image.getHeight();
			ResourceLocation id = ResourceLocation.fromNamespaceAndPath("steamcraft", "thumbnails/" + nextId++);
			minecraft.getTextureManager().register(id, Gfx.texture(id.toString(), image));
			return new Entry(modified, id, width, height);
		} catch (IOException e) {
			return new Entry(modified, null, 0, 0);
		}
	}

	static ResourceLocation loadPreview(byte[] png, ResourceLocation id) {
		try {
			Minecraft.getInstance().getTextureManager().register(id, Gfx.texture(id.toString(), NativeImage.read(png)));
			return id;
		} catch (IOException e) {
			return null;
		}
	}

	static ResourceLocation loadPreview(Path file, ResourceLocation id) {
		if (!Files.isRegularFile(file)) return null;
		try {
			return loadPreview(Files.readAllBytes(file), id);
		} catch (IOException e) {
			Log.warn("Could not show " + file, e);
			return null;
		}
	}

	static void drawFace(GuiGraphics graphics, Path file, int x, int y, int size) {
		Entry entry = ENTRIES.get(file);
		if (entry == null || entry.texture() == null || entry.width() != 64) return;
		Gfx.blended(() -> {
			Gfx.blit(graphics, entry.texture(), x, y, size, size, 8, 8, 8, 8, entry.width(), entry.height());
			Gfx.blit(graphics, entry.texture(), x, y, size, size, 40, 8, 8, 8, entry.width(), entry.height());
		});
	}

	static void drawCape(GuiGraphics graphics, Path file, int x, int y, int height) {
		Entry entry = ENTRIES.get(file);
		if (entry == null || entry.texture() == null || entry.width() != 64) return;
		int width = Math.max(1, height * 10 / 16);
		Gfx.blit(graphics, entry.texture(), x, y, width, height, 1, 1, 10, 16, entry.width(), entry.height());
	}
}
