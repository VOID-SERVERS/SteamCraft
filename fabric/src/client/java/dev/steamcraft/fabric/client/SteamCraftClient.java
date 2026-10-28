package dev.steamcraft.fabric.client;

import dev.steamcraft.common.SkinPayloads;
import dev.steamcraft.common.SteamLogin;
import dev.steamcraft.common.client.ClientCommands;
import dev.steamcraft.common.client.ClientPlatform;
import dev.steamcraft.common.client.ClientSkins;
import dev.steamcraft.common.client.ClientSteam;
import dev.steamcraft.common.client.CommandFeedback;
import dev.steamcraft.common.client.SkinUploader;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class SteamCraftClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlatform.set(new ClientPlatform() {
			@Override
			public boolean canSendToServer(CustomPacketPayload.Type<?> type) {
				return ClientPlayNetworking.canSend(type);
			}

			@Override
			public void sendToServer(CustomPacketPayload payload) {
				ClientPlayNetworking.send(payload);
			}
		});
		ClientSteam.start();

		ClientLoginNetworking.registerGlobalReceiver(SteamLogin.CHANNEL, (client, handler, buf, callbacks) ->
				ClientSteam.answerAuthQuery().thenApply(response -> {
					FriendlyByteBuf answer = PacketByteBufs.create();
					answer.writeBytes(response);
					return answer;
				}));
		ClientLoginConnectionEvents.DISCONNECT.register((handler, client) -> ClientSteam.cancelTickets());
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> SkinUploader.joined());
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			ClientSteam.cancelTickets();
			ClientSkins.disconnected();
		});
		ClientPlayNetworking.registerGlobalReceiver(SkinPayloads.PlayerSkin.TYPE, (payload, context) -> ClientSkins.received(payload.player(), payload.skin()));
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> ClientCommands.register(dispatcher, new CommandFeedback<FabricClientCommandSource>() {
			@Override
			public void info(FabricClientCommandSource source, Component message) {
				source.sendFeedback(message);
			}

			@Override
			public void error(FabricClientCommandSource source, Component message) {
				source.sendError(message);
			}
		}));
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ClientSteam.stop());
	}
}
