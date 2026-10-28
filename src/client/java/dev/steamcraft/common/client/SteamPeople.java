package dev.steamcraft.common.client;

import com.mojang.blaze3d.platform.NativeImage;
import dev.steamcraft.common.SteamRuntime;
import dev.steamcraft.common.client.gui.Gfx;
import dev.steamcraft.core.Log;
import dev.steamcraft.core.steam.SteamClient;
import dev.steamcraft.core.steam.SteamFriend;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class SteamPeople {
	private static final long REFRESH_MILLIS = 3000;
	private static final long IDLE_MILLIS = 15000;
	private static final long AVATAR_RETRY_MILLIS = 5000;

	private static volatile List<SteamFriend> friends = Collections.emptyList();
	private static volatile Map<Long, SteamFriend> friendsById = Collections.emptyMap();
	private static volatile long lastWanted;
	private static Thread refresher;
	private static final Map<Long, String> personaNames = new ConcurrentHashMap<>();
	private static final Set<Long> namesLoading = ConcurrentHashMap.newKeySet();

	private static final Map<Long, AvatarTexture> avatars = new HashMap<>();
	private static final Map<Long, Long> avatarRetryAt = new HashMap<>();
	private static final Set<Long> avatarsLoading = ConcurrentHashMap.newKeySet();

	private SteamPeople() {
	}

	public static List<SteamFriend> friends() {
		wanted();
		return friends;
	}

	public static SteamFriend friend(long steamId) {
		wanted();
		return friendsById.get(steamId);
	}

	public static void refreshSoon() {
		wanted();
		Thread thread = refresher;
		if (thread != null) thread.interrupt();
	}

	public static String personaName(long steamId) {
		SteamFriend friend = friendsById.get(steamId);
		if (friend != null) return friend.name;
		String known = personaNames.get(steamId);
		if (known != null) return known;
		SteamClient steam = SteamRuntime.client();
		if (steam != null && namesLoading.add(steamId)) {
			Util.backgroundExecutor().execute(() -> {
				try {
					String name = steam.friendName(steamId);
					if (name != null && !name.isEmpty() && !name.equals("[unknown]")) personaNames.put(steamId, name);
				} catch (RuntimeException e) {
					Log.debug("Could not get the Steam name of " + steamId + ": " + e.getMessage());
				} finally {
					namesLoading.remove(steamId);
				}
			});
		}
		return null;
	}

	private static synchronized void wanted() {
		lastWanted = System.currentTimeMillis();
		if (refresher != null) return;
		refresher = new Thread(SteamPeople::refreshLoop, "steamcraft-friends");
		refresher.setDaemon(true);
		refresher.start();
	}

	private static void refreshLoop() {
		while (true) {
			SteamClient steam = SteamRuntime.client();
			if (steam != null && System.currentTimeMillis() - lastWanted < IDLE_MILLIS) {
				try {
					List<SteamFriend> list = steam.friendList();
					Map<Long, SteamFriend> byId = new HashMap<>();
					for (SteamFriend friend : list) byId.put(friend.steamId, friend);
					friends = Collections.unmodifiableList(list);
					friendsById = byId;
				} catch (RuntimeException e) {
					Log.debug("Could not refresh Steam friends: " + e.getMessage());
				}
			}
			try {
				Thread.sleep(REFRESH_MILLIS);
			} catch (InterruptedException e) {
			}
		}
	}

	public static void drawAvatar(GuiGraphics graphics, long steamId, int x, int y, int size) {
		AvatarTexture texture = avatar(steamId);
		if (texture == null) {
			graphics.fill(x, y, x + size, y + size, 0xFF3A3F47);
			return;
		}
		Gfx.blit(graphics, texture.id, x, y, size, size, 0, 0, texture.width, texture.height, texture.width, texture.height);
	}

	private record AvatarTexture(ResourceLocation id, int width, int height) {
	}

	private static AvatarTexture avatar(long steamId) {
		AvatarTexture texture = avatars.get(steamId);
		if (texture != null) return texture;
		SteamClient steam = SteamRuntime.client();
		Long retryAt = avatarRetryAt.get(steamId);
		if (steam == null || retryAt != null && System.currentTimeMillis() < retryAt || !avatarsLoading.add(steamId)) return null;
		Minecraft minecraft = Minecraft.getInstance();
		Util.backgroundExecutor().execute(() -> {
			SteamClient.Avatar avatar;
			try {
				avatar = steam.smallAvatar(steamId);
			} catch (RuntimeException e) {
				avatar = null;
			}
			SteamClient.Avatar loaded = avatar;
			minecraft.execute(() -> {
				avatarsLoading.remove(steamId);
				if (loaded == null) {
					avatarRetryAt.put(steamId, System.currentTimeMillis() + AVATAR_RETRY_MILLIS);
					return;
				}
				ResourceLocation id = ResourceLocation.fromNamespaceAndPath("steamcraft", "avatars/" + Long.toUnsignedString(steamId));
				minecraft.getTextureManager().register(id, Gfx.texture(id.toString(), toImage(loaded)));
				avatars.put(steamId, new AvatarTexture(id, loaded.width, loaded.height));
			});
		});
		return null;
	}

	private static NativeImage toImage(SteamClient.Avatar avatar) {
		NativeImage image = new NativeImage(avatar.width, avatar.height, false);
		byte[] rgba = avatar.rgba;
		for (int y = 0; y < avatar.height; y++) {
			for (int x = 0; x < avatar.width; x++) {
				int i = (y * avatar.width + x) * 4;
				int abgr = (rgba[i + 3] & 0xFF) << 24 | (rgba[i + 2] & 0xFF) << 16 | (rgba[i + 1] & 0xFF) << 8 | rgba[i] & 0xFF;
				Gfx.setPixel(image, x, y, abgr);
			}
		}
		return image;
	}
}
