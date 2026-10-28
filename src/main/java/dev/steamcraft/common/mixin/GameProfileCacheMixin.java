package dev.steamcraft.common.mixin;

import dev.steamcraft.common.SteamCraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
//? if >=1.21.9 {
/*import net.minecraft.server.players.CachedUserNameToIdResolver;
import net.minecraft.server.players.NameAndId;
*///?} else {
import com.mojang.authlib.GameProfile;
import net.minecraft.server.players.GameProfileCache;
//?}

//? if >=1.21.9 {
/*@Mixin(CachedUserNameToIdResolver.class)
*///?} else {
@Mixin(GameProfileCache.class)
//?}
abstract class GameProfileCacheMixin {
	@Inject(method = "get(Ljava/lang/String;)Ljava/util/Optional;", at = @At("HEAD"), cancellable = true)
	//? if >=1.21.9 {
	/*private void steamcraft$steamProfile(String name, CallbackInfoReturnable<Optional<NameAndId>> cir) {
		SteamCraftServer context = SteamCraftServer.current();
		if (context != null && context.steamMode()) cir.setReturnValue(context.steamProfile(name).map(NameAndId::new));
	}
	*///?} else {
	private void steamcraft$steamProfile(String name, CallbackInfoReturnable<Optional<GameProfile>> cir) {
		SteamCraftServer context = SteamCraftServer.current();
		if (context != null && context.steamMode()) cir.setReturnValue(context.steamProfile(name));
	}
	//?}
}
