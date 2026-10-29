package dev.steamcraft.common.client.mixin;

import dev.steamcraft.common.client.PresencePublisher;
import dev.steamcraft.common.client.SkinUploader;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
abstract class MinecraftMixin {
	@Inject(method = "tick", at = @At("TAIL"))
	private void steamcraft$tick(CallbackInfo ci) {
		PresencePublisher.tick();
		SkinUploader.tick();
	}
}
