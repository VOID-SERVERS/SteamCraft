package dev.steamcraft.core.tunnel;

import java.util.function.LongPredicate;

public interface P2PTransport {
	int CHANNEL_TO_HOST = 0;
	int CHANNEL_TO_CLIENT = 1;

	interface Listener {
		void onPacket(long peer, byte[] data);

		default void onSessionFailed(long peer, String reason) {
		}
	}

	void setListener(int channel, Listener listener);

	void setSessionFilter(LongPredicate filter);

	void send(long peer, int channel, byte[] data);

	void closeSession(long peer);
}
