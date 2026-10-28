package dev.steamcraft.core.steam;

import com.codedisaster.steamworks.SteamAPI;
import com.codedisaster.steamworks.SteamAuth;
import com.codedisaster.steamworks.SteamAuthTicket;
import com.codedisaster.steamworks.SteamException;
import com.codedisaster.steamworks.SteamFriends;
import com.codedisaster.steamworks.SteamFriendsCallback;
import com.codedisaster.steamworks.SteamID;
import com.codedisaster.steamworks.SteamNetworking;
import com.codedisaster.steamworks.SteamNetworkingCallback;
import com.codedisaster.steamworks.SteamResult;
import com.codedisaster.steamworks.SteamUser;
import com.codedisaster.steamworks.SteamUserCallback;
import com.codedisaster.steamworks.SteamUtils;
import com.codedisaster.steamworks.SteamUtilsCallback;
import dev.steamcraft.core.Log;
import dev.steamcraft.core.Timeouts;
import dev.steamcraft.core.auth.TicketValidator;
import dev.steamcraft.core.tunnel.P2PTransport;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.LongPredicate;

public final class SteamClient implements P2PTransport {
	private static final int MAX_PACKETS_PER_TICK = 512;
	private static final int[] CHANNELS = {CHANNEL_TO_HOST, CHANNEL_TO_CLIENT};

	private final SteamThread thread;
	private SteamUser user;
	private SteamFriends friends;
	private SteamUtils utils;
	private SteamNetworking networking;
	private long localSteamId;
	private AuthSessionValidator validator;

	private final Listener[] listeners = new Listener[CHANNELS.length];
	private volatile LongPredicate sessionFilter = peer -> false;
	private volatile Consumer<String> joinRequestListener = connect -> {
	};
	private final Map<Long, CompletableFuture<Void>> ticketCallbacks = new ConcurrentHashMap<>();
	private final List<SteamAuthTicket> activeTickets = new ArrayList<>();
	private ByteBuffer receiveBuffer = ByteBuffer.allocateDirect(64 * 1024);
	private final ByteBuffer sendBuffer = ByteBuffer.allocateDirect(64 * 1024);
	private final ByteBuffer ticketBuffer = ByteBuffer.allocateDirect(4096);

	private SteamClient() {
		this.thread = new SteamThread("steamcraft-steam");
	}

	public static SteamClient start(Path nativesDir, int appId, int ticketTimeoutSeconds) throws SteamUnavailableException {
		SteamNatives.writeAppId(appId);
		if (!SteamAPI.loadLibraries(new SteamNatives(nativesDir))) {
			throw new SteamUnavailableException("Steam natives are not available for this platform");
		}
		SteamClient client = new SteamClient();
		try {
			client.thread.call(() -> {
				client.init(ticketTimeoutSeconds);
				return null;
			});
		} catch (IllegalStateException e) {
			client.thread.stop(null);
			throw new SteamUnavailableException(e.getCause() != null ? e.getCause().getMessage() : e.getMessage(), e);
		}
		return client;
	}

	private void init(int ticketTimeoutSeconds) throws SteamException {
		SteamAPI.InitResult result = SteamAPI.initEx();
		if (result != SteamAPI.InitResult.OK) throw new SteamException("SteamAPI_Init failed: " + result);
		user = new SteamUser(new UserCallback());
		friends = new SteamFriends(new FriendsCallback());
		utils = new SteamUtils(new SteamUtilsCallback() {
		});
		networking = new SteamNetworking(new NetworkingCallback());
		networking.allowP2PPacketRelay(true);
		localSteamId = SteamID.getNativeHandle(user.getSteamID());
		validator = new AuthSessionValidator(thread, new AuthSessionValidator.Backend() {
			@Override
			public SteamAuth.BeginAuthSessionResult begin(ByteBuffer ticket, long steamId) throws SteamException {
				return user.beginAuthSession(ticket, SteamID.createFromNativeHandle(steamId));
			}

			@Override
			public void end(long steamId) {
				user.endAuthSession(SteamID.createFromNativeHandle(steamId));
			}
		}, ticketTimeoutSeconds, CompletableFuture.completedFuture(null));
		thread.addTicker(SteamAPI::runCallbacks);
		thread.addTicker(this::pollPackets);
		Log.info("Steam ready as " + friends.getPersonaName() + " (" + Long.toUnsignedString(localSteamId) + ")");
	}

