package dev.steamcraft.core.auth;

import java.util.concurrent.CompletableFuture;

public interface TicketValidator {
	CompletableFuture<TicketResult> validate(long steamId, byte[] ticket);

	void endSession(long steamId);

	void setRevocationListener(RevocationListener listener);

	interface RevocationListener {
		void onRevoked(long steamId, String reason);
	}

	final class TicketResult {
		public final boolean ok;
		public final String failure;

		private TicketResult(boolean ok, String failure) {
			this.ok = ok;
			this.failure = failure;
		}

		public static TicketResult success() {
			return new TicketResult(true, null);
		}

		public static TicketResult fail(String failure) {
			return new TicketResult(false, failure);
		}
	}
}
