package dev.steamcraft.common.client.gui;

import com.mojang.blaze3d.platform.Window;
import dev.steamcraft.common.SteamRuntime;
import dev.steamcraft.common.client.SteamPeople;
import dev.steamcraft.core.steam.SteamClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
//? if >=1.21.9
/*import net.minecraft.client.input.KeyEvent;*/
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.List;

public final class SteamCraftScreen extends Screen {
	private final Screen parent;
	private HeaderAndFooterLayout layout;
	private final TabManager tabManager = new TabManager(this::addRenderableWidget, this::removeWidget);
	private TabNavigationBar tabs;
	private SkinTab skinTab;
	private SettingsTab settingsTab;

	public SteamCraftScreen(Screen parent) {
		super(Component.translatable("steamcraft.screen.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		layout = new HeaderAndFooterLayout(this);
		skinTab = new SkinTab();
		settingsTab = new SettingsTab();
		tabs = TabNavigationBar.builder(tabManager, width)
				.addTabs(new PlayTab(this), skinTab, settingsTab)
				.build();
		addRenderableWidget(tabs);
		layout.addToFooter(Button.builder(CommonComponents.GUI_BACK, b -> onClose()).width(200).build());
		layout.visitWidgets(widget -> {
			widget.setTabOrderGroup(1);
			addRenderableWidget(widget);
		});
		tabs.selectTab(0, false);
		repositionElements();
	}

	@Override
	public void repositionElements() {
		if (tabs == null) return;
		//? if >=26.2 {
		/*tabs.arrangeElements(width);
		*///?} else {
		tabs.setWidth(width);
		tabs.arrangeElements();
		//?}
		int top = tabs.getRectangle().bottom();
		tabManager.setTabArea(new ScreenRectangle(0, top, width, height - layout.getFooterHeight() - top));
		layout.setHeaderHeight(top);
		layout.arrangeElements();
	}

	@Override
	public void tick() {
		super.tick();
		if (tabManager.getCurrentTab() == settingsTab) settingsTab.updateSteamState();
	}

	//? if >=1.21.9 {
	/*@Override
	public boolean keyPressed(KeyEvent event) {
		return tabs.keyPressed(event) || super.keyPressed(event);
	}
	*///?} else {
	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		return tabs.keyPressed(keyCode) || super.keyPressed(keyCode, scanCode, modifiers);
	}
	//?}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		Gfx.drawOverlays(graphics);
		Component identity = identity();
		graphics.drawString(font, identity, 8, height - layout.getFooterHeight() / 2 - font.lineHeight / 2, 0xFFA0A0A0);
	}

	private static Component identity() {
		SteamClient steam = SteamRuntime.client();
		if (steam == null) return Component.translatable("steamcraft.steam_off");
		String name = SteamPeople.personaName(steam.localSteamId());
		return Component.translatable("steamcraft.screen.identity", name != null ? name : "...");
	}

	@Override
	public void onFilesDrop(List<Path> files) {
		if (tabManager.getCurrentTab() != skinTab) return;
		Window window = minecraft.getWindow();
		double mouseX = minecraft.mouseHandler.xpos() * window.getGuiScaledWidth() / window.getScreenWidth();
		skinTab.filesDropped(files, mouseX < width / 2.0);
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}
}
