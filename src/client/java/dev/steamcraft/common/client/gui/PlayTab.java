package dev.steamcraft.common.client.gui;

import dev.steamcraft.common.SteamRuntime;
import dev.steamcraft.common.client.SteamJoin;
import dev.steamcraft.common.client.SteamPeople;
import dev.steamcraft.core.config.CoopAccess;
import dev.steamcraft.core.identity.SteamIds;
import dev.steamcraft.core.steam.SteamFriend;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.tabs.Tab;
//? if >=26.2 {
/*import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.Layout;
*///?}
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

final class PlayTab implements Tab {
	//? if >=26.2
	/*private static final Layout EMPTY_LAYOUT = new GridLayout();*/
	private static final int MAX_WIDTH = 340;

	private static boolean offlineOpen;

	private final Screen screen;
	private final EditBox search;
	private final SteamList list;
	private final EditBox hostId;
	private final Button join;

	PlayTab(Screen screen) {
		this.screen = screen;
		Minecraft minecraft = Minecraft.getInstance();
		search = new EditBox(minecraft.font, 0, 0, 100, 16, Component.translatable("steamcraft.search"));
		search.setHint(Component.translatable("steamcraft.search").withStyle(ChatFormatting.DARK_GRAY));
		list = new SteamList(minecraft.font, this::rows);
		search.setResponder(text -> list.refresh());
		hostId = new EditBox(minecraft.font, 0, 0, 100, 18, Component.translatable("steamcraft.play.by_id"));
		hostId.setHint(Component.translatable("steamcraft.play.by_id").withStyle(ChatFormatting.DARK_GRAY));
		hostId.setMaxLength(40);
		join = Button.builder(Component.translatable("steamcraft.play.join"), b -> joinById()).width(70).build();
		join.active = false;
		hostId.setResponder(text -> {
			Long id = parseHost(text);
			join.active = id != null;
			hostId.setTextColor(id != null || text.isBlank() ? 0xE0E0E0 : 0xFF5555);
		});
	}

	@Override
	public Component getTabTitle() {
		return Component.translatable("steamcraft.tab.play");
	}

	//? if >=1.21.6 {
	/*@Override
	public Component getTabExtraNarration() {
		return Component.empty();
	}
	*///?}

	//? if >=26.2 {
	/*@Override
	public Layout getLayout() {
		return EMPTY_LAYOUT;
	}
	*///?}

	@Override
	public void visitChildren(Consumer<AbstractWidget> visitor) {
		visitor.accept(search);
		visitor.accept(list);
		visitor.accept(hostId);
		visitor.accept(join);
	}

	@Override
	public void doLayout(ScreenRectangle area) {
		int width = Math.min(MAX_WIDTH, area.width() - 20);
		int left = area.left() + (area.width() - width) / 2;
		int top = area.top() + 8;
		search.setRectangle(width, 16, left, top);
		int bottom = area.bottom() - 6;
		hostId.setRectangle(width - 74, 18, left, bottom - 19);
		join.setRectangle(70, 20, left + width - 70, bottom - 20);
		list.setRectangle(width, bottom - 26 - (top + 20), left, top + 20);
	}

	private List<SteamList.Row> rows() {
		List<SteamList.Row> rows = new ArrayList<>();
		if (SteamRuntime.client() == null) {
			rows.add(new SteamList.Note(Component.translatable("steamcraft.steam_not_running")));
			return rows;
		}
		String filter = search.getValue();
		List<SteamList.Row> hosts = new ArrayList<>();
		List<SteamList.Row> online = new ArrayList<>();
		List<SteamList.Row> offline = new ArrayList<>();
		for (SteamFriend friend : FriendRows.sorted(SteamPeople.friends())) {
			if (!FriendRows.matches(filter, friend.name)) continue;
			if (friend.hosting != null) {
				hosts.add(person(friend, joinAction(friend)));
			} else {
				(friend.isOnline() ? online : offline).add(person(friend, null));
			}
		}
		section(rows, "steamcraft.play.section.hosting", hosts);
		section(rows, "steamcraft.play.section.online", online);
		if (!offline.isEmpty()) {
			rows.add(new SteamList.Header(Component.translatable("steamcraft.panel.section.offline", offline.size()),
					() -> offlineOpen = !offlineOpen, !offlineOpen));
			if (offlineOpen) rows.addAll(offline);
		}
		if (hosts.isEmpty() && filter.isBlank()) rows.add(0, new SteamList.Note(Component.translatable("steamcraft.play.nobody_hosting")));
		if (rows.isEmpty()) rows.add(new SteamList.Note(Component.translatable("steamcraft.search.nothing")));
		return rows;
	}

	private static void section(List<SteamList.Row> rows, String title, List<SteamList.Row> people) {
		if (people.isEmpty()) return;
		rows.add(new SteamList.Header(Component.translatable(title, people.size())));
		rows.addAll(people);
	}

	private static SteamList.Person person(SteamFriend friend, SteamList.Action action) {
		return new SteamList.Person(friend.steamId, null, Component.literal(friend.name), FriendRows.status(friend), FriendRows.dot(friend),
				FriendRows.details(friend), action, Collections.emptyList());
	}

	private SteamList.Action joinAction(SteamFriend friend) {
		Component tooltip = friend.hosting == CoopAccess.INVITE ? Component.translatable("steamcraft.play.join.invite_only") : null;
		return new SteamList.Action(Component.translatable("steamcraft.play.join"), true, tooltip, () -> SteamJoin.join(screen, friend.steamId, friend.name));
	}

	private void joinById() {
		Long id = parseHost(hostId.getValue());
		if (id == null) return;
		SteamFriend friend = SteamPeople.friend(id);
		SteamJoin.join(screen, id, friend != null ? friend.name : SteamIds.format(id));
	}

	private static Long parseHost(String text) {
		String value = text.trim();
		if (value.isEmpty()) return null;
		Long fromAddress = SteamIds.fromAddressHost(value);
		if (fromAddress != null) return fromAddress;
		try {
			return SteamIds.parse(value);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