	public long localSteamId() {
		return localSteamId;
	}

	public String personaName() {
		return thread.call(() -> friends.getPersonaName());
	}

	public String friendName(long steamId) {
		return thread.call(() -> friends.getFriendPersonaName(SteamID.createFromNativeHandle(steamId)));
	}

	public boolean isFriend(long steamId) {
		return thread.call(() -> friends.getFriendRelationship(SteamID.createFromNativeHandle(steamId)) == SteamFriends.FriendRelationship.Friend);
	}

	public Map<Long, String> friends() {
		return thread.call(() -> {
			Map<Long, String> result = new LinkedHashMap<>();
			int count = friends.getFriendCount(SteamFriends.FriendFlags.Immediate);
			for (int i = 0; i < count; i++) {
				SteamID friend = friends.getFriendByIndex(i, SteamFriends.FriendFlags.Immediate);
				result.put(SteamID.getNativeHandle(friend), friends.getFriendPersonaName(friend));
			}
			return result;
		});
	}

	public List<SteamFriend> friendList() {
		return thread.call(() -> {
			List<SteamFriend> result = new ArrayList<>();
			SteamFriends.FriendGameInfo game = new SteamFriends.FriendGameInfo();
			int count = friends.getFriendCount(SteamFriends.FriendFlags.Immediate);
			for (int i = 0; i < count; i++) {
				SteamID friend = friends.getFriendByIndex(i, SteamFriends.FriendFlags.Immediate);
				result.add(new SteamFriend(SteamID.getNativeHandle(friend), friends.getFriendPersonaName(friend),
						status(friends.getFriendPersonaState(friend)), friends.getFriendGamePlayed(friend, game),
						SteamCraftPresence.decode(key -> friends.getFriendRichPresence(friend, key))));
			}
			return result;
		});
	}

	private static SteamFriend.Status status(SteamFriends.PersonaState state) {
		if (state == null) return SteamFriend.Status.OFFLINE;
		switch (state) {
			case Offline:
			case Invisible:
				return SteamFriend.Status.OFFLINE;
			case Away:
			case Snooze:
				return SteamFriend.Status.AWAY;
			case Busy:
				return SteamFriend.Status.BUSY;
			default:
				return SteamFriend.Status.ONLINE;
		}
	}

	public Avatar smallAvatar(long steamId) {
		return thread.call(() -> {
			int image = friends.getSmallFriendAvatar(SteamID.createFromNativeHandle(steamId));
			if (image <= 0) return null;
			int width = utils.getImageWidth(image);
			int height = utils.getImageHeight(image);
			if (width <= 0 || height <= 0) return null;
			ByteBuffer rgba = ByteBuffer.allocateDirect(width * height * 4);
			if (!utils.getImageRGBA(image, rgba)) return null;
			byte[] pixels = new byte[width * height * 4];
			rgba.get(pixels);
			return new Avatar(width, height, pixels);
		});
	}

	public static final class Avatar {
		public final int width;
		public final int height;
		public final byte[] rgba;

		Avatar(int width, int height, byte[] rgba) {
			this.width = width;
			this.height = height;
			this.rgba = rgba;
		}
	}

	public void setPresence(SteamCraftPresence.Activity activity, String connect) {
		Map<String, String> values = SteamCraftPresence.encode(activity);
		thread.execute(() -> {
			for (Map.Entry<String, String> value : values.entrySet()) friends.setRichPresence(value.getKey(), value.getValue());
			friends.setRichPresence("connect", connect != null ? connect : "");
		});
	}

	public boolean inviteToGame(long steamId, String connect) {
		return thread.call(() -> friends.inviteUserToGame(SteamID.createFromNativeHandle(steamId), connect));
	}

	public TicketValidator validator() {
		return validator;
	}

	public CompletableFuture<byte[]> requestTicket() {
		return thread.submit(() -> {
			ticketBuffer.clear();
			int[] size = new int[1];
			SteamAuthTicket ticket = user.getAuthSessionTicket(ticketBuffer, size);
			if (!ticket.isValid()) throw new SteamException("GetAuthSessionTicket failed");
			activeTickets.add(ticket);
			byte[] bytes = new byte[size[0]];
			ticketBuffer.get(bytes);
			CompletableFuture<Void> confirmed = new CompletableFuture<>();
			ticketCallbacks.put(SteamAuthTicket.getNativeHandle(ticket), confirmed);
			Timeouts.completeOnTimeout(confirmed, 3, TimeUnit.SECONDS, () -> null);
			return confirmed.thenApply(ignored -> bytes);
		}).thenCompose(future -> future);
	}

