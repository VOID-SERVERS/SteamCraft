package dev.steamcraft.neoforge.mixin;

import dev.steamcraft.common.client.ClientSteam;
import dev.steamcraft.neoforge.RawQueryPayload;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.protocol.login.ClientboundCustomQueryPacket;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientHandshakePacketListenerImpl.class)
abstract class ClientHandshakePacketListenerImplMixin {
	@Shadow
	@Final
	private Connection connection;

	@Inject(method = "handleCustomQuery", at = @At("HEAD"), cancellable = true)
	private void steamcraft$answerSteamQuery(ClientboundCustomQueryPacket packet, CallbackInfo ci) {
		if (!(packet.payload() instanceof RawQueryPayload)) return;
		ci.cancel();
		int transactionId = packet.transactionId();
		ClientSteam.answerAuthQuery().thenAccept(response ->
				connection.send(new ServerboundCustomQueryAnswerPacket(transactionId, new RawQueryPayload(response))));
	}

	@Inject(method = "onDisconnect", at = @At("HEAD"))
	private void steamcraft$cancelTickets(DisconnectionDetails details, CallbackInfo ci) {
		ClientSteam.cancelTickets();
	}
}
