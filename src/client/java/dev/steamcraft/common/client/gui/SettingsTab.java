package dev.steamcraft.common.client.gui;

import dev.steamcraft.common.SteamRuntime;
import dev.steamcraft.common.client.SteamPeople;
import dev.steamcraft.core.access.Invites;
import dev.steamcraft.core.config.CoopAccess;
import dev.steamcraft.core.identity.SteamIds;
import dev.steamcraft.core.steam.SteamClient;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.tabs.Tab;
//? if >=26.2 {
/*import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.Layout;
*///?}
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

final class SettingsTab implements Tab {
	//? if >=26.2
	/*private static final Layout EMPTY_LAYOUT = new GridLayout();*/
	private static final int WIDTH = 310;

	private final Minecraft minecraft = Minecraft.getInstance();
	private final MultiLineTextWidget steamState;
	private final Button copyId;
	private final CycleButton<CoopAccess> access;
	private final MultiLineTextWidget accessNote;
	private final CycleButton<Boolean> showWorldName;
	private final MultiLineTextWidget showWorldNameNote;

	SettingsTab() {
		steamState = new MultiLineTextWidget(Component.empty(), minecraft.font);
		copyId = Button.builder(Component.translatable("steamcraft.settings.copy_id"), b -> {
			SteamClient steam = SteamRuntime.client();
			if (steam != null) minecraft.keyboardHandler.setClipboard(SteamIds.format(steam.localSteamId()));
		}).build();
		access = Gfx.<CoopAccess>cycle(FriendRows::access, SteamRuntime.config().coop.access)
				.withValues(CoopAccess.values())
				.create(0, 0, WIDTH, 20, Component.translatable("steamcraft.settings.access"), (b, value) -> {
					SteamRuntime.config().coop.access = value;
					SteamRuntime.saveConfig();
				});
		accessNote = new MultiLineTextWidget(Component.translatable("steamcraft.settings.access.note", Invites.VALID_MINUTES)
				.withStyle(ChatFormatting.GRAY), minecraft.font);
		showWorldName = CycleButton.onOffBuilder(SteamRuntime.config().presence.showWorldName)
				.create(0, 0, WIDTH, 20, Component.translatable("steamcraft.settings.show_world_name"), (b, value) -> {
					SteamRuntime.config().presence.showWorldName = value;
					SteamRuntime.saveConfig();
				});
		showWorldNameNote = new MultiLineTextWidget(Component.translatable("steamcraft.settings.show_world_name.note")
				.withStyle(ChatFormatting.GRAY), minecraft.font);
		updateSteamState();
	}

	@Override
	public Component getTabTitle() {
		return Component.translatable("steamcraft.tab.settings");
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
		updateSteamState();
		visitor.accept(steamState);
		visitor.accept(copyId);
		visitor.accept(access);
		visitor.accept(accessNote);
		visitor.accept(showWorldName);
		visitor.accept(showWorldNameNote);
	}

	@Override
	public void doLayout(ScreenRectangle area) {
		int width = Math.min(WIDTH, area.width() - 20);
		int left = area.left() + (area.width() - width) / 2;
		int y = area.top() + 12;
		steamState.setMaxWidth(width - 110);
		steamState.setPosition(left, y + 6);
		copyId.setRectangle(100, 20, left + width - 100, y);
		y += 32;
		access.setRectangle(width, 20, left, y);
		y += 24;
		accessNote.setMaxWidth(width);
		accessNote.setPosition(left, y);
		y += accessNote.getHeight() + 12;
		showWorldName.setRectangle(width, 20, left, y);
		y += 24;
		showWorldNameNote.setMaxWidth(width);
		showWorldNameNote.setPosition(left, y);
	}

	void updateSteamState() {
		SteamClient steam = SteamRuntime.client();
		copyId.active = steam != null;
		if (steam == null) {
			steamState.setMessage(Component.translatable("steamcraft.settings.steam.off").withStyle(ChatFormatting.RED));
			return;
		}
		String name = SteamPeople.personaName(steam.localSteamId());
		steamState.setMessage(Component.translatable("steamcraft.settings.steam.on", name != null ? name : "..."));
	}
}
