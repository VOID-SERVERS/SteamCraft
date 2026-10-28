package dev.steamcraft.core.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthProtocolTest {
	@Test
	void responseRoundTrip() {
		AuthProtocol.Response response = new AuthProtocol.Response(AuthProtocol.Status.OK, 76561198000000001L, new byte[]{5, 6, 7});
		AuthProtocol.Response decoded = AuthProtocol.decodeResponse(AuthProtocol.encodeResponse(response));
		assertEquals(AuthProtocol.Status.OK, decoded.status);
		assertEquals(76561198000000001L, decoded.steamId);
		assertArrayEquals(new byte[]{5, 6, 7}, decoded.ticket);
	}

	@Test
	void requestHasVersionAndNonce() {
		AuthProtocol.Request request = AuthProtocol.decodeRequest(AuthProtocol.newRequest());
		assertEquals(AuthProtocol.VERSION, request.version);
		assertEquals(16, request.nonce.length);
	}

	@Test
	void rejectsGarbage() {
		assertThrows(IllegalArgumentException.class, () -> AuthProtocol.decodeResponse(new byte[]{1}));
		assertThrows(IllegalArgumentException.class, () -> AuthProtocol.decodeResponse(new byte[]{1, 99, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}));
		assertThrows(IllegalArgumentException.class, () -> AuthProtocol.decodeRequest(new byte[3]));
	}
}
