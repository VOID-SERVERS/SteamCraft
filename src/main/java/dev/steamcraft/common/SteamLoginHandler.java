package dev.steamcraft.common;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.Connection;

public interface SteamLoginHandler {
	Connection steamcraft$connection();

	GameProfile steamcraft$authenticatedProfile();

	String steamcraft$requestedUsername();

	void steamcraft$replaceProfile(GameProfile profile);

	void steamcraft$deny();

	void steamcraft$setPending(boolean pending);

	void steamcraft$setSteamId(Long steamId);

	Long steamcraft$steamId();

	void steamcraft$setQueryId(int transactionId);

	int steamcraft$queryId();
}
