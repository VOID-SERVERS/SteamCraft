package dev.steamcraft.common.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public final class SteamPanelScreen extends Screen {
	private final Screen parent;

	public SteamPanelScreen(Screen parent) {
		super(Component.translatable("steamcraft.panel.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int panelWidth = Math.min(width - 16, 260);
		SteamPanel.addTo(this, new ScreenRectangle((width - panelWidth) / 2, 8, panelWidth, height - 44), this::addRenderableWidget);
		addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, b -> onClose()).bounds(width / 2 - 100, height - 28, 200, 20).build());
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		Gfx.drawOverlays(graphics);
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}
}
