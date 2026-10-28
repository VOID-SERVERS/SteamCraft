package dev.steamcraft.common.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.steamcraft.common.SteamCraftServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.io.File;

@Mixin(PlayerList.class)
abstract class PlayerListMixin {
	//? if >=1.21.9 {
	/*@ModifyArg(method = "<init>", index = 0, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/server/players/UserWhiteList;<init>(Ljava/io/File;Lnet/minecraft/server/notifications/NotificationService;)V"))
	*///?} else {
	@ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/UserWhiteList;<init>(Ljava/io/File;)V"))
	//?}
	private File steamcraft$whitelistFile(File file, @Local(argsOnly = true) MinecraftServer server) {
		return steamcraft$listFile(server, file);
	}

	//? if >=1.21.9 {
	/*@ModifyArg(method = "<init>", index = 0, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/server/players/ServerOpList;<init>(Ljava/io/File;Lnet/minecraft/server/notifications/NotificationService;)V"))
	*///?} else {
	@ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/ServerOpList;<init>(Ljava/io/File;)V"))
	//?}
	private File steamcraft$opsFile(File file, @Local(argsOnly = true) MinecraftServer server) {
		return steamcraft$listFile(server, file);
	}

	//? if >=1.21.9 {
	/*@ModifyArg(method = "<init>", index = 0, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/server/players/UserBanList;<init>(Ljava/io/File;Lnet/minecraft/server/notifications/NotificationService;)V"))
	*///?} else {
	@ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/UserBanList;<init>(Ljava/io/File;)V"))
	//?}
	private File steamcraft$bansFile(File file, @Local(argsOnly = true) MinecraftServer server) {
		return steamcraft$listFile(server, file);
	}

	private static File steamcraft$listFile(MinecraftServer server, File file) {
		return SteamCraftServer.isSteamMode(server) ? SteamCraftServer.steamListFile(server, file) : file;
	}
}
