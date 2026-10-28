package dev.steamcraft.core.tunnel;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PeerRegistry {
	private final Map<Integer, Long> byLocalPort = new ConcurrentHashMap<>();

	void register(int localPort, long steamId) {
		byLocalPort.put(localPort, steamId);
	}

	void unregister(int localPort) {
		byLocalPort.remove(localPort);
	}

	public Long lookup(SocketAddress remoteAddress) {
		if (!(remoteAddress instanceof InetSocketAddress)) return null;
		InetSocketAddress address = (InetSocketAddress) remoteAddress;
		if (address.getAddress() == null || !address.getAddress().isLoopbackAddress()) return null;
		return byLocalPort.get(address.getPort());
	}
}
