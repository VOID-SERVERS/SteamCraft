package dev.steamcraft.core.auth;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.SecureRandom;

public final class AuthProtocol {
	public static final String NAMESPACE = "steamcraft";
	public static final String PATH = "auth";
	public static final int VERSION = 1;
	public static final int MAX_TICKET_SIZE = 4096;
	private static final SecureRandom RANDOM = new SecureRandom();

	public enum Status {
		OK,
		NO_STEAM,
		ERROR
	}

	public static final class Request {
		public final int version;
		public final byte[] nonce;

		public Request(int version, byte[] nonce) {
			this.version = version;
			this.nonce = nonce;
		}
	}

	public static final class Response {
		public final Status status;
		public final long steamId;
		public final byte[] ticket;

		public Response(Status status, long steamId, byte[] ticket) {
			this.status = status;
			this.steamId = steamId;
			this.ticket = ticket;
		}

		public static Response failure(Status status) {
			return new Response(status, 0L, new byte[0]);
		}
	}

	private AuthProtocol() {
	}

	public static byte[] newRequest() {
		byte[] nonce = new byte[16];
		RANDOM.nextBytes(nonce);
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		bytes.write(VERSION);
		bytes.write(0);
		bytes.write(nonce, 0, nonce.length);
		return bytes.toByteArray();
	}

	public static Request decodeRequest(byte[] data) {
		if (data.length < 18) throw new IllegalArgumentException("Auth request too short");
		byte[] nonce = new byte[16];
		System.arraycopy(data, 2, nonce, 0, 16);
		return new Request(data[0] & 0xFF, nonce);
	}

	public static byte[] encodeResponse(Response response) {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (DataOutputStream out = new DataOutputStream(bytes)) {
			out.writeByte(VERSION);
			out.writeByte(response.status.ordinal());
			out.writeLong(response.steamId);
			out.writeShort(response.ticket.length);
			out.write(response.ticket);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		return bytes.toByteArray();
	}

	public static Response decodeResponse(byte[] data) {
		try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
			in.readUnsignedByte();
			int status = in.readUnsignedByte();
			if (status >= Status.values().length) throw new IllegalArgumentException("Unknown auth status " + status);
			long steamId = in.readLong();
			int length = in.readUnsignedShort();
			if (length > MAX_TICKET_SIZE) throw new IllegalArgumentException("Auth ticket too large");
			byte[] ticket = new byte[length];
			in.readFully(ticket);
			return new Response(Status.values()[status], steamId, ticket);
		} catch (IOException e) {
			throw new IllegalArgumentException("Malformed auth response", e);
		}
	}
}
