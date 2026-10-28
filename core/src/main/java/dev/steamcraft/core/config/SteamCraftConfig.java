package dev.steamcraft.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import dev.steamcraft.core.Log;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class SteamCraftConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	public int steamAppId = 480;
	public Auth auth = new Auth();
	public Coop coop = new Coop();
	public Skin skin = new Skin();
	public Presence presence = new Presence();

	public static final class Auth {
		public int ticketTimeoutSeconds = 10;
	}

	public static final class Coop {
		public CoopAccess access = CoopAccess.FRIENDS;
	}

	public static final class Skin {
		public SkinSource source = SkinSource.MOJANG;
		public SkinModel model = SkinModel.CLASSIC;
		public SkinSource cape = SkinSource.MOJANG;
		public String file;
		public String capeFile;
	}

	public static final class Presence {
		public boolean showWorldName = false;
	}

	private SteamCraftConfig normalize() {
		if (auth == null) auth = new Auth();
		if (auth.ticketTimeoutSeconds <= 0) auth.ticketTimeoutSeconds = 10;
		if (coop == null) coop = new Coop();
		if (coop.access == null) coop.access = CoopAccess.FRIENDS;
		if (skin == null) skin = new Skin();
		if (skin.source == null) skin.source = SkinSource.MOJANG;
		if (skin.model == null) skin.model = SkinModel.CLASSIC;
		if (skin.cape == null) skin.cape = SkinSource.MOJANG;
		if (presence == null) presence = new Presence();
		return this;
	}

	public static SteamCraftConfig load(Path file) {
		SteamCraftConfig config = null;
		String text = null;
		if (Files.isRegularFile(file)) {
			try {
				text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
				config = GSON.fromJson(text, SteamCraftConfig.class);
			} catch (IOException | JsonParseException e) {
				Log.warn("Could not read " + file + ", using defaults", e);
				return new SteamCraftConfig();
			}
		}
		config = config == null ? new SteamCraftConfig() : config.normalize();
		if (!GSON.toJson(config).equals(text)) config.save(file);
		return config;
	}

	public void save(Path file) {
		try {
			writeAtomically(file, GSON.toJson(this));
		} catch (IOException e) {
			Log.warn("Could not save " + file, e);
		}
	}

	public static void writeAtomically(Path file, String content) throws IOException {
		Path parent = file.toAbsolutePath().getParent();
		Files.createDirectories(parent);
		Path temp = parent.resolve(file.getFileName() + ".tmp");
		try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
			writer.write(content);
		}
		Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
	}
}
