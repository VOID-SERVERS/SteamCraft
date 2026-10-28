package dev.steamcraft.core.tunnel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class TunnelTest {
	static final long HOST = 76561198000000001L;
	static final long GUEST = 76561198000000002L;

	MemoryNetwork network;
	EchoServer server;
	PeerRegistry registry;
	TunnelHost host;
	TunnelClients clients;
	volatile String rejectReason;

	@BeforeEach
	void setUp() throws IOException {
		network = new MemoryNetwork();
		server = new EchoServer();
		registry = new PeerRegistry();
		host = new TunnelHost(network.endpoint(HOST), server.port(), peer -> rejectReason, registry);
		host.start();
		clients = new TunnelClients(network.endpoint(GUEST));
	}

	@AfterEach
	void tearDown() throws IOException {
		clients.closeAll();
		host.stop();
		server.close();
	}

	@Test
	void bytesFlowBothWaysAndServerSeesTheSteamId() throws Exception {
		try (Socket socket = new Socket(InetAddress.getLoopbackAddress(), clients.portFor(HOST))) {
			socket.setSoTimeout(5000);
			byte[] payload = new byte[200_000];
			new Random(1).nextBytes(payload);
			socket.getOutputStream().write(payload);
			byte[] echoed = new byte[payload.length];
			new DataInputStream(socket.getInputStream()).readFully(echoed);
			assertArrayEquals(payload, echoed);

			Socket serverSide = server.accepted.poll(5, TimeUnit.SECONDS);
			assertNotNull(serverSide);
			assertEquals(GUEST, registry.lookup(serverSide.getRemoteSocketAddress()));
		}
	}

	@Test
	void notAcceptingKeepsOpenStreamsButRefusesNewOnes() throws Exception {
		try (Socket open = new Socket(InetAddress.getLoopbackAddress(), clients.portFor(HOST))) {
			open.setSoTimeout(5000);
			assertEcho(open, "before");
			assertNotNull(server.accepted.poll(5, TimeUnit.SECONDS));

			host.setAccepting(false);
			try (Socket late = new Socket(InetAddress.getLoopbackAddress(), clients.portFor(HOST))) {
				late.setSoTimeout(5000);
				late.getOutputStream().write(handshake(767, 2));
				String reply = new String(readAll(late.getInputStream()), StandardCharsets.UTF_8);
				assertTrue(reply.contains("no longer open through Steam"), reply);
			}
			assertNull(server.accepted.poll(200, TimeUnit.MILLISECONDS), "the late connection never reached the server");
			assertEcho(open, "after");

			host.setAccepting(true);
			try (Socket again = new Socket(InetAddress.getLoopbackAddress(), clients.portFor(HOST))) {
				again.setSoTimeout(5000);
				assertEcho(again, "reopened");
			}
		}
	}

	private static void assertEcho(Socket socket, String text) throws IOException {
		byte[] payload = text.getBytes(StandardCharsets.UTF_8);
		socket.getOutputStream().write(payload);
		byte[] echoed = new byte[payload.length];
		new DataInputStream(socket.getInputStream()).readFully(echoed);
		assertArrayEquals(payload, echoed);
	}

	@Test
	void rejectedLoginGetsAReadableDisconnect() throws Exception {
		rejectReason = "Friends only";
		try (Socket socket = new Socket(InetAddress.getLoopbackAddress(), clients.portFor(HOST))) {
			socket.setSoTimeout(5000);
			socket.getOutputStream().write(handshake(767, 2));
			byte[] reply = readAll(socket.getInputStream());
			ByteBuffer in = ByteBuffer.wrap(reply);
			int length = FakeServerReply.readVarInt(in);
			assertEquals(in.remaining(), length);
			assertEquals(0x00, FakeServerReply.readVarInt(in), "login disconnect packet id");
			int textLength = FakeServerReply.readVarInt(in);
			String json = new String(reply, in.position(), textLength, StandardCharsets.UTF_8);
			assertEquals("{\"text\":\"Friends only\",\"color\":\"red\"}", json);
		}
		assertNull(server.accepted.poll(200, TimeUnit.MILLISECONDS), "the server never saw the connection");
	}

	@Test
	void rejectionThatBeatsTheHandshakeStillShowsTheReason() throws Exception {
		rejectReason = "Friends only";
		try (Socket socket = new Socket(InetAddress.getLoopbackAddress(), clients.portFor(HOST))) {
			socket.setSoTimeout(5000);
			Thread.sleep(300);
			socket.getOutputStream().write(handshake(767, 2));
			String reply = new String(readAll(socket.getInputStream()), StandardCharsets.UTF_8);
			assertTrue(reply.contains("Friends only"), reply);
		}
	}

	@Test
	void rejectedPingShowsReasonAsMotd() throws Exception {
		rejectReason = "Not \"you\"";
		try (Socket socket = new Socket(InetAddress.getLoopbackAddress(), clients.portFor(HOST))) {
			socket.setSoTimeout(5000);
			socket.getOutputStream().write(handshake(767, 1));
			String reply = new String(readAll(socket.getInputStream()), StandardCharsets.UTF_8);
			assertTrue(reply.contains("\"protocol\":767"));
			assertTrue(reply.contains("Not \\\"you\\\""));
		}
	}

	static byte[] handshake(int protocol, int nextState) {
		ByteArrayOutputStream body = new ByteArrayOutputStream();
		FakeServerReply.writeVarInt(body, 0x00);
		FakeServerReply.writeVarInt(body, protocol);
		byte[] host = "76561198000000001.steam".getBytes(StandardCharsets.UTF_8);
		FakeServerReply.writeVarInt(body, host.length);
		body.write(host, 0, host.length);
		body.write(0x63);
		body.write(0xDD);
		FakeServerReply.writeVarInt(body, nextState);
		ByteArrayOutputStream framed = new ByteArrayOutputStream();
		FakeServerReply.writeVarInt(framed, body.size());
		framed.write(body.toByteArray(), 0, body.size());
		framed.write(new byte[]{3, 0, 1, 'A'}, 0, 4);
		return framed.toByteArray();
	}

	static byte[] readAll(InputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buffer = new byte[4096];
		int read;
		while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
		return out.toByteArray();
	}

	static final class EchoServer {
		final ServerSocket socket = new ServerSocket(0, 8, InetAddress.getLoopbackAddress());
		final BlockingQueue<Socket> accepted = new ArrayBlockingQueue<>(8);

		EchoServer() throws IOException {
			Thread thread = new Thread(() -> {
				while (!socket.isClosed()) {
					try {
						Socket client = socket.accept();
						accepted.add(client);
						Thread echo = new Thread(() -> echo(client));
						echo.setDaemon(true);
						echo.start();
					} catch (IOException e) {
						return;
					}
				}
			});
			thread.setDaemon(true);
			thread.start();
		}

		int port() {
			return socket.getLocalPort();
		}

		private static void echo(Socket client) {
			try (InputStream in = client.getInputStream(); OutputStream out = client.getOutputStream()) {
				byte[] buffer = new byte[8192];
				int read;
				while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
			} catch (IOException ignored) {
			}
		}

		void close() throws IOException {
			socket.close();
		}
	}
}
