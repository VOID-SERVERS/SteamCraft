package dev.steamcraft.probe;

import dev.steamcraft.core.auth.AuthProtocol;
import dev.steamcraft.core.identity.PlayerUuids;
import dev.steamcraft.core.identity.SteamIds;
import dev.steamcraft.core.steam.SteamClient;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Random;
import java.util.UUID;
import java.util.zip.DataFormatException;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;

public final class LoginProbe {
	private static final int PROTOCOL = 767;
	private int threshold = -1;
	private Long claimedSteamId;
	private boolean forgeTicket;

	public static void main(String[] args) throws Exception {
		if (args.length != 4) {
			System.err.println("usage: LoginProbe <host> <port> <name> modded|vanilla|spoof:<SteamID64>|forged:<SteamID64>");
			System.exit(2);
		}
		LoginProbe probe = new LoginProbe();
		String mode = args[3];
		if (mode.startsWith("spoof:") || mode.startsWith("forged:")) {
			probe.claimedSteamId = SteamIds.parse(mode.substring(mode.indexOf(':') + 1));
			probe.forgeTicket = mode.startsWith("forged:");
		}
		probe.run(args[0], Integer.parseInt(args[1]), args[2], !mode.equals("vanilla"));
	}

	private void run(String host, int port, String name, boolean modded) throws Exception {
		SteamClient steam = null;
		if (modded) {
			steam = SteamClient.start(Paths.get("natives"), 480, 10);
			Thread.sleep(1500);
		}
		try (Socket socket = new Socket(host, port)) {
			socket.setSoTimeout(20000);
			DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
			OutputStream out = socket.getOutputStream();

			ByteArrayOutputStream handshake = new ByteArrayOutputStream();
			varInt(handshake, 0x00);
			varInt(handshake, PROTOCOL);
			string(handshake, host);
			handshake.write(port >> 8);
			handshake.write(port & 0xFF);
			varInt(handshake, 2);
			send(out, handshake.toByteArray());

			ByteArrayOutputStream start = new ByteArrayOutputStream();
			varInt(start, 0x00);
			string(start, name);
			UUID uuid = PlayerUuids.offline(name);
			DataOutputStream data = new DataOutputStream(start);
			data.writeLong(uuid.getMostSignificantBits());
			data.writeLong(uuid.getLeastSignificantBits());
			send(out, start.toByteArray());

			while (true) {
				DataInputStream packet = new DataInputStream(new ByteArrayInputStream(read(in)));
				int id = readVarInt(packet);
				switch (id) {
					case 0x00:
						System.out.println("RESULT DISCONNECT " + readString(packet));
						return;
					case 0x01:
						System.out.println("RESULT ENCRYPTION_REQUEST (online-mode server; the probe cannot continue)");
						return;
					case 0x02: {
						UUID got = new UUID(packet.readLong(), packet.readLong());
						String kind = steam != null && got.equals(PlayerUuids.fromSteam(steam.localSteamId())) ? "steam"
								: got.equals(PlayerUuids.offline(name)) ? "offline" : "other";
						System.out.println("RESULT SUCCESS uuid=" + got + " (" + kind + ") name=" + readString(packet));
						return;
					}
					case 0x03:
						threshold = readVarInt(packet);
						break;
					case 0x04:
						answerQuery(out, packet, steam);
						break;
					default:
						System.out.println("ignoring login packet 0x" + Integer.toHexString(id));
				}
			}
		} finally {
			if (steam != null) steam.shutdown();
		}
	}

	private void answerQuery(OutputStream out, DataInputStream packet, SteamClient steam) throws Exception {
		int message = readVarInt(packet);
		String channel = readString(packet);
		ByteArrayOutputStream response = new ByteArrayOutputStream();
		varInt(response, 0x02);
		varInt(response, message);
		if (steam != null && channel.equals(AuthProtocol.NAMESPACE + ":" + AuthProtocol.PATH)) {
			byte[] ticket = steam.requestTicket().get();
			if (forgeTicket) new Random().nextBytes(ticket);
			long steamId = claimedSteamId != null ? claimedSteamId : steam.localSteamId();
			response.write(1);
			response.write(AuthProtocol.encodeResponse(new AuthProtocol.Response(AuthProtocol.Status.OK, steamId, ticket)));
		} else {
			response.write(0);
		}
		send(out, response.toByteArray());
	}

	private void send(OutputStream out, byte[] packet) throws IOException {
		ByteArrayOutputStream body = new ByteArrayOutputStream();
		if (threshold >= 0 && packet.length >= threshold) {
			varInt(body, packet.length);
			DeflaterOutputStream deflater = new DeflaterOutputStream(body);
			deflater.write(packet);
			deflater.finish();
		} else {
			if (threshold >= 0) varInt(body, 0);
			body.write(packet);
		}
		ByteArrayOutputStream framed = new ByteArrayOutputStream();
		varInt(framed, body.size());
		body.writeTo(framed);
		out.write(framed.toByteArray());
		out.flush();
	}

	private byte[] read(DataInputStream in) throws IOException {
		byte[] frame = new byte[readVarInt(in)];
		in.readFully(frame);
		if (threshold < 0) return frame;
		DataInputStream framed = new DataInputStream(new ByteArrayInputStream(frame));
		int dataLength = readVarInt(framed);
		byte[] rest = framed.readAllBytes();
		if (dataLength == 0) return rest;
		Inflater inflater = new Inflater();
		inflater.setInput(rest);
		byte[] data = new byte[dataLength];
		try {
			inflater.inflate(data);
		} catch (DataFormatException e) {
			throw new IOException(e);
		}
		return data;
	}

	private static void varInt(OutputStream out, int value) throws IOException {
		while ((value & ~0x7F) != 0) {
			out.write((value & 0x7F) | 0x80);
			value >>>= 7;
		}
		out.write(value);
	}

	private static int readVarInt(DataInputStream in) throws IOException {
		int value = 0;
		for (int shift = 0; shift < 35; shift += 7) {
			int b = in.readUnsignedByte();
			value |= (b & 0x7F) << shift;
			if ((b & 0x80) == 0) return value;
		}
		throw new IOException("VarInt too big");
	}

	private static void string(OutputStream out, String value) throws IOException {
		byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
		varInt(out, bytes.length);
		out.write(bytes);
	}

	private static String readString(DataInputStream in) throws IOException {
		byte[] bytes = new byte[readVarInt(in)];
		in.readFully(bytes);
		return new String(bytes, StandardCharsets.UTF_8);
	}
}
