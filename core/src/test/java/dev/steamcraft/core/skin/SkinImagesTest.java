package dev.steamcraft.core.skin;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class SkinImagesTest {
	static final int RED = 0xFFFF0000;
	static final int MAX = SkinImages.MAX_PNG_BYTES;

	static BufferedImage image(int width, int height) {
		return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
	}

	static byte[] png(BufferedImage image) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(image, "png", out);
		return out.toByteArray();
	}

	static BufferedImage read(byte[] png) throws IOException {
		return ImageIO.read(new ByteArrayInputStream(png));
	}

	@Test
	void legacySkinsGetMirroredLimbsLikeInVanilla() throws IOException {
		BufferedImage legacy = image(64, 32);
		legacy.setRGB(4, 20, RED);
		BufferedImage skin = read(SkinImages.skin(png(legacy), MAX));
		assertEquals(64, skin.getWidth());
		assertEquals(64, skin.getHeight());
		assertEquals(RED, skin.getRGB(23, 52), "the left leg's front is the mirrored right one");
	}

	@Test
	void theBaseLayerIsOpaqueAndTheHatLayerIsNot() throws IOException {
		BufferedImage modern = image(64, 64);
		BufferedImage skin = read(SkinImages.skin(png(modern), MAX));
		assertEquals(0xFF, skin.getRGB(10, 10) >>> 24, "head");
		assertEquals(0xFF, skin.getRGB(20, 52) >>> 24, "left leg");
		assertEquals(0, skin.getRGB(40, 10) >>> 24, "hat stays see-through");
	}

	@Test
	void anOpaqueHatOnAnOldSkinIsHidden() throws IOException {
		BufferedImage legacy = image(64, 32);
		for (int y = 0; y < 32; y++) for (int x = 32; x < 64; x++) legacy.setRGB(x, y, RED);
		BufferedImage skin = read(SkinImages.skin(png(legacy), MAX));
		assertEquals(0, skin.getRGB(40, 10) >>> 24);
	}

	@Test
	void redrawingTwiceChangesNothing() throws IOException {
		BufferedImage modern = image(64, 64);
		for (int i = 0; i < 64; i++) modern.setRGB(i, i, 0x80123456);
		byte[] once = SkinImages.skin(png(modern), MAX);
		assertArrayEquals(once, SkinImages.skin(once, MAX));
	}

	@Test
	void wrongSizesAndNonImagesAreRefused() throws IOException {
		assertTrue(refusal(() -> SkinImages.skin(png(image(128, 128)), MAX)).contains("64x64"));
		assertTrue(refusal(() -> SkinImages.cape(png(image(64, 64)), MAX)).contains("64x32"));
		assertTrue(refusal(() -> SkinImages.skin("GIF89a".getBytes(), MAX)).contains("PNG"));
		byte[] damaged = Arrays.copyOf(png(image(64, 64)), 30);
		assertTrue(refusal(() -> SkinImages.skin(damaged, MAX)).contains("damaged"));
		assertTrue(refusal(() -> SkinImages.skin(new byte[MAX + 1], MAX)).contains("too large"));
	}

	@Test
	void elytraAreFoundOnlyWhereMinecraftLooksForThem() throws IOException {
		BufferedImage capeOnly = image(64, 32);
		for (int y = 0; y < 17; y++) for (int x = 0; x < 22; x++) capeOnly.setRGB(x, y, RED);
		assertFalse(SkinImages.hasElytra(png(capeOnly)));

		BufferedImage withElytra = image(64, 32);
		withElytra.setRGB(45, 21, RED);
		assertTrue(SkinImages.hasElytra(png(withElytra)));
		assertFalse(SkinImages.hasElytra("not a png".getBytes()));
	}

	@Test
	void capesKeepTheirPixels() throws IOException {
		BufferedImage cape = image(64, 32);
		cape.setRGB(1, 1, 0x40FF0000);
		assertEquals(0x40FF0000, read(SkinImages.cape(png(cape), MAX)).getRGB(1, 1));
	}

	@Test
	void slimArmsAreRecognised() throws IOException {
		BufferedImage slim = image(64, 64);
		BufferedImage classic = image(64, 64);
		for (int y = 20; y < 32; y++) for (int x = 40; x < 56; x++) classic.setRGB(x, y, RED);
		for (int y = 20; y < 32; y++) for (int x = 40; x < 54; x++) slim.setRGB(x, y, RED);
		assertTrue(SkinImages.looksSlim(png(slim)));
		assertFalse(SkinImages.looksSlim(png(classic)));
		assertFalse(SkinImages.looksSlim(png(image(64, 32))), "old skins are classic");
	}

	interface Action {
		void run() throws IOException;
	}

	static String refusal(Action action) {
		return assertThrows(IllegalArgumentException.class, action::run).getMessage();
	}
}
