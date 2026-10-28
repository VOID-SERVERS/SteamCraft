package dev.steamcraft.common;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
//? if >=1.21.9
/*import net.minecraft.server.players.NameAndId;*/

import java.util.UUID;

public final class Profiles {
	private Profiles() {
	}

	public static UUID id(GameProfile profile) {
		//? if >=1.21.9 {
		/*return profile.id();
		*///?} else {
		return profile.getId();
		//?}
	}

	public static String name(GameProfile profile) {
		//? if >=1.21.9 {
		/*return profile.name();
		*///?} else {
		return profile.getName();
		//?}
	}

	public static boolean isOwner(MinecraftServer server, GameProfile profile) {
		//? if >=1.21.9 {
		/*return server.isSingleplayerOwner(new NameAndId(profile));
		*///?} else {
		return server.isSingleplayerOwner(profile);
		//?}
	}
}
