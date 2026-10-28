package dev.steamcraft.probe;

import dev.steamcraft.core.auth.TicketValidator;
import dev.steamcraft.core.steam.SteamClient;
import dev.steamcraft.core.steam.SteamServer;

import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class RevocationCheck {
	public static void main(String[] args) throws Exception {
		SteamClient client = SteamClient.start(Paths.get("natives"), 480, 10);
		SteamServer server = SteamServer.start(Paths.get("natives"), 480, 27099, 10);
		try {
			Thread.sleep(1500);
			CompletableFuture<String> revoked = new CompletableFuture<>();
			server.validator().setRevocationListener((steamId, reason) -> revoked.complete(reason));

			byte[] ticket = client.requestTicket().get();
			TicketValidator.TicketResult login = server.validator().validate(client.localSteamId(), ticket).get(15, TimeUnit.SECONDS);
			System.out.println("login: " + (login.ok ? "OK" : login.failure));
			if (!login.ok) System.exit(1);

			client.cancelTickets();
			long start = System.currentTimeMillis();
			try {
				String reason = revoked.get(120, TimeUnit.SECONDS);
				System.out.println("RESULT REVOKED after " + (System.currentTimeMillis() - start) + " ms: " + reason);
			} catch (TimeoutException e) {
				System.out.println("RESULT NOT REVOKED within 120 s");
			}
		} finally {
			server.shutdown();
			client.shutdown();
		}
		System.exit(0);
	}
}
