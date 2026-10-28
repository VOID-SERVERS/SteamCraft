package dev.steamcraft.core.tunnel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.LongPredicate;

final class MemoryNetwork {
	private final Map<Long, Endpoint> endpoints = new ConcurrentHashMap<>();

	Endpoint endpoint(long steamId) {
		Endpoint endpoint = new Endpoint(steamId);
		endpoints.put(steamId, endpoint);
		return endpoint;
	}

	final class Endpoint implements P2PTransport {
		final long steamId;
		private final P2PTransport.Listener[] listeners = new P2PTransport.Listener[2];
		private final ExecutorService inbox = Executors.newSingleThreadExecutor(r -> {
			Thread thread = new Thread(r, "memory-p2p");
			thread.setDaemon(true);
			return thread;
		});
		volatile LongPredicate filter = peer -> false;

		Endpoint(long steamId) {
			this.steamId = steamId;
		}

		@Override
		public void setListener(int channel, Listener listener) {
			listeners[channel] = listener;
		}

		@Override
		public void setSessionFilter(LongPredicate filter) {
			this.filter = filter;
		}

		@Override
		public void send(long peer, int channel, byte[] data) {
			Endpoint target = endpoints.get(peer);
			if (target == null) return;
			long from = steamId;
			target.inbox.execute(() -> {
				Listener listener = target.listeners[channel];
				if (listener != null) listener.onPacket(from, data);
			});
		}

		@Override
		public void closeSession(long peer) {
		}
	}
}
