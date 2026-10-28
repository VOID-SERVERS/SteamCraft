package dev.steamcraft.common.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;
import dev.steamcraft.common.Platform;
import dev.steamcraft.common.SteamCraftServer;
import dev.steamcraft.common.SteamLoginHandler;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLoginPacketListenerImpl.class)
abstract class ServerLoginPacketListenerImplMixin implements SteamLoginHandler {
	@Shadow
	@Final
	Connection connection;
	@Shadow
	String requestedUsername;
	@Shadow
	private GameProfile authenticatedProfile;

	@Unique
	private GameProfile steamcraft$replacement;
	@Unique
	private boolean steamcraft$denied;
	@Unique
	private Long steamcraft$steamId;
	@Unique
	private volatile boolean steamcraft$pending;
	@Unique
	private boolean steamcraft$verifying;
	@Unique
	private volatile int steamcraft$queryId = -1;

	@Override
	public Connection steamcraft$connection() {
		return connection;
	}

	@Override
	public GameProfile steamcraft$authenticatedProfile() {
		return authenticatedProfile;
	}

	@Override
	public String steamcraft$requestedUsername() {
		return requestedUsername;
	}

	@Override
	public void steamcraft$replaceProfile(GameProfile profile) {
		steamcraft$replacement = profile;
		authenticatedProfile = profile;
	}

	@Override
	public void steamcraft$deny() {
		steamcraft$denied = true;
	}

	@Override
	public void steamcraft$setPending(boolean pending) {
		steamcraft$pending = pending;
	}

	@Override
	public void steamcraft$setSteamId(Long steamId) {
		steamcraft$steamId = steamId;
	}

	@Override
	public Long steamcraft$steamId() {
		return steamcraft$steamId;
	}

	@Override
	public void steamcraft$setQueryId(int transactionId) {
		steamcraft$queryId = transactionId;
	}

	@Override
	public int steamcraft$queryId() {
		return steamcraft$queryId;
	}

	@WrapOperation(method = "handleHello", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;usesAuthentication()Z"))
	private boolean steamcraft$skipMojangForSteamGuests(MinecraftServer server, Operation<Boolean> original) {
		SteamCraftServer context = SteamCraftServer.current();
		if (context != null && context.tunnelSteamId(connection) != null) return false;
		return original.call(server);
	}

	@Inject(method = "verifyLoginAndFinishConnectionSetup", at = @At("HEAD"), cancellable = true)
	private void steamcraft$holdUntilDecided(GameProfile profile, CallbackInfo ci) {
		if (!steamcraft$verifying) {
			steamcraft$verifying = true;
			Platform.get().loginVerificationStarting(this);
		}
		if (steamcraft$denied || steamcraft$pending) ci.cancel();
	}

	@ModifyVariable(method = "verifyLoginAndFinishConnectionSetup", at = @At("HEAD"), argsOnly = true)
	private GameProfile steamcraft$useReplacement(GameProfile profile) {
		return steamcraft$replacement != null ? steamcraft$replacement : profile;
	}
}
