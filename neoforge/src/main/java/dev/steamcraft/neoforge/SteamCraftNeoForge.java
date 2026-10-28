package dev.steamcraft.neoforge;

import dev.steamcraft.common.Platform;
import dev.steamcraft.common.ServerCommands;
import dev.steamcraft.common.SkinPayloads;
import dev.steamcraft.common.SkinSync;
import dev.steamcraft.common.SteamCraft;
import dev.steamcraft.common.SteamCraftServer;
import dev.steamcraft.common.SteamLogin;
import dev.steamcraft.common.SteamLoginHandler;
import dev.steamcraft.neoforge.client.SteamCraftNeoForgeClient;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.nio.file.Path;
import java.util.function.Consumer;

@Mod(SteamCraft.MOD_ID)
public final class SteamCraftNeoForge {
	private static volatile Consumer<SkinPayloads.PlayerSkin> skinReceiver = skin -> {
	};

	public SteamCraftNeoForge(IEventBus modBus) {
		SteamCraft.init(new Platform() {
			@Override
			public Path configDir() {
				return FMLPaths.CONFIGDIR.get();
			}

			@Override
			public Path gameDir() {
				return FMLPaths.GAMEDIR.get();
			}

			@Override
			public boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type) {
				return player.connection.hasChannel(type);
			}

			@Override
			public void send(ServerPlayer player, CustomPacketPayload payload) {
				PacketDistributor.sendToPlayer(player, payload);
			}

			@Override
			public void loginVerificationStarting(SteamLoginHandler handler) {
				LoginQuery.start(handler);
			}
		});

		modBus.addListener(SteamCraftNeoForge::registerPayloads);
		IEventBus events = NeoForge.EVENT_BUS;
		events.addListener((RegisterCommandsEvent event) -> ServerCommands.register(event.getDispatcher()));
		events.addListener((ServerStartedEvent event) -> SteamCraftServer.start(event.getServer()));
		events.addListener((ServerStoppingEvent event) -> SteamCraftServer.stop());
		events.addListener((ServerTickEvent.Post event) -> SkinSync.tick(event.getServer()));
		events.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
			if (!(event.getEntity() instanceof ServerPlayer player)) return;
			SteamLogin.joined(player);
			SkinSync.joined(player);
		});
		events.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
			if (!(event.getEntity() instanceof ServerPlayer player)) return;
			SteamLogin.left(player);
			SkinSync.left(player.level().getServer(), player);
		});

		//? if >=1.21.9 {
		/*if (FMLEnvironment.getDist().isClient()) SteamCraftNeoForgeClient.init();
		*///?} else {
		if (FMLEnvironment.dist.isClient()) SteamCraftNeoForgeClient.init();
		//?}
	}

	public static void setSkinReceiver(Consumer<SkinPayloads.PlayerSkin> receiver) {
		skinReceiver = receiver;
	}

	private static void registerPayloads(RegisterPayloadHandlersEvent event) {
		PayloadRegistrar registrar = event.registrar("1").optional();
		registrar.playToServer(SkinPayloads.Upload.TYPE, SkinPayloads.Upload.CODEC,
				(payload, context) -> SkinSync.uploaded((ServerPlayer) context.player(), payload));
		registrar.playToClient(SkinPayloads.PlayerSkin.TYPE, SkinPayloads.PlayerSkin.CODEC,
				(payload, context) -> skinReceiver.accept(payload));
	}
}
