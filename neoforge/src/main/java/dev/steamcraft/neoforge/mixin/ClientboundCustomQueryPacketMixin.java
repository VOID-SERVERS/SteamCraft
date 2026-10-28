package dev.steamcraft.neoforge.mixin;

import dev.steamcraft.common.SteamLogin;
import dev.steamcraft.neoforge.RawQueryPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.login.ClientboundCustomQueryPacket;
import net.minecraft.network.protocol.login.custom.CustomQueryPayload;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientboundCustomQueryPacket.class)
abstract class ClientboundCustomQueryPacketMixin {
	@Inject(method = "readPayload", at = @At("HEAD"), cancellable = true)
	private static void steamcraft$readSteamQuery(ResourceLocation id, FriendlyByteBuf buf, CallbackInfoReturnable<CustomQueryPayload> cir) {
		if (id.equals(SteamLogin.CHANNEL)) cir.setReturnValue(RawQueryPayload.read(buf));
	}
}
