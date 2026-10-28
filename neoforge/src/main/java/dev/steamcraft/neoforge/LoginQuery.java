package dev.steamcraft.neoforge;

import dev.steamcraft.common.SteamLogin;
import dev.steamcraft.common.SteamLoginHandler;
import net.minecraft.network.protocol.login.ClientboundCustomQueryPacket;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import net.minecraft.server.MinecraftServer;

import java.util.concurrent.ThreadLocalRandom;

public final class LoginQuery {
	private LoginQuery() {
	}

	static void start(SteamLoginHandler handler) {
		byte[] request = SteamLogin.start(handler);
		if (request == null) return;
		int transactionId = ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);
		handler.steamcraft$setQueryId(transactionId);
		handler.steamcraft$connection().send(new ClientboundCustomQueryPacket(transactionId, new RawQueryPayload(request)));
	}

	public static boolean answered(MinecraftServer server, SteamLoginHandler handler, ServerboundCustomQueryAnswerPacket packet) {
		int expected = handler.steamcraft$queryId();
		if (expected < 0 || packet.transactionId() != expected) return false;
		handler.steamcraft$setQueryId(-1);
		byte[] response = packet.payload() instanceof RawQueryPayload raw ? raw.data() : null;
		SteamLogin.answered(server, handler, response);
		return true;
	}
}
