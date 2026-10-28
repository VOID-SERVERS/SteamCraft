package dev.steamcraft.core.steam;

import com.codedisaster.steamworks.SteamAPI;
import com.codedisaster.steamworks.SteamLibraryLoader;
import dev.steamcraft.core.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

public final class SteamNatives implements SteamLibraryLoader {
	private final Path directory;

	public SteamNatives(Path directory) {
		this.directory = directory;
	}

	@Override
	public boolean loadLibrary(String name) {
		String file = fileName(name);
		try (InputStream in = SteamAPI.class.getResourceAsStream("/" + file)) {
			if (in == null) {
				Log.warn("Native library " + file + " is not bundled for this platform");
				return false;
			}
			Files.createDirectories(directory);
			Path target = directory.resolve(file);
			Path temp = directory.resolve(file + ".tmp");
			Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
			if (!Files.exists(target) || Files.size(target) != Files.size(temp)) {
				Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
			} else {
				Files.delete(temp);
			}
			System.load(target.toAbsolutePath().toString());
			return true;
		} catch (IOException | UnsatisfiedLinkError e) {
			Log.warn("Could not load native library " + file, e);
			return false;
		}
	}

	static String fileName(String name) {
		String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		if (os.contains("win")) {
			return name + (System.getProperty("os.arch", "").contains("64") ? "64" : "") + ".dll";
		}
		if (os.contains("mac")) return "lib" + name + ".dylib";
		return "lib" + name + ".so";
	}

	public static void writeAppId(int appId) {
		Path file = Paths.get("steam_appid.txt").toAbsolutePath();
		String wanted = Integer.toString(appId);
		try {
			if (Files.isRegularFile(file) && new String(Files.readAllBytes(file), StandardCharsets.US_ASCII).trim().equals(wanted)) return;
			Files.write(file, wanted.getBytes(StandardCharsets.US_ASCII));
			Log.info("Wrote " + file + " (AppID " + wanted + ")");
		} catch (IOException e) {
			Log.warn("Could not write " + file + "; Steam may not start", e);
		}
	}
}
