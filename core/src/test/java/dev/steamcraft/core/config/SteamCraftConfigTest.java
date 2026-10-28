package dev.steamcraft.core.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SteamCraftConfigTest {
	@TempDir
	Path dir;

	@Test
	void missingFileIsCreatedWithDefaults() {
		Path file = dir.resolve("steamcraft.json");
		SteamCraftConfig config = SteamCraftConfig.load(file);
		assertEquals(CoopAccess.FRIENDS, config.coop.access);
		assertEquals(SkinSource.MOJANG, config.skin.source);
		assertEquals(SkinModel.CLASSIC, config.skin.model);
		assertEquals(SkinSource.MOJANG, config.skin.cape);
		assertFalse(config.presence.showWorldName, "world names stay private unless the player chooses");
		assertTrue(Files.isRegularFile(file));
	}

	@Test
	void outdatedKeysAreDroppedAndValuesKept() throws Exception {
		Path file = dir.resolve("steamcraft.json");
		Files.write(file, "{\"auth\":{\"mode\":\"STEAM\",\"ticketTimeoutSeconds\":5},\"whitelist\":{\"type\":\"ANY\"}}".getBytes(StandardCharsets.UTF_8));
		SteamCraftConfig config = SteamCraftConfig.load(file);
		assertEquals(5, config.auth.ticketTimeoutSeconds);
		String rewritten = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
		assertFalse(rewritten.contains("\"mode\""));
		assertFalse(rewritten.contains("whitelist"));
		assertTrue(rewritten.contains("\"access\": \"FRIENDS\""));
	}

	@Test
	void brokenFileIsLeftAlone() throws Exception {
		Path file = dir.resolve("steamcraft.json");
		Files.write(file, "{ not json".getBytes(StandardCharsets.UTF_8));
		assertEquals(480, SteamCraftConfig.load(file).steamAppId);
		assertEquals("{ not json", new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
	}
}
