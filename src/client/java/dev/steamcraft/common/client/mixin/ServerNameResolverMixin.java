package dev.steamcraft.common.client.mixin;

import dev.steamcraft.core.identity.SteamIds;
import dev.steamcraft.common.client.SteamJoin;
import net.minecraft.client.multiplayer.resolver.ResolvedServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerNameResolver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(ServerNameResolver.class)
abstract class ServerNameResolverMixin {
	@Inject(method = "resolveAddress", at = @At("HEAD"), cancellable = true)
	private void steamcraft$resolveSteamAddress(ServerAddress address, CallbackInfoReturnable<Optional<ResolvedServerAddress>> cir) {
		Long host = SteamIds.fromAddressHost(address.getHost());
		if (host != null) cir.setReturnValue(SteamJoin.resolve(host));
	}
}
