package dev.steamcraft.neoforge.mixin;

import dev.steamcraft.common.SteamLogin;
import dev.steamcraft.common.SteamLoginHandler;
import dev.steamcraft.neoforge.LoginQuery;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLoginPacketListenerImpl.class)
abstract class ServerLoginPacketListenerImplMixin {
	@Shadow
	@Final
	MinecraftServer server;

	@Inject(method = "handleCustomQueryPacket", at = @At("HEAD"), cancellable = true)
	private void steamcraft$steamAnswer(ServerboundCustomQueryAnswerPacket packet, CallbackInfo ci) {
		if (LoginQuery.answered(server, (SteamLoginHandler) this, packet)) ci.cancel();
	}

	@Inject(method = "onDisconnect", at = @At("HEAD"))
	private void steamcraft$leftDuringLogin(DisconnectionDetails details, CallbackInfo ci) {
		SteamLogin.loginDisconnected((SteamLoginHandler) this);
	}
}
