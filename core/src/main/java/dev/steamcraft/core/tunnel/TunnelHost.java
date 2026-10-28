package dev.steamcraft.core.tunnel;

import dev.steamcraft.core.Log;
import dev.steamcraft.core.identity.SteamIds;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class TunnelHost implements P2PTransport.Listener {
	public interface Gate {
		String rejectReason(long peer);
	}

	static final int MAX_STREAMS_PER_PEER = 8;
	private static final int CONNECT_TIMEOUT_MS = 5000;

	private final P2PTransport transport;
	private final int targetPort;
	private final Gate gate;
	private final PeerRegistry registry;
	private final Map<StreamKey, HostStream> streams = new ConcurrentHashMap<>();
	private final ExecutorService connector = Executors.newCachedThreadPool(r -> {
		Thread thread = new Thread(r, "steamcraft-tunnel-connect");
		thread.setDaemon(true);
		return thread;
	});
	private volatile boolean running;
	private volatile boolean accepting = true;

	public TunnelHost(P2PTransport transport, int targetPort, Gate gate, PeerRegistry registry) {
		this.transport = transport;
		this.targetPort = targetPort;
		this.gate = gate;
		this.registry = registry;
	}

	public int targetPort() {
		return targetPort;
	}

	public boolean isAccepting() {
		return accepting;
	}

	public void setAccepting(boolean accepting) {
		this.accepting = accepting;
	}

	public void start() {
		running = true;
		transport.setSessionFilter(peer -> running && (accepting || streamCount(peer) > 0));
		transport.setListener(P2PTransport.CHANNEL_TO_HOST, this);
		Log.info("Tunnel host forwarding Steam P2P to 127.0.0.1:" + targetPort);
	}

	public void stop() {
		running = false;
		transport.setListener(P2PTransport.CHANNEL_TO_HOST, null);
		transport.setSessionFilter(peer -> false);
		for (HostStream stream : streams.values()) stream.close();
		streams.clear();
		connector.shutdownNow();
	}

	@Override
	public void onPacket(long peer, byte[] message) {
		Frames.Frame frame = Frames.parse(message);
		if (frame == null || !running) return;
		StreamKey key = new StreamKey(peer, frame.stream);
		switch (frame.type) {
			case Frames.OPEN:
				open(key, frame);
				break;
			case Frames.DATA: {
				HostStream stream = streams.get(key);
				if (stream != null) stream.write(frame.payload);
				break;
			}
			case Frames.CLOSE: {
				HostStream stream = streams.remove(key);
				if (stream != null) stream.finish();
				break;
			}
			default:
		}
	}

	@Override
	public void onSessionFailed(long peer, String reason) {
		for (HostStream stream : streams.values()) {
			if (stream.key.peer == peer) stream.close();
		}
	}

	private void open(StreamKey key, Frames.Frame frame) {
		if (streams.containsKey(key)) return;
		String reason = !accepting ? "This world is no longer open through Steam."
				: frame.openVersion() == Frames.PROTOCOL_VERSION ? checkGate(key.peer) : "SteamCraft versions of you and the host do not match.";
		if (reason == null && streamCount(key.peer) >= MAX_STREAMS_PER_PEER) reason = "Too many connections.";
		if (reason != null) {
			Log.info("Refused tunnel from " + SteamIds.format(key.peer) + ": " + reason);
			transport.send(key.peer, P2PTransport.CHANNEL_TO_CLIENT, Frames.reject(key.stream, reason));
			return;
		}
		HostStream stream = new HostStream(key);
		streams.put(key, stream);
		connector.execute(stream::connect);
	}

	private String checkGate(long peer) {
		try {
			return gate.rejectReason(peer);
		} catch (RuntimeException e) {
			Log.warn("Tunnel gate failed", e);
			return "The host could not check your access.";
		}
	}

	private int streamCount(long peer) {
		int count = 0;
		for (StreamKey key : streams.keySet()) {
			if (key.peer == peer) count++;
		}
		return count;
	}

	private final class HostStream implements TcpPipe.Sink {
		final StreamKey key;
		private TcpPipe pipe;
		private int localPort = -1;

		HostStream(StreamKey key) {
			this.key = key;
		}

		void connect() {
			Socket socket = new Socket();
			try {
				socket.setTcpNoDelay(true);
				socket.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), targetPort), CONNECT_TIMEOUT_MS);
			} catch (IOException e) {
				Log.warn("Tunnel could not reach the local server on port " + targetPort, e);
				streams.remove(key, this);
				transport.send(key.peer, P2PTransport.CHANNEL_TO_CLIENT, Frames.reject(key.stream, "The host's world is not accepting connections."));
				return;
			}
			synchronized (this) {
				localPort = socket.getLocalPort();
				registry.register(localPort, key.peer);
				pipe = new TcpPipe(socket, "steamcraft-tunnel-host-" + key.stream, this);
			}
			transport.send(key.peer, P2PTransport.CHANNEL_TO_CLIENT, Frames.openOk(key.stream));
			pipe.start();
			if (!streams.containsKey(key)) pipe.close();
		}

		synchronized void write(byte[] data) {
			if (pipe != null) pipe.write(data);
		}

		synchronized void finish() {
			if (pipe != null) pipe.finish();
		}

		synchronized void close() {
			if (pipe != null) pipe.close();
		}

		@Override
		public void onData(byte[] chunk) {
			transport.send(key.peer, P2PTransport.CHANNEL_TO_CLIENT, Frames.data(key.stream, chunk, 0, chunk.length));
		}

		@Override
		public void onClosed() {
			registry.unregister(localPort);
			if (streams.remove(key, this)) {
				transport.send(key.peer, P2PTransport.CHANNEL_TO_CLIENT, Frames.close(key.stream));
			}
		}
	}

	static final class StreamKey {
		final long peer;
		final int stream;

		StreamKey(long peer, int stream) {
			this.peer = peer;
			this.stream = stream;
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof StreamKey)) return false;
			StreamKey other = (StreamKey) o;
			return peer == other.peer && stream == other.stream;
		}

		@Override
		public int hashCode() {
			return Long.hashCode(peer) * 31 + stream;
		}
	}
}
