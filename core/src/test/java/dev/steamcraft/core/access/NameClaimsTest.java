package dev.steamcraft.core.access;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class NameClaimsTest {
	static final long ALICE = 76561198000000001L;
	static final long BOB = 76561198000000002L;

	@TempDir
	Path dir;

	@Test
	void formerNamesFindThePlayerUntilSomeoneElseTakesThem() {
		NameClaims names = new NameClaims(dir.resolve("names.json"));
		names.claim("Alice", ALICE);
		names.claim("Alicia", ALICE);
		assertNull(names.owner("Alice"));
		assertEquals(ALICE, names.formerOwner("alice"));
		assertNull(names.formerOwner("Alicia"), "a current name is not a former one");

		names.claim("Alice", BOB);
		assertNull(names.formerOwner("Alice"), "taken again: the current owner wins");
		assertEquals(BOB, names.owner("Alice"));
	}

	@Test
	void historySurvivesRestartsAndRenamingBack() {
		NameClaims names = new NameClaims(dir.resolve("names.json"));
		names.claim("Alice", ALICE);
		names.claim("Alicia", ALICE);
		names = new NameClaims(dir.resolve("names.json"));
		assertEquals(ALICE, names.formerOwner("Alice"));

		names.claim("Alice", ALICE);
		assertEquals(ALICE, names.formerOwner("Alicia"));
		assertNull(names.formerOwner("Alice"));
	}

	@Test
	void ambiguousFormerNamesResolveToNobody() {
		NameClaims names = new NameClaims(dir.resolve("names.json"));
		names.claim("Sam", ALICE);
		names.claim("Alice", ALICE);
		names.claim("Sam", BOB);
		names.claim("Bob", BOB);
		assertNull(names.formerOwner("Sam"));
	}
}
