package dev.steamcraft.neoforge.client;

import dev.steamcraft.common.SkinPayloads;
import dev.steamcraft.common.client.ClientCommands;
import dev.steamcraft.common.client.ClientPlatform;
import dev.steamcraft.common.client.ClientSkins;
import dev.steamcraft.common.client.ClientSteam;
import dev.steamcraft.common.client.CommandFeedback;
import dev.steamcraft.common.client.SkinUploader;
import dev.steamcraft.neoforge.SteamCraftNeoForge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
//? if >=1.21.6 {
/*import net.neoforged.neoforge.client.network.ClientPacketDistributor;
*///?} else {
import net.neoforged.neoforge.network.PacketDistributor;
//?}
import net.neoforged.neoforge.network.payload.MinecraftRegisterPayload;

import java.util.Set;

public final class SteamCraftNeoForgeClient {
	private SteamCraftNeoForgeClient() {
	}

	public static void init() {
		ClientPlatform.set(new ClientPlatform() {
			@Override
			public boolean canSendToServer(CustomPacketPayload.Type<?> type) {
				ClientPacketListener connection = Minecraft.getInstance().getConnection();
				return connection != null && connection.hasChannel(type);
			}

			@Override
			public void sendToServer(CustomPacketPayload payload) {
				//? if >=1.21.6 {
				/*ClientPacketDistributor.sendToServer(payload);
				*///?} else {
				PacketDistributor.sendToServer(payload);
				//?}
			}
		});
		ClientSteam.start();
		SteamCraftNeoForge.setSkinReceiver(payload -> ClientSkins.received(payload.player(), payload.skin()));

		IEventBus events = NeoForge.EVENT_BUS;
		events.addListener((ClientPlayerNetworkEvent.LoggingIn event) -> {
			announceChannels();
			SkinUploader.joined();
		});
		events.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> {
			ClientSteam.cancelTickets();
			ClientSkins.disconnected();
		});
		events.addListener((RegisterClientCommandsEvent event) -> ClientCommands.register(event.getDispatcher(), new CommandFeedback<CommandSourceStack>() {
			@Override
			public void info(CommandSourceStack source, Component message) {
				source.sendSuccess(() -> message, false);
			}

			@Override
			public void error(CommandSourceStack source, Component message) {
				source.sendFailure(message);
			}
		}));
		events.addListener((GameShuttingDownEvent event) -> ClientSteam.stop());
	}

	private static void announceChannels() {
		ClientPacketListener connection = Minecraft.getInstance().getConnection();
		if (connection == null || connection.getConnectionType().isNeoForge()) return;
		connection.send(new MinecraftRegisterPayload(Set.of(SkinPayloads.PlayerSkin.TYPE.id())));
	}
}
