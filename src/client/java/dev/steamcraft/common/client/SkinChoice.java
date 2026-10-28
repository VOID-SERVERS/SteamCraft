package dev.steamcraft.common.client;

import dev.steamcraft.common.Platform;
import dev.steamcraft.common.SteamRuntime;
import dev.steamcraft.core.config.SkinModel;
import dev.steamcraft.core.config.SkinSource;
import dev.steamcraft.core.config.SteamCraftConfig;
import dev.steamcraft.core.skin.SkinData;
import dev.steamcraft.core.skin.SkinImages;
import net.minecraft.client.Minecraft;
//? if <1.21.9
import net.minecraft.client.User;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class SkinChoice {
	public static final String SKINS_DIR = "skins";
	public static final String CAPES_DIR = "capes";

	private SkinChoice() {
	}

	public static SteamCraftConfig.Skin current() {
		return SteamRuntime.config().skin;
	}

	public static Path dir(boolean skin) {
		return Platform.get().gameDir().resolve(skin ? SKINS_DIR : CAPES_DIR);
	}

	public static Path copyOf(boolean skin) {
		return skin ? SkinUploader.SKIN_FILE : SkinUploader.CAPE_FILE;
	}

	public static List<Path> folderPngs(boolean skin) {
		Path dir = dir(skin);
		if (!Files.isDirectory(dir)) return Collections.emptyList();
		try (Stream<Path> list = Files.list(dir)) {
			return list.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png") && Files.isRegularFile(p))
					.sorted()
					.collect(Collectors.toList());
		} catch (IOException e) {
			return Collections.emptyList();
		}
	}

	public static boolean hasAccount() {
		//? if >=1.21.9 {
		/*return Minecraft.getInstance().getUser().getXuid().isPresent();
		*///?} else {
		return Minecraft.getInstance().getUser().getType() == User.Type.MSA;
		//?}
	}

	public static byte[] copyFile(Path file, boolean skin) throws IOException {
		if (Files.size(file) > SkinImages.MAX_FILE_BYTES) {
			throw new IllegalArgumentException("That file is too large for a " + (skin ? "skin." : "cape."));
		}
		byte[] picked = Files.readAllBytes(file);
		byte[] image = skin ? SkinImages.skin(picked, SkinImages.MAX_FILE_BYTES) : SkinImages.cape(picked, SkinImages.MAX_FILE_BYTES);
		Path copy = copyOf(skin);
		Files.createDirectories(copy.getParent());
		Files.write(copy, image);
		return picked;
	}

	public static SkinModel useSkinFile(Path file) throws IOException {
		byte[] picked = copyFile(file, true);
		SkinModel model = SkinImages.looksSlim(picked) ? SkinModel.SLIM : SkinModel.CLASSIC;
		current().source = SkinSource.FILE;
		current().model = model;
		current().file = file.getFileName().toString();
		return model;
	}

	public static void useCapeFile(Path file) throws IOException {
		copyFile(file, false);
		current().cape = SkinSource.FILE;
		current().capeFile = file.getFileName().toString();
	}

	public static void setSkinSource(SkinSource source) {
		if (source == SkinSource.MOJANG) SkinUploader.forgetAccount();
		current().source = source;
	}

	public static void setCapeSource(SkinSource source) {
		if (source == SkinSource.MOJANG) SkinUploader.forgetAccount();
		current().cape = source;
	}

	public static void setModel(SkinModel model) {
		current().model = model;
	}

	public static void saveAndUpload(Consumer<SkinData> sent) {
		SteamRuntime.saveConfig();
		SkinUploader.upload(Minecraft.getInstance(), sent);
	}
}
