package dev.steamcraft.core.access;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class CoopOperatorsTest {
	static final long ALICE = 76561198000000001L;
	static final long BOB = 76561198000000002L;

	@TempDir
	Path dir;

	@Test
	void addAndRemoveReportWhetherAnythingChanged() {
		CoopOperators ops = new CoopOperators(dir.resolve("ops.json"));
		assertTrue(ops.add(ALICE, "Alice"));
		assertFalse(ops.add(ALICE, "Alice"), "already an operator");
		assertTrue(ops.contains(ALICE));
		assertFalse(ops.contains(BOB));

		assertTrue(ops.remove(ALICE));
		assertFalse(ops.remove(ALICE), "no longer an operator");
		assertFalse(ops.contains(ALICE));
	}

	@Test
	void operatorsSurviveRestartsInOrder() {
		CoopOperators ops = new CoopOperators(dir.resolve("ops.json"));
		ops.add(BOB, "Bob");
		ops.add(ALICE, "Alice");
		ops = new CoopOperators(dir.resolve("ops.json"));
		assertEquals(Arrays.asList(BOB, ALICE), Arrays.asList(ops.all().keySet().toArray()));
		assertEquals("Alice", ops.all().get(ALICE));
	}

	@Test
	void brokenEntriesAreSkipped() throws IOException {
		Files.write(dir.resolve("ops.json"), ("[{\"steamId\":\"76561198000000001\",\"name\":\"Alice\"},"
				+ "{\"steamId\":\"not a number\"},{\"name\":\"Nobody\"},null]").getBytes(StandardCharsets.UTF_8));
		CoopOperators ops = new CoopOperators(dir.resolve("ops.json"));
		assertEquals(1, ops.all().size());
		assertTrue(ops.contains(ALICE));
	}
}
