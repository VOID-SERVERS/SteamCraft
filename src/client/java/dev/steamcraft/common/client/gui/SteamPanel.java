package dev.steamcraft.common.client.gui;

import com.mojang.authlib.GameProfile;
import dev.steamcraft.common.SteamCraftServer;
import dev.steamcraft.common.SteamRuntime;
import dev.steamcraft.common.client.CoopHosting;
import dev.steamcraft.common.client.SteamPeople;
import dev.steamcraft.core.config.CoopAccess;
import dev.steamcraft.core.steam.SteamFriend;
import dev.steamcraft.common.Profiles;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public final class SteamPanel {
	public static final int MIN_WIDTH = 150;
	public static final int MAX_WIDTH = 210;
	private static final int PADDING = 5;

	private static boolean offlineOpen;

	private final Screen screen;
	private final Minecraft minecraft = Minecraft.getInstance();
	private final Font font;
	private EditBox search;
	private SteamList list;

	private SteamPanel(Screen screen) {
		this.screen = screen;
		this.font = minecraft.font;
	}

	public static void addTo(Screen screen, ScreenRectangle area, Consumer<AbstractWidget> add) {
		new SteamPanel(screen).build(area, add);
	}

	private void build(ScreenRectangle area, Consumer<AbstractWidget> add) {
		int x = area.left();
		int width = area.width();
		int y = area.top();
		add.accept(new Frame(area));
		y += PADDING;
		int titleY = y;
		y += font.lineHeight + 4;

		SteamCraftServer host = CoopHosting.hostedWorld();
		boolean steam = SteamRuntime.client() != null;
		if (host != null && steam) {
			if (host.isHostingOverSteam()) {
				int small = 16;
				Button close = Button.builder(Component.literal("✕"), b -> {
					CoopHosting.close();
					minecraft.setScreen(screen);
				}).bounds(x + width - PADDING - small, titleY - 4, small, small).build();
				close.setTooltip(Tooltip.create(Component.translatable("steamcraft.panel.close")));
				Button settings = Button.builder(Component.literal("⚙"), b -> minecraft.setScreen(HostSettingsScreen.forCurrent(screen)))
						.bounds(close.getX() - small - 2, titleY - 4, small, small).build();
				settings.setTooltip(Tooltip.create(Component.translatable("steamcraft.panel.settings")));
				add.accept(settings);
				add.accept(close);
				y += font.lineHeight + 4;
			} else {
				add.accept(Button.builder(Component.translatable("steamcraft.panel.open"), b -> minecraft.setScreen(HostSettingsScreen.forNew(screen)))
						.bounds(x + PADDING, y, width - 2 * PADDING, 20).build());
				y += 24;
			}
		}

		search = new EditBox(font, x + PADDING, y, width - 2 * PADDING, 16, Component.translatable("steamcraft.search"));
		search.setHint(Component.translatable("steamcraft.search").withStyle(ChatFormatting.DARK_GRAY));
		search.setResponder(text -> list.refresh());
		add.accept(search);
		y += 20;

		list = new SteamList(font, this::rows);
		list.setRectangle(width - 2 * PADDING, area.bottom() - PADDING - y, x + PADDING, y);
		add.accept(list);
	}

	private List<SteamList.Row> rows() {
		List<SteamList.Row> rows = new ArrayList<>();
		if (SteamRuntime.client() == null) {
			rows.add(new SteamList.Note(Component.translatable("steamcraft.steam_not_running")));
			return rows;
		}
		SteamCraftServer host = CoopHosting.hostedWorld();
		boolean open = host != null && host.isHostingOverSteam();
		String filter = search.getValue();

		if (open && host.access() == CoopAccess.INVITE) {
			List<SteamList.Row> knocking = new ArrayList<>();
			for (long steamId : host.knocks()) {
				String name = CoopHosting.nameOf(host, steamId);
				if (!FriendRows.matches(filter, name)) continue;
				knocking.add(new SteamList.Person(steamId, null, Component.literal(name), Component.translatable("steamcraft.panel.knocking"),
						FriendRows.YELLOW, null, new SteamList.Action(Component.translatable("steamcraft.panel.let_in"), true, null, () -> invite(host, steamId, name)),
						List.of(new SteamList.MenuItem(Component.translatable("steamcraft.panel.dismiss"), () -> host.dismissKnock(steamId)))));
			}
			section(rows, "steamcraft.panel.section.knocking", knocking);
		}

		Set<Long> inWorld = new HashSet<>();
		section(rows, "steamcraft.panel.section.world", playersInWorld(host, filter, inWorld));

		List<SteamFriend> online = new ArrayList<>();
		List<SteamFriend> offline = new ArrayList<>();
		for (SteamFriend friend : FriendRows.sorted(SteamPeople.friends())) {
			if (inWorld.contains(friend.steamId) || !FriendRows.matches(filter, friend.name)) continue;
			(friend.isOnline() ? online : offline).add(friend);
		}
		List<SteamList.Row> onlineRows = new ArrayList<>();
		for (SteamFriend friend : online) onlineRows.add(friendRow(host, open, friend));
		section(rows, "steamcraft.panel.section.online", onlineRows);
		if (!offline.isEmpty()) {
			rows.add(new SteamList.Header(Component.translatable("steamcraft.panel.section.offline", offline.size()),
					() -> offlineOpen = !offlineOpen, !offlineOpen));
			if (offlineOpen) {
				for (SteamFriend friend : offline) rows.add(friendRow(host, open, friend));
			}
		}
		if (rows.isEmpty()) {
			rows.add(new SteamList.Note(Component.translatable(filter.isBlank() ? "steamcraft.panel.no_friends" : "steamcraft.search.nothing")));
		}
		return rows;
	}

	private static void section(List<SteamList.Row> rows, String title, List<SteamList.Row> people) {
		if (people.isEmpty()) return;
		rows.add(new SteamList.Header(Component.translatable(title, people.size())));
		rows.addAll(people);
	}

	private List<SteamList.Row> playersInWorld(SteamCraftServer host, String filter, Set<Long> steamIds) {
		ClientPacketListener connection = minecraft.getConnection();
		if (connection == null || minecraft.player == null) return Collections.emptyList();
		List<SteamList.Row> rows = new ArrayList<>();
		for (PlayerInfo info : connection.getOnlinePlayers()) {
			GameProfile profile = info.getProfile();
			if (Profiles.id(profile).equals(minecraft.player.getUUID())) continue;
			String gameName = Profiles.name(profile);
			Long steamId = host != null ? host.steamIdOf(profile) : null;
			if (steamId == null) steamId = SteamPeople.friendPlaying(Profiles.id(profile), gameName);
			boolean friend = steamId != null && SteamPeople.friend(steamId) != null;
			String steamName = steamId != null ? SteamPeople.personaName(steamId) : null;
			if (!FriendRows.matches(filter, gameName, steamName)) continue;
			if (steamId != null) steamIds.add(steamId);

			MutableComponent name = Component.literal(steamName != null ? steamName : gameName);
			if (steamName != null && !steamName.equalsIgnoreCase(gameName)) {
				name.append(Component.literal(" (" + gameName + ")").withStyle(ChatFormatting.GRAY));
			}
			String who = friend ? "steamcraft.panel.who.friend" : steamId != null ? "steamcraft.panel.who.steam" : "steamcraft.panel.who.player";
			MutableComponent detail = Component.translatable(who);
			List<SteamList.MenuItem> menu = new ArrayList<>();
			if (host != null && steamId != null && host.steamIdOf(profile) != null) {
				boolean op = host.operators().containsKey(steamId);
				if (op) detail.append(" · ").append(Component.translatable("steamcraft.panel.operator"));
				long id = steamId;
				menu.add(op ? new SteamList.MenuItem(Component.translatable("steamcraft.panel.deop"), () -> host.deop(id))
						: new SteamList.MenuItem(Component.translatable("steamcraft.panel.op"), () -> host.op(id, gameName)));
			} else if (host != null) {
				detail.append(" · ").append(Component.translatable("steamcraft.panel.lan_player"));
			}
			if (host != null) detail.append(" · ").append(Component.translatable("steamcraft.panel.actions"));
			if (host != null) menu.add(new SteamList.MenuItem(Component.translatable("steamcraft.panel.kick"), () -> host.kick(Profiles.id(profile))));
			rows.add(new SteamList.Person(steamId != null ? steamId : 0, info.getSkin(), name, detail,
					friend ? FriendRows.GREEN : steamId != null ? FriendRows.BLUE : 0, null, null, menu));
		}
		return rows;
	}

	private SteamList.Person friendRow(SteamCraftServer host, boolean open, SteamFriend friend) {
		SteamList.Action action = null;
		if (host != null) {
			int minutes = open ? host.inviteMinutesLeft(friend.steamId) : 0;
			if (!open) {
				action = new SteamList.Action(Component.translatable("steamcraft.panel.invite"), false,
						Component.translatable("steamcraft.panel.invite.open_first"), () -> {
				});
			} else if (minutes > 0) {
				action = new SteamList.Action(Component.translatable("steamcraft.panel.invited", minutes), true,
						Component.translatable("steamcraft.panel.invited.tooltip", minutes), () -> invite(host, friend.steamId, friend.name));
			} else {
				action = new SteamList.Action(Component.translatable("steamcraft.panel.invite"), true, null, () -> invite(host, friend.steamId, friend.name));
			}
		}
		return new SteamList.Person(friend.steamId, null, Component.literal(friend.name), FriendRows.status(friend), FriendRows.dot(friend),
				FriendRows.details(friend), action, Collections.emptyList());
	}

	private void invite(SteamCraftServer host, long steamId, String name) {
		if (host.invite(steamId)) return;
		SystemToast.add(KnockToast.toasts(), SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
				Component.translatable("steamcraft.panel.invite.failed"), Component.literal(name));
	}

	private final class Frame extends AbstractWidget {
		Frame(ScreenRectangle area) {
			super(area.left(), area.top(), area.width(), area.height(), CommonComponents.EMPTY);
			active = false;
		}

		@Override
		protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
			graphics.fill(getX(), getY(), getRight(), getBottom(), 0x80000000);
			Gfx.outline(graphics, getX(), getY(), getWidth(), getHeight(), 0x40FFFFFF);
			int y = getY() + PADDING;
			graphics.drawString(font, Component.translatable("steamcraft.panel.title"), getX() + PADDING, y, 0xFFFFFFFF);
			SteamCraftServer host = CoopHosting.hostedWorld();
			if (host == null || !host.isHostingOverSteam()) return;
			Component state = Component.translatable("steamcraft.panel.open_to", FriendRows.access(host.access()),
					Component.translatable(host.cheats() ? "steamcraft.panel.cheats_on" : "steamcraft.panel.cheats_off"));
			graphics.fill(getX() + PADDING, y + font.lineHeight + 6, getX() + PADDING + 4, y + font.lineHeight + 10, FriendRows.GREEN);
			graphics.drawString(font, state, getX() + PADDING + 8, y + font.lineHeight + 4, 0xFFB0B0B0);
		}

		@Override
		public boolean isMouseOver(double mouseX, double mouseY) {
			return false;
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
		}
	}
}
