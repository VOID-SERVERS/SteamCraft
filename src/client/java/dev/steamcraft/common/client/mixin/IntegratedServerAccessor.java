package dev.steamcraft.common.client.mixin;

import net.minecraft.client.server.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
//? if <26.2 {
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.gen.Accessor;
//?}

@Mixin(IntegratedServer.class)
public interface IntegratedServerAccessor {
	//? if <26.2 {
	@Accessor("publishedGameType")
	void steamcraft$setPublishedGameType(GameType mode);
	//?}
}
