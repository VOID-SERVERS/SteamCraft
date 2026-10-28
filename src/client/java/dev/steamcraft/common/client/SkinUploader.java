package dev.steamcraft.common.client;

import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import dev.steamcraft.core.Log;
import dev.steamcraft.core.config.SkinModel;
import dev.steamcraft.core.config.SkinSource;
import dev.steamcraft.core.config.SteamCraftConfig;
import dev.steamcraft.core.skin.SkinData;
import dev.steamcraft.core.skin.SkinImages;
import dev.steamcraft.common.SkinPayloads;
import dev.steamcraft.common.Platform;
import dev.steamcraft.common.SteamRuntime;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class SkinUploader {
	static final Path SKIN_FILE = Platform.get().configDir().resolve("steamcraft").resolve("skin.png");
	static final Path CAPE_FILE = Platform.get().configDir().resolve("steamcraft").resolve("cape.png");

	private static final Duration TIMEOUT = Duration.ofSeconds(15);
	private static volatile SkinData account;

	private SkinUploader() {
	}

	public static void joined() {
		upload(Minecraft.getInstance(), skin -> {
		});
	}

	static void upload(Minecraft client, Consumer<SkinData> done) {
		SteamCraftConfig.Skin config = SteamRuntime.config().skin;
		boolean needsAccount = config.source == SkinSource.MOJANG || config.cape == SkinSource.MOJANG;
		CompletableFuture<SkinData> fromAccount = needsAccount ? account(client) : CompletableFuture.completedFuture(SkinData.NONE);
		fromAccount.thenApplyAsync(accountSkin -> choose(config, accountSkin), Util.ioPool()).thenAcceptAsync(skin -> {
			ClientPlatform platform = ClientPlatform.get();
			if (platform.canSendToServer(SkinPayloads.Upload.TYPE)) platform.sendToServer(new SkinPayloads.Upload(skin));
			done.accept(skin);
		}, client);
	}

	static void forgetAccount() {
		account = null;
	}

	private static SkinData choose(SteamCraftConfig.Skin config, SkinData accountSkin) {
		byte[] skin;
		boolean slim;
		switch (config.source) {
			case MOJANG:
				skin = accountSkin.skin;
				slim = accountSkin.slim;
				break;
			case FILE:
				skin = readCopy(SKIN_FILE, true);
				slim = config.model == SkinModel.SLIM;
				break;
			default:
				skin = null;
				slim = false;
		}
		byte[] cape;
		switch (config.cape) {
			case MOJANG:
				cape = accountSkin.cape;
				break;
			case FILE:
				cape = readCopy(CAPE_FILE, false);
				break;
			default:
				cape = null;
		}
		return new SkinData(slim, skin, cape);
	}

	private static byte[] readCopy(Path file, boolean skin) {
		try {
			byte[] png = Files.readAllBytes(file);
			return skin ? SkinImages.skin(png, SkinImages.MAX_PNG_BYTES) : SkinImages.cape(png, SkinImages.MAX_PNG_BYTES);
		} catch (IOException | IllegalArgumentException e) {
			Log.warn("Could not use " + file + ": " + e.getMessage());
			return null;
		}
	}

	private static CompletableFuture<SkinData> account(Minecraft client) {
		SkinData known = account;
		if (known != null) return CompletableFuture.completedFuture(known);
		return CompletableFuture.supplyAsync(() -> account = fetchAccount(client), Util.nonCriticalIoPool())
				.exceptionally(e -> {
					Log.warn("Could not fetch the skin of your Minecraft account", e);
					return SkinData.NONE;
				});
	}

	private static SkinData fetchAccount(Minecraft client) {
		//? if >=1.21.9 {
		/*MinecraftProfileTextures textures = client.services().sessionService().getTextures(client.getGameProfile());
		*///?} else {
		MinecraftProfileTextures textures = client.getMinecraftSessionService().getTextures(client.getGameProfile());
		//?}
		MinecraftProfileTexture skin = textures.skin();
		MinecraftProfileTexture cape = textures.cape();
		return new SkinData(
				skin != null && "slim".equals(skin.getMetadata("model")),
				skin == null ? null : SkinImages.skin(download(skin.getUrl()), SkinImages.MAX_FILE_BYTES),
				cape == null ? null : SkinImages.cape(download(cape.getUrl()), SkinImages.MAX_FILE_BYTES));
	}

	private static byte[] download(String url) {
		try (HttpClient http = HttpClient.newBuilder().connectTimeout(TIMEOUT).followRedirects(HttpClient.Redirect.NORMAL).build()) {
			HttpResponse<byte[]> response = http.send(HttpRequest.newBuilder(URI.create(url)).timeout(TIMEOUT).build(), HttpResponse.BodyHandlers.ofByteArray());
			if (response.statusCode() != 200) throw new IllegalStateException("HTTP " + response.statusCode() + " from " + url);
			return response.body();
		} catch (IOException e) {
			throw new IllegalStateException("Could not download " + url, e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while downloading " + url, e);
		}
	}
}