	public void cancelTickets() {
		thread.execute(() -> {
			for (SteamAuthTicket ticket : activeTickets) user.cancelAuthTicket(ticket);
			activeTickets.clear();
		});
	}

	public void setRichPresence(String key, String value) {
		thread.execute(() -> friends.setRichPresence(key, value));
	}

	public void clearRichPresence() {
		thread.execute(() -> friends.clearRichPresence());
	}

	public void setJoinRequestListener(Consumer<String> listener) {
		joinRequestListener = listener;
	}

	@Override
	public void setListener(int channel, Listener listener) {
		listeners[channel] = listener;
	}

	@Override
	public void setSessionFilter(LongPredicate filter) {
		sessionFilter = filter;
	}

	@Override
	public void send(long peer, int channel, byte[] data) {
		thread.execute(() -> {
			sendBuffer.clear();
			sendBuffer.put(data).flip();
			try {
				if (!networking.sendP2PPacket(SteamID.createFromNativeHandle(peer), sendBuffer, SteamNetworking.P2PSend.Reliable, channel)) {
					Log.warn("SendP2PPacket to " + Long.toUnsignedString(peer) + " failed");
				}
			} catch (SteamException e) {
				Log.warn("SendP2PPacket failed", e);
			}
		});
	}

	@Override
	public void closeSession(long peer) {
		thread.execute(() -> networking.closeP2PSessionWithUser(SteamID.createFromNativeHandle(peer)));
	}

	private void pollPackets() {
		int[] size = new int[1];
		SteamID remote = new SteamID();
		for (int channel : CHANNELS) {
			for (int i = 0; i < MAX_PACKETS_PER_TICK && networking.isP2PPacketAvailable(channel, size); i++) {
				if (receiveBuffer.capacity() < size[0]) receiveBuffer = ByteBuffer.allocateDirect(size[0]);
				receiveBuffer.clear();
				int read;
				try {
					read = networking.readP2PPacket(remote, receiveBuffer, channel);
				} catch (SteamException e) {
					Log.warn("ReadP2PPacket failed", e);
					break;
				}
				byte[] data = new byte[read];
				receiveBuffer.get(data, 0, read);
				Listener listener = listeners[channel];
				if (listener != null) listener.onPacket(SteamID.getNativeHandle(remote), data);
			}
		}
	}

	public void shutdown() {
		thread.stop(() -> {
			for (SteamAuthTicket ticket : activeTickets) user.cancelAuthTicket(ticket);
			friends.clearRichPresence();
			SteamAPI.shutdown();
		});
	}

	private final class UserCallback implements SteamUserCallback {
		@Override
		public void onAuthSessionTicket(SteamAuthTicket ticket, SteamResult result) {
			CompletableFuture<Void> confirmed = ticketCallbacks.remove(SteamAuthTicket.getNativeHandle(ticket));
			if (confirmed != null) confirmed.complete(null);
		}

		@Override
		public void onValidateAuthTicket(SteamID steamId, SteamAuth.AuthSessionResponse response, SteamID ownerSteamId) {
			validator.onResponse(SteamID.getNativeHandle(steamId), response);
		}
	}

	private final class FriendsCallback implements SteamFriendsCallback {
		@Override
		public void onGameRichPresenceJoinRequested(SteamID friend, String connect) {
			joinRequestListener.accept(connect);
		}
	}

	private final class NetworkingCallback implements SteamNetworkingCallback {
		@Override
		public void onP2PSessionRequest(SteamID remote) {
			long peer = SteamID.getNativeHandle(remote);
			if (sessionFilter.test(peer)) {
				networking.acceptP2PSessionWithUser(remote);
			} else {
				Log.debug("Ignoring P2P session request from " + Long.toUnsignedString(peer));
			}
		}

		@Override
		public void onP2PSessionConnectFail(SteamID remote, SteamNetworking.P2PSessionError error) {
			long peer = SteamID.getNativeHandle(remote);
			for (Listener listener : listeners) {
				if (listener != null) listener.onSessionFailed(peer, error.name());
			}
		}
	}
}
