package dev.steamcraft.common.client;

import dev.steamcraft.core.Log;
import dev.steamcraft.core.identity.SteamIds;
import dev.steamcraft.core.steam.SteamClient;
import dev.steamcraft.core.tunnel.TunnelClients;
import dev.steamcraft.common.SteamRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
//? if <1.21.6
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.client.multiplayer.ServerData;
//? if >=1.21.6
/*import net.minecraft.client.multiplayer.ClientLevel;*/
import net.minecraft.client.multiplayer.resolver.ResolvedServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Optional;

public final class SteamJoin {
	private SteamJoin() {
	}

	public static Optional<ResolvedServerAddress> resolve(long host) {
		SteamClient steam = SteamRuntime.client();
		TunnelClients tunnels = SteamRuntime.tunnels();
		if (steam == null || tunnels == null) {
			Log.warn("Cannot join " + SteamIds.toAddress(host) + ": Steam is not running");
			return Optional.empty();
		}
		if (host == steam.localSteamId()) {
			Log.warn("Cannot join your own Steam address; use the singleplayer world directly");
			return Optional.empty();
		}
		try {
			int port = tunnels.portFor(host);
			return Optional.of(ResolvedServerAddress.from(new InetSocketAddress(InetAddress.getLoopbackAddress(), port)));
		} catch (IOException e) {
			Log.warn("Could not open a Steam tunnel to " + SteamIds.format(host), e);
			return Optional.empty();
		}
	}

	static void fromSteamFriendsList(String connect) {
		Minecraft minecraft = Minecraft.getInstance();
		minecraft.execute(() -> {
			Long host = SteamIds.fromAddressHost(connect.trim());
			if (host == null) {
				Log.warn("Ignoring Steam join request with unknown connect string: " + connect);
				return;
			}
			join(minecraft.screen != null ? minecraft.screen : new TitleScreen(), host, CoopHosting.nameOf(null, host));
		});
	}

	public static void join(Screen current, long host, String name) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null) {
			connect(current, host, name);
			return;
		}
		boolean singleplayer = minecraft.isLocalServer();
		minecraft.setScreen(new ConfirmScreen(yes -> {
			if (!yes) {
				minecraft.setScreen(current);
				return;
			}
			leaveWorld(minecraft, singleplayer);
			connect(new JoinMultiplayerScreen(new TitleScreen()), host, name);
		}, Component.translatable("steamcraft.join.confirm.title", name),
				Component.translatable(singleplayer ? "steamcraft.join.confirm.singleplayer" : "steamcraft.join.confirm.server"),
				Component.translatable("steamcraft.join.confirm.yes"), CommonComponents.GUI_CANCEL));
	}

	private static void connect(Screen parent, long host, String name) {
		Minecraft minecraft = Minecraft.getInstance();
		String address = SteamIds.toAddress(host);
		ServerData data = new ServerData(name, address, ServerData.Type.OTHER);
		ConnectScreen.startConnecting(parent, minecraft, ServerAddress.parseString(address), data, false, null);
	}

	private static void leaveWorld(Minecraft minecraft, boolean singleplayer) {
		//? if >=1.21.6 {
		/*if (minecraft.level != null) minecraft.level.disconnect(ClientLevel.DEFAULT_QUIT_MESSAGE);
		if (singleplayer) {
			minecraft.disconnectWithSavingScreen();
		} else {
			minecraft.disconnectWithProgressScreen();
		}
		*///?} else {
		if (minecraft.level != null) minecraft.level.disconnect();
		if (singleplayer) {
			minecraft.disconnect(new GenericMessageScreen(Component.translatable("menu.savingLevel")));
		} else {
			minecraft.disconnect();
		}
		//?}
	}
}
