package dev.steamcraft.core.tunnel;

import java.io.ByteArrayOutputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

final class FakeServerReply {
	private static final int MAX_HOST_LENGTH = 255 * 4;

	private FakeServerReply() {
	}

	static byte[] build(byte[] clientBytes, String reason) {
		Handshake handshake = Handshake.parse(clientBytes);
		if (handshake == null) return null;
		String text = "{\"text\":" + quote(reason) + ",\"color\":\"red\"}";
		switch (handshake.nextState) {
			case 1:
				return packet(0x00, "{\"version\":{\"name\":\"SteamCraft\",\"protocol\":" + handshake.protocol
						+ "},\"players\":{\"max\":0,\"online\":0},\"description\":" + text + "}");
			case 2:
			case 3:
				return packet(0x00, text);
			default:
				return null;
		}
	}

	static final class Handshake {
		final int protocol;
		final int nextState;

		private Handshake(int protocol, int nextState) {
			this.protocol = protocol;
			this.nextState = nextState;
		}

		static Handshake parse(byte[] bytes) {
			try {
				ByteBuffer in = ByteBuffer.wrap(bytes);
				int length = readVarInt(in);
				if (length <= 0 || length > in.remaining()) return null;
				ByteBuffer packet = (ByteBuffer) in.slice().limit(length);
				if (readVarInt(packet) != 0x00) return null;
				int protocol = readVarInt(packet);
				int hostLength = readVarInt(packet);
				if (hostLength < 0 || hostLength > MAX_HOST_LENGTH) return null;
				packet.position(packet.position() + hostLength + 2);
				return new Handshake(protocol, readVarInt(packet));
			} catch (BufferUnderflowException | IllegalArgumentException e) {
				return null;
			}
		}
	}

	private static byte[] packet(int id, String string) {
		ByteArrayOutputStream body = new ByteArrayOutputStream();
		writeVarInt(body, id);
		byte[] utf8 = string.getBytes(StandardCharsets.UTF_8);
		writeVarInt(body, utf8.length);
		body.write(utf8, 0, utf8.length);
		ByteArrayOutputStream framed = new ByteArrayOutputStream();
		writeVarInt(framed, body.size());
		framed.write(body.toByteArray(), 0, body.size());
		return framed.toByteArray();
	}

	static int readVarInt(ByteBuffer in) {
		int value = 0;
		for (int shift = 0; shift < 35; shift += 7) {
			byte b = in.get();
			value |= (b & 0x7F) << shift;
			if ((b & 0x80) == 0) return value;
		}
		throw new IllegalArgumentException("VarInt too big");
	}

	static void writeVarInt(ByteArrayOutputStream out, int value) {
		while ((value & ~0x7F) != 0) {
			out.write((value & 0x7F) | 0x80);
			value >>>= 7;
		}
		out.write(value);
	}

	static String quote(String text) {
		StringBuilder out = new StringBuilder(text.length() + 2).append('"');
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c == '"' || c == '\\') {
				out.append('\\').append(c);
			} else if (c < 0x20) {
				out.append(String.format("\\u%04x", (int) c));
			} else {
				out.append(c);
			}
		}
		return out.append('"').toString();
	}
}
