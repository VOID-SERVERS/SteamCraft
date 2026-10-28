package dev.steamcraft.common.mixin;

import dev.steamcraft.common.SteamCraftServer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.KickCommand;
import net.minecraft.server.commands.SayCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.Predicate;

@Mixin({KickCommand.class, SayCommand.class})
abstract class AdminCommandsMixin {
	@ModifyArg(method = "register", at = @At(value = "INVOKE",
			target = "Lcom/mojang/brigadier/builder/LiteralArgumentBuilder;requires(Ljava/util/function/Predicate;)Lcom/mojang/brigadier/builder/ArgumentBuilder;"))
	private static Predicate<CommandSourceStack> steamcraft$allowCoopAdmins(Predicate<CommandSourceStack> requirement) {
		return requirement.or(SteamCraftServer::allowsAdminCommandsWithoutCheats);
	}
}
