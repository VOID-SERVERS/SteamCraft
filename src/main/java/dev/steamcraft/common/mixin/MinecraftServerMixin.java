package dev.steamcraft.common.mixin;

import dev.steamcraft.common.SteamCraftServer;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;
//? if >=1.21.9 {
/*import net.minecraft.server.players.NameAndId;
*///?} else {
import com.mojang.authlib.GameProfile;
import dev.steamcraft.common.Profiles;
//?}
//? if >=1.21.11
/*import net.minecraft.server.permissions.LevelBasedPermissionSet;*/

@Mixin(MinecraftServer.class)
abstract class MinecraftServerMixin {
	@Inject(method = "getProfilePermissions", at = @At("HEAD"), cancellable = true)
	//? if >=1.21.11 {
	/*private void steamcraft$coopPermissions(NameAndId player, CallbackInfoReturnable<LevelBasedPermissionSet> cir) {
		Integer level = level(player.id(), ((MinecraftServer) (Object) this).isSingleplayerOwner(player));
		if (level != null) cir.setReturnValue(level >= 4 ? LevelBasedPermissionSet.OWNER : LevelBasedPermissionSet.ALL);
	}
	*///?} else if >=1.21.9 {
	/*private void steamcraft$coopPermissions(NameAndId player, CallbackInfoReturnable<Integer> cir) {
		Integer level = level(player.id(), ((MinecraftServer) (Object) this).isSingleplayerOwner(player));
		if (level != null) cir.setReturnValue(level);
	}
	*///?} else {
	private void steamcraft$coopPermissions(GameProfile profile, CallbackInfoReturnable<Integer> cir) {
		Integer level = level(Profiles.id(profile), Profiles.isOwner((MinecraftServer) (Object) this, profile));
		if (level != null) cir.setReturnValue(level);
	}
	//?}

	private Integer level(UUID player, boolean owner) {
		SteamCraftServer context = SteamCraftServer.current();
		if (context == null || context.server() != (Object) this) return null;
		return context.coopPermissionLevel(player, owner);
	}
}
