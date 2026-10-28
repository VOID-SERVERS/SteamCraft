package dev.steamcraft.common;

import dev.steamcraft.core.Log;
import dev.steamcraft.core.skin.SkinData;
import dev.steamcraft.core.skin.SkinImages;
import dev.steamcraft.core.skin.SkinStore;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;

public final class SkinSync {
	private SkinSync() {
	}

	public static void uploaded(ServerPlayer player, SkinPayloads.Upload payload) {
		SteamCraftServer steamCraft = SteamCraftServer.current();
		if (steamCraft == null || !steamCraft.skinsEnabled()) return;
		if (steamCraft.skins().submit(player.getUUID(), payload.skin())) publish(steamCraft, player, payload.skin());
	}

	public static void tick(MinecraftServer server) {
		SteamCraftServer steamCraft = SteamCraftServer.current();
		if (steamCraft == null || !steamCraft.skinsEnabled()) return;
		for (Map.Entry<UUID, SkinData> upload : steamCraft.skins().due().entrySet()) {
			ServerPlayer player = server.getPlayerList().getPlayer(upload.getKey());
			if (player != null) publish(steamCraft, player, upload.getValue());
		}
	}

	public static void joined(ServerPlayer player) {
		SteamCraftServer steamCraft = SteamCraftServer.current();
		if (steamCraft == null || !steamCraft.skinsEnabled() || !Platform.get().canSend(player, SkinPayloads.PlayerSkin.TYPE)) return;
		if (!steamCraft.skins().introduce(player.getUUID())) return;
		steamCraft.skins().published().forEach((id, skin) -> Platform.get().send(player, new SkinPayloads.PlayerSkin(id, skin)));
	}

	public static void left(MinecraftServer server, ServerPlayer player) {
		SteamCraftServer steamCraft = SteamCraftServer.current();
		if (steamCraft == null || !steamCraft.skinsEnabled()) return;
		UUID id = player.getUUID();
		steamCraft.skins().remove(id);
		broadcast(server, new SkinPayloads.PlayerSkin(id, SkinData.NONE), id);
	}

	private static void publish(SteamCraftServer steamCraft, ServerPlayer player, SkinData upload) {
		SkinData skin;
		try {
			skin = SkinImages.clean(upload);
		} catch (IllegalArgumentException e) {
			Log.warn("Ignored the skin of " + Profiles.name(player.getGameProfile()) + ": " + e.getMessage());
			return;
		}
		SkinStore skins = steamCraft.skins();
		skins.publish(player.getUUID(), skin);
		broadcast(steamCraft.server(), new SkinPayloads.PlayerSkin(player.getUUID(), skin), null);
	}

	private static void broadcast(MinecraftServer server, SkinPayloads.PlayerSkin update, UUID except) {
		Platform platform = Platform.get();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (player.getUUID().equals(except) || !platform.canSend(player, SkinPayloads.PlayerSkin.TYPE)) continue;
			platform.send(player, update);
		}
	}
}
