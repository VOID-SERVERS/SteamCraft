package dev.steamcraft.core.skin;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

public final class SkinImages {
	public static final int MAX_PNG_BYTES = 20 * 1024;
	public static final int MAX_FILE_BYTES = 1024 * 1024;

	private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n'};
	private static final int SIZE = 64;

	private SkinImages() {
	}

	public static SkinData clean(SkinData data) {
		return new SkinData(data.slim, data.skin == null ? null : skin(data.skin, MAX_PNG_BYTES), data.cape == null ? null : cape(data.cape, MAX_PNG_BYTES));
	}

	public static byte[] skin(byte[] png, int maxBytes) {
		int[] size = size(png, maxBytes, "skin");
		if (size[0] != SIZE || (size[1] != SIZE && size[1] != SIZE / 2)) {
			throw new IllegalArgumentException("A skin must be 64x64 pixels (or the old 64x32), not " + size[0] + "x" + size[1] + ".");
		}
		boolean legacy = size[1] == SIZE / 2;
		int[] pixels = Arrays.copyOf(decode(png, "skin"), SIZE * SIZE);
		if (legacy) mirrorLegacyLimbs(pixels);
		setOpaque(pixels, 0, 0, 32, 16);
		if (legacy) clearIfFullyOpaque(pixels, 32, 0, 64, 32);
		setOpaque(pixels, 0, 16, 64, 32);
		setOpaque(pixels, 16, 48, 48, 64);
		return encode(pixels, SIZE, SIZE);
	}

	public static byte[] cape(byte[] png, int maxBytes) {
		int[] size = size(png, maxBytes, "cape");
		if (size[0] != SIZE || size[1] != SIZE / 2) {
			throw new IllegalArgumentException("A cape must be 64x32 pixels, not " + size[0] + "x" + size[1] + ".");
		}
		return encode(decode(png, "cape"), SIZE, SIZE / 2);
	}

	public static boolean looksSlim(byte[] png) {
		int[] size = size(png, MAX_FILE_BYTES, "skin");
		if (size[0] != SIZE || size[1] != SIZE) return false;
		int[] pixels = decode(png, "skin");
		for (int y = 20; y < 32; y++) {
			for (int x = 54; x < 56; x++) {
				if (pixels[y * SIZE + x] >>> 24 != 0) return false;
			}
		}
		return true;
	}

	public static boolean hasElytra(byte[] capePng) {
		int[] pixels;
		try {
			pixels = decode(capePng, "cape");
		} catch (IllegalArgumentException e) {
			return false;
		}
		for (int y = 0; y < 22; y++) {
			for (int x = 22; x < 46; x++) {
				int i = y * SIZE + x;
				if (i < pixels.length && pixels[i] >>> 24 != 0) return true;
			}
		}
		return false;
	}

	private static int[] size(byte[] png, int maxBytes, String what) {
		if (png.length > maxBytes) throw new IllegalArgumentException("The " + what + " file is too large (" + png.length / 1024 + " KiB).");
		if (png.length < PNG_SIGNATURE.length || !Arrays.equals(Arrays.copyOf(png, PNG_SIGNATURE.length), PNG_SIGNATURE)) {
			throw new IllegalArgumentException("The " + what + " must be a PNG image.");
		}
		ImageReader reader = pngReader();
		try (ImageInputStream in = new MemoryCacheImageInputStream(new ByteArrayInputStream(png))) {
			reader.setInput(in, true, true);
			return new int[]{reader.getWidth(0), reader.getHeight(0)};
		} catch (IOException | RuntimeException e) {
			throw new IllegalArgumentException("The " + what + " image is damaged.");
		} finally {
			reader.dispose();
		}
	}

	private static int[] decode(byte[] png, String what) {
		ImageReader reader = pngReader();
		try (ImageInputStream in = new MemoryCacheImageInputStream(new ByteArrayInputStream(png))) {
			reader.setInput(in, true, true);
			BufferedImage image = reader.read(0);
			return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
		} catch (IOException | RuntimeException e) {
			throw new IllegalArgumentException("The " + what + " image is damaged.");
		} finally {
			reader.dispose();
		}
	}

	private static ImageReader pngReader() {
		Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("png");
		if (!readers.hasNext()) throw new IllegalStateException("This Java has no PNG support");
		return readers.next();
	}

	private static byte[] encode(int[] pixels, int width, int height) {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		image.setRGB(0, 0, width, height, pixels, 0, width);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (ImageOutputStream stream = new MemoryCacheImageOutputStream(out)) {
			ImageIO.write(image, "png", stream);
		} catch (IOException e) {
			throw new IllegalStateException("Could not write a PNG", e);
		}
		return out.toByteArray();
	}

	private static void mirrorLegacyLimbs(int[] pixels) {
		Arrays.fill(pixels, SIZE * SIZE / 2, SIZE * SIZE, 0);
		copyMirrored(pixels, 4, 16, 16, 32, 4, 4);
		copyMirrored(pixels, 8, 16, 16, 32, 4, 4);
		copyMirrored(pixels, 0, 20, 24, 32, 4, 12);
		copyMirrored(pixels, 4, 20, 16, 32, 4, 12);
		copyMirrored(pixels, 8, 20, 8, 32, 4, 12);
		copyMirrored(pixels, 12, 20, 16, 32, 4, 12);
		copyMirrored(pixels, 44, 16, -8, 32, 4, 4);
		copyMirrored(pixels, 48, 16, -8, 32, 4, 4);
		copyMirrored(pixels, 40, 20, 0, 32, 4, 12);
		copyMirrored(pixels, 44, 20, -8, 32, 4, 12);
		copyMirrored(pixels, 48, 20, -16, 32, 4, 12);
		copyMirrored(pixels, 52, 20, -8, 32, 4, 12);
	}

	private static void copyMirrored(int[] pixels, int x, int y, int dx, int dy, int width, int height) {
		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				pixels[(y + dy + row) * SIZE + x + dx + width - 1 - col] = pixels[(y + row) * SIZE + x + col];
			}
		}
	}

	private static void setOpaque(int[] pixels, int x1, int y1, int x2, int y2) {
		for (int y = y1; y < y2; y++) {
			for (int x = x1; x < x2; x++) pixels[y * SIZE + x] |= 0xFF000000;
		}
	}

	private static void clearIfFullyOpaque(int[] pixels, int x1, int y1, int x2, int y2) {
		for (int y = y1; y < y2; y++) {
			for (int x = x1; x < x2; x++) {
				if (pixels[y * SIZE + x] >>> 24 < 128) return;
			}
		}
		for (int y = y1; y < y2; y++) {
			for (int x = x1; x < x2; x++) pixels[y * SIZE + x] &= 0x00FFFFFF;
		}
	}
}
