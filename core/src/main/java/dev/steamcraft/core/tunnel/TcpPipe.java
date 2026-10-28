package dev.steamcraft.core.tunnel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

final class TcpPipe {
	interface Sink {
		void onData(byte[] chunk);

		void onClosed();
	}

	private static final byte[] END = new byte[0];

	private final Socket socket;
	private final String name;
	private final Sink sink;
	private final BlockingQueue<byte[]> outgoing = new LinkedBlockingQueue<>();
	private final AtomicBoolean closed = new AtomicBoolean();

	TcpPipe(Socket socket, String name, Sink sink) {
		this.socket = socket;
		this.name = name;
		this.sink = sink;
	}

	void start() {
		thread(this::readLoop, name + "-read").start();
		thread(this::writeLoop, name + "-write").start();
	}

	void write(byte[] data) {
		if (!closed.get() && data.length > 0) outgoing.add(data);
	}

	void finish() {
		outgoing.add(END);
	}

	void close() {
		if (!closed.compareAndSet(false, true)) return;
		outgoing.add(END);
		try {
			socket.close();
		} catch (IOException ignored) {
		}
		sink.onClosed();
	}

	private void readLoop() {
		byte[] buffer = new byte[Frames.MAX_DATA];
		try (InputStream in = socket.getInputStream()) {
			int read;
			while ((read = in.read(buffer)) != -1) {
				if (read > 0) sink.onData(Arrays.copyOf(buffer, read));
			}
		} catch (IOException ignored) {
		} finally {
			close();
		}
	}

	private void writeLoop() {
		try {
			OutputStream out = socket.getOutputStream();
			while (true) {
				byte[] data = outgoing.take();
				if (data == END) break;
				out.write(data);
				if (outgoing.isEmpty()) out.flush();
			}
			out.flush();
		} catch (IOException ignored) {
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} finally {
			close();
		}
	}

	private static Thread thread(Runnable task, String name) {
		Thread thread = new Thread(task, name);
		thread.setDaemon(true);
		return thread;
	}
}
