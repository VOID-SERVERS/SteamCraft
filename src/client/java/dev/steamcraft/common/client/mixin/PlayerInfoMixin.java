package dev.steamcraft.common.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.authlib.GameProfile;
import dev.steamcraft.common.client.ClientSkins;
import dev.steamcraft.common.Profiles;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerInfo.class)
abstract class PlayerInfoMixin {
	@Shadow
	public abstract GameProfile getProfile();

	@ModifyReturnValue(method = "getSkin", at = @At("RETURN"))
	private PlayerSkin steamcraft$steamSkin(PlayerSkin vanilla) {
		return ClientSkins.apply(Profiles.id(getProfile()), vanilla);
	}
}
