package dev.steamcraft.core.access;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import dev.steamcraft.core.Log;
import dev.steamcraft.core.config.SteamCraftConfig;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

abstract class JsonListStore<E> {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	private final Path file;
	private final Class<E> entryType;

	JsonListStore(Path file, Class<E> entryType) {
		this.file = file;
		this.entryType = entryType;
	}

	final List<E> read() {
		if (!Files.isRegularFile(file)) return new ArrayList<>();
		try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			List<E> entries = GSON.fromJson(reader, TypeToken.getParameterized(List.class, entryType).getType());
			List<E> valid = new ArrayList<>();
			if (entries != null) {
				for (E entry : entries) {
					if (entry != null && isValid(entry)) valid.add(entry);
				}
			}
			return valid;
		} catch (IOException | JsonParseException e) {
			Log.warn("Could not read " + file + ", starting empty", e);
			return new ArrayList<>();
		}
	}

	final void write(List<E> entries) {
		try {
			SteamCraftConfig.writeAtomically(file, GSON.toJson(entries));
		} catch (IOException e) {
			Log.warn("Could not save " + file, e);
		}
	}

	abstract boolean isValid(E entry);

	static Long parseSteamId(String text) {
		if (text == null) return null;
		try {
			return Long.parseUnsignedLong(text);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
