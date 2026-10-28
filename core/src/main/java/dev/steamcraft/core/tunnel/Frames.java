package dev.steamcraft.core.tunnel;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

final class Frames {
	static final byte OPEN = 1;
	static final byte OPEN_OK = 2;
	static final byte REJECT = 3;
	static final byte DATA = 4;
	static final byte CLOSE = 5;

	static final int PROTOCOL_VERSION = 1;
	static final int HEADER_SIZE = 5;
	static final int MAX_DATA = 32 * 1024;

	static final class Frame {
		final byte type;
		final int stream;
		final byte[] payload;

		Frame(byte type, int stream, byte[] payload) {
			this.type = type;
			this.stream = stream;
			this.payload = payload;
		}

		int openVersion() {
			return payload.length >= 4 ? ByteBuffer.wrap(payload).getInt() : -1;
		}

		String text() {
			return new String(payload, StandardCharsets.UTF_8);
		}
	}

	private Frames() {
	}

	static byte[] open(int stream) {
		return frame(OPEN, stream, ByteBuffer.allocate(4).putInt(PROTOCOL_VERSION).array(), 0, 4);
	}

	static byte[] openOk(int stream) {
		return frame(OPEN_OK, stream, null, 0, 0);
	}

	static byte[] reject(int stream, String reason) {
		byte[] text = reason.getBytes(StandardCharsets.UTF_8);
		return frame(REJECT, stream, text, 0, Math.min(text.length, 1024));
	}

	static byte[] data(int stream, byte[] data, int offset, int length) {
		if (length > MAX_DATA) throw new IllegalArgumentException("Frame too large: " + length);
		return frame(DATA, stream, data, offset, length);
	}

	static byte[] close(int stream) {
		return frame(CLOSE, stream, null, 0, 0);
	}

	private static byte[] frame(byte type, int stream, byte[] payload, int offset, int length) {
		ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE + length);
		buffer.put(type).putInt(stream);
		if (length > 0) buffer.put(payload, offset, length);
		return buffer.array();
	}

	static Frame parse(byte[] message) {
		if (message.length < HEADER_SIZE || message.length > HEADER_SIZE + MAX_DATA) return null;
		ByteBuffer buffer = ByteBuffer.wrap(message);
		byte type = buffer.get();
		if (type < OPEN || type > CLOSE) return null;
		int stream = buffer.getInt();
		byte[] payload = new byte[buffer.remaining()];
		buffer.get(payload);
		return new Frame(type, stream, payload);
	}
}
