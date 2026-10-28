package dev.steamcraft.common.client.mixin;

import dev.steamcraft.common.client.gui.Gfx;
import dev.steamcraft.common.client.gui.SkinPanel;
import dev.steamcraft.common.client.gui.SteamMenuButton;
import dev.steamcraft.common.client.gui.SteamPanel;
import dev.steamcraft.common.client.gui.SteamPanelScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
abstract class PauseScreenMixin extends Screen {
	private static final int MENU_HALF_WIDTH = 102;
	private static final int GAP = 10;
	private static final int MARGIN = 6;
	private static final int BUTTON_WIDTH = 120;

	protected PauseScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void steamcraft$addPanels(CallbackInfo ci) {
		if (!((PauseScreen) (Object) this).showsPauseMenu()) return;
		int menuLeft = width / 2 - MENU_HALF_WIDTH - GAP;
		int menuRight = width / 2 + MENU_HALF_WIDTH + GAP;
		int buttonTop = height - MARGIN - 20;

		if (SkinPanel.isShown() && menuLeft - SkinPanel.WIDTH >= MARGIN) {
			int panelHeight = SkinPanel.height();
			int middle = menuMiddle();
			int top = Math.max(MARGIN, Math.min(height - MARGIN - panelHeight, middle - panelHeight / 2));
			SkinPanel.addTo(new ScreenRectangle(menuLeft - SkinPanel.WIDTH, top, SkinPanel.WIDTH, panelHeight), this::addRenderableWidget);
		}

		int panelWidth = Math.min(SteamPanel.MAX_WIDTH, width - MARGIN - menuRight);
		if (panelWidth >= SteamPanel.MIN_WIDTH) {
			SteamPanel.addTo(this, new ScreenRectangle(width - MARGIN - panelWidth, MARGIN, panelWidth, buttonTop - 4 - MARGIN),
					this::addRenderableWidget);
		} else {
			addRenderableWidget(Button.builder(Component.translatable("steamcraft.panel.title"), b -> minecraft.setScreen(new SteamPanelScreen(this)))
					.bounds(width - MARGIN - 90, MARGIN, 90, 20).build());
		}
		addRenderableWidget(new SteamMenuButton(this, width - MARGIN - BUTTON_WIDTH, buttonTop, BUTTON_WIDTH, 20));
	}

	@Inject(method = "render", at = @At("TAIL"))
	private void steamcraft$drawOverlays(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		Gfx.drawOverlays(graphics);
	}

	private int menuMiddle() {
		int top = Integer.MAX_VALUE;
		int bottom = Integer.MIN_VALUE;
		for (GuiEventListener child : children()) {
			if (!(child instanceof AbstractWidget widget) || !(widget instanceof AbstractButton)) continue;
			if (widget.getX() < width / 2 - MENU_HALF_WIDTH - 1 || widget.getRight() > width / 2 + MENU_HALF_WIDTH + 1) continue;
			top = Math.min(top, widget.getY());
			bottom = Math.max(bottom, widget.getBottom());
		}
		return top <= bottom ? (top + bottom) / 2 : height / 4 + 60;
	}
}
