package dev.steamcraft.common.client.gui;

import dev.steamcraft.common.SteamRuntime;
import dev.steamcraft.common.client.SteamPeople;
import dev.steamcraft.core.steam.SteamFriend;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.narration.NarrationElementOutput;
//? if >=1.21.9
/*import net.minecraft.client.input.InputWithModifiers;*/
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class SteamMenuButton extends AbstractButton {
	private static final WidgetSprites SPRITES = new WidgetSprites(ResourceLocation.withDefaultNamespace("widget/button"),
			ResourceLocation.withDefaultNamespace("widget/button_disabled"), ResourceLocation.withDefaultNamespace("widget/button_highlighted"));

	private final Screen parent;

	public SteamMenuButton(Screen parent, int x, int y, int width, int height) {
		super(x, y, width, height, Component.translatable("steamcraft.menu.button"));
		this.parent = parent;
	}

	//? if >=1.21.9 {
	/*@Override
	public void onPress(InputWithModifiers input) {
		open();
	}
	*///?} else {
	@Override
	public void onPress() {
		open();
	}
	//?}

	private void open() {
		Minecraft.getInstance().setScreen(new SteamCraftScreen(parent));
	}

	//? if >=1.21.11 {
	/*@Override
	protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		draw(graphics);
	}
	*///?} else {
	@Override
	protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		draw(graphics);
	}
	//?}

	private void draw(GuiGraphics graphics) {
		Minecraft minecraft = Minecraft.getInstance();
		Gfx.sprite(graphics, SPRITES.get(active, isHoveredOrFocused()), getX(), getY(), getWidth(), getHeight());
		Component detail = detail();
		int color = active ? 0xFFFFFFFF : 0xFFA0A0A0;
		if (getHeight() < 2 * minecraft.font.lineHeight + 6) {
			Gfx.label(graphics, minecraft.font, getMessage(), getX() + 2, getY(), getRight() - 2, getBottom(), color);
			return;
		}
		int middle = getY() + getHeight() / 2;
		graphics.drawCenteredString(minecraft.font, getMessage(), getX() + getWidth() / 2, middle - minecraft.font.lineHeight, color);
		graphics.drawCenteredString(minecraft.font, detail, getX() + getWidth() / 2, middle + 1, 0xFFA8D86A);
	}

	private static Component detail() {
		if (SteamRuntime.client() == null) return Component.translatable("steamcraft.menu.no_steam");
		int hosting = 0;
		for (SteamFriend friend : SteamPeople.friends()) {
			if (friend.hosting != null) hosting++;
		}
		return hosting > 0 ? Component.translatable("steamcraft.menu.hosting", hosting) : Component.translatable("steamcraft.menu.idle");
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		defaultButtonNarrationText(output);
	}
}
