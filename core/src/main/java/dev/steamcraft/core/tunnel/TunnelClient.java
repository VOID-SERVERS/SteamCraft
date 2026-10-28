package dev.steamcraft.core.tunnel;

import dev.steamcraft.core.Log;
import dev.steamcraft.core.Timeouts;
import dev.steamcraft.core.identity.SteamIds;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class TunnelClient {
	private static final int OPEN_TIMEOUT_SECONDS = 15;
	private static final int MAX_PENDING_BYTES = 64 * 1024;
	private static final int REJECT_REPLY_WAIT_SECONDS = 5;

	private final P2PTransport transport;
	private final long host;
	private final Map<Integer, ClientStream> streams = new ConcurrentHashMap<>();
	private final AtomicInteger nextStream = new AtomicInteger(1);
	private ServerSocket listener;
	private volatile long lastActivity = System.currentTimeMillis();

	TunnelClient(P2PTransport transport, long host) {
		this.transport = transport;
		this.host = host;
	}

	int start() throws IOException {
		listener = new ServerSocket(0, 8, InetAddress.getLoopbackAddress());
		Thread thread = new Thread(this::acceptLoop, "steamcraft-tunnel-accept-" + SteamIds.format(host));
		thread.setDaemon(true);
		thread.start();
		return listener.getLocalPort();
	}

	int port() {
		return listener.getLocalPort();
	}

	boolean isClosed() {
		return listener == null || listener.isClosed();
	}

	boolean isIdle(long idleMillis) {
		return streams.isEmpty() && System.currentTimeMillis() - lastActivity > idleMillis;
	}

	void close() {
		try {
			if (listener != null) listener.close();
		} catch (IOException ignored) {
		}
		for (ClientStream stream : streams.values()) stream.pipe.close();
		transport.closeSession(host);
	}

	private void acceptLoop() {
		while (!listener.isClosed()) {
			Socket socket;
			try {
				socket = listener.accept();
				socket.setTcpNoDelay(true);
			} catch (IOException e) {
				return;
			}
			lastActivity = System.currentTimeMillis();
			int id = nextStream.getAndIncrement();
			ClientStream stream = new ClientStream(id, socket);
			streams.put(id, stream);
			transport.send(host, P2PTransport.CHANNEL_TO_HOST, Frames.open(id));
			stream.pipe.start();
			Timeouts.schedule(() -> stream.reject("The host did not answer. Is the world still open through Steam?"), OPEN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
		}
	}

	void onPacket(byte[] message) {
		Frames.Frame frame = Frames.parse(message);
		if (frame == null) return;
		ClientStream stream = streams.get(frame.stream);
		if (stream == null) return;
		lastActivity = System.currentTimeMillis();
		switch (frame.type) {
			case Frames.OPEN_OK:
				stream.opened();
				break;
			case Frames.REJECT:
				stream.reject(frame.text());
				break;
			case Frames.DATA:
				stream.pipe.write(frame.payload);
				break;
			case Frames.CLOSE:
				streams.remove(frame.stream);
				stream.pipe.finish();
				break;
			default:
		}
	}

	void failAll(String reason) {
		for (ClientStream stream : streams.values()) {
			if (!stream.reject(reason)) stream.pipe.close();
		}
	}

	private final class ClientStream implements TcpPipe.Sink {
		final int id;
		final TcpPipe pipe;
		private boolean open;
		private boolean done;
		private String rejection;
		private ByteArrayOutputStream pending = new ByteArrayOutputStream();

		ClientStream(int id, Socket socket) {
			this.id = id;
			this.pipe = new TcpPipe(socket, "steamcraft-tunnel-client-" + id, this);
		}

		synchronized void opened() {
			if (open || done) return;
			open = true;
			byte[] buffered = pending.toByteArray();
			pending = null;
			for (int offset = 0; offset < buffered.length; offset += Frames.MAX_DATA) {
				int length = Math.min(Frames.MAX_DATA, buffered.length - offset);
				transport.send(host, P2PTransport.CHANNEL_TO_HOST, Frames.data(id, buffered, offset, length));
			}
		}

		synchronized boolean reject(String reason) {
			if (open || done) return false;
			done = true;
			Log.info("Tunnel to " + SteamIds.format(host) + " refused: " + reason);
			rejection = reason;
			if (!replyToRejection()) Timeouts.schedule(this::giveUpOnRejection, REJECT_REPLY_WAIT_SECONDS, TimeUnit.SECONDS);
			return true;
		}

		private boolean replyToRejection() {
			byte[] reply = FakeServerReply.build(pending.toByteArray(), rejection);
			if (reply == null) return false;
			rejection = null;
			pipe.write(reply);
			pipe.finish();
			return true;
		}

		private synchronized void giveUpOnRejection() {
			if (rejection == null) return;
			rejection = null;
			pipe.finish();
		}

		@Override
		public synchronized void onData(byte[] chunk) {
			if (open) {
				transport.send(host, P2PTransport.CHANNEL_TO_HOST, Frames.data(id, chunk, 0, chunk.length));
			} else if (!done || rejection != null) {
				if (pending.size() + chunk.length > MAX_PENDING_BYTES) {
					pipe.close();
					return;
				}
				pending.write(chunk, 0, chunk.length);
				if (rejection != null) replyToRejection();
			}
		}

		@Override
		public void onClosed() {
			lastActivity = System.currentTimeMillis();
			if (streams.remove(id, this) && isOpen()) {
				transport.send(host, P2PTransport.CHANNEL_TO_HOST, Frames.close(id));
			}
		}

		private synchronized boolean isOpen() {
			return open;
		}
	}
}
