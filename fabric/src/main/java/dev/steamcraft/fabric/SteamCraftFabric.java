package dev.steamcraft.fabric;

import dev.steamcraft.common.Platform;
import dev.steamcraft.common.ServerCommands;
import dev.steamcraft.common.SkinPayloads;
import dev.steamcraft.common.SkinSync;
import dev.steamcraft.common.SteamCraft;
import dev.steamcraft.common.SteamCraftServer;
import dev.steamcraft.common.SteamLogin;
import dev.steamcraft.common.SteamLoginHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.S2CPlayChannelEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;

public final class SteamCraftFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		SteamCraft.init(new Platform() {
			@Override
			public Path configDir() {
				return FabricLoader.getInstance().getConfigDir();
			}

			@Override
			public Path gameDir() {
				return FabricLoader.getInstance().getGameDir();
			}

			@Override
			public boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type) {
				return ServerPlayNetworking.canSend(player, type);
			}

			@Override
			public void send(ServerPlayer player, CustomPacketPayload payload) {
				ServerPlayNetworking.send(player, payload);
			}
		});

		registerLogin();
		registerSkins();
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> ServerCommands.register(dispatcher));
		ServerLifecycleEvents.SERVER_STARTED.register(SteamCraftServer::start);
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> SteamCraftServer.stop());
	}

	private static void registerLogin() {
		ServerLoginConnectionEvents.QUERY_START.register((handler, server, sender, synchronizer) -> {
			byte[] request = SteamLogin.start((SteamLoginHandler) handler);
			if (request == null) return;
			FriendlyByteBuf buf = PacketByteBufs.create();
			buf.writeBytes(request);
			sender.sendPacket(SteamLogin.CHANNEL, buf);
		});
		ServerLoginNetworking.registerGlobalReceiver(SteamLogin.CHANNEL, (server, handler, understood, buf, synchronizer, responseSender) -> {
			byte[] response = null;
			if (understood) {
				response = new byte[buf.readableBytes()];
				buf.readBytes(response);
			}
			synchronizer.waitFor(SteamLogin.answered(server, (SteamLoginHandler) handler, response));
		});
		ServerLoginConnectionEvents.DISCONNECT.register((handler, server) -> SteamLogin.loginDisconnected((SteamLoginHandler) handler));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> SteamLogin.joined(handler.getPlayer()));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> SteamLogin.left(handler.getPlayer()));
	}

	private static void registerSkins() {
		PayloadTypeRegistry.playC2S().register(SkinPayloads.Upload.TYPE, SkinPayloads.Upload.CODEC);
		PayloadTypeRegistry.playS2C().register(SkinPayloads.PlayerSkin.TYPE, SkinPayloads.PlayerSkin.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(SkinPayloads.Upload.TYPE, (payload, context) -> SkinSync.uploaded(context.player(), payload));
		ServerTickEvents.END_SERVER_TICK.register(SkinSync::tick);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> SkinSync.joined(handler.getPlayer()));
		S2CPlayChannelEvents.REGISTER.register((handler, sender, server, channels) -> {
			if (channels.contains(SkinPayloads.PlayerSkin.TYPE.id())) server.execute(() -> SkinSync.joined(handler.getPlayer()));
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> SkinSync.left(server, handler.getPlayer()));
	}
}
