package dev.steamcraft.core.tunnel;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TunnelClients implements P2PTransport.Listener {
	private static final long IDLE_MILLIS = 2 * 60 * 1000;

	private final P2PTransport transport;
	private final Map<Long, TunnelClient> clients = new ConcurrentHashMap<>();

	public TunnelClients(P2PTransport transport) {
		this.transport = transport;
		transport.setListener(P2PTransport.CHANNEL_TO_CLIENT, this);
	}

	public synchronized int portFor(long host) throws IOException {
		closeIdle(host);
		TunnelClient client = clients.get(host);
		if (client == null || client.isClosed()) {
			client = new TunnelClient(transport, host);
			client.start();
			clients.put(host, client);
		}
		return client.port();
	}

	private void closeIdle(long keep) {
		for (Iterator<Map.Entry<Long, TunnelClient>> it = clients.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<Long, TunnelClient> entry = it.next();
			if (entry.getKey() != keep && entry.getValue().isIdle(IDLE_MILLIS)) {
				entry.getValue().close();
				it.remove();
			}
		}
	}

	public synchronized void closeAll() {
		for (TunnelClient client : clients.values()) client.close();
		clients.clear();
	}

	@Override
	public void onPacket(long peer, byte[] data) {
		TunnelClient client = clients.get(peer);
		if (client != null) client.onPacket(data);
	}

	@Override
	public void onSessionFailed(long peer, String reason) {
		TunnelClient client = clients.get(peer);
		if (client != null) client.failAll("Could not reach the host through Steam (" + reason + ").");
	}
}
