package dev.steamcraft.common.client.gui;

import dev.steamcraft.common.client.CoopHosting;
import dev.steamcraft.common.client.SteamPeople;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
//? if >=1.21.2 {
/*import net.minecraft.client.gui.components.toasts.ToastManager;
*///?} else {
import net.minecraft.client.gui.components.toasts.ToastComponent;
//?}

public final class KnockToast implements Toast {
	private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("toast/system");
	private static final long SHOW_MILLIS = 10000;

	private final long steamId;
	private Visibility visibility = Visibility.SHOW;

	public KnockToast(long steamId) {
		this.steamId = steamId;
	}

	public static void show(long steamId) {
		toasts().addToast(new KnockToast(steamId));
	}

	//? if >=1.21.2 {
	/*public static ToastManager toasts() {
		return Minecraft.getInstance().getToastManager();
	}

	@Override
	public Visibility getWantedVisibility() {
		return visibility;
	}

	@Override
	public void update(ToastManager toasts, long visibleMillis) {
		visibility = visibleMillis >= SHOW_MILLIS * toasts.getNotificationDisplayTimeMultiplier() ? Visibility.HIDE : Visibility.SHOW;
	}

	@Override
	public void render(GuiGraphics graphics, Font font, long visibleMillis) {
		draw(graphics, font);
	}
	*///?} else {
	public static ToastComponent toasts() {
		return Minecraft.getInstance().getToasts();
	}

	@Override
	public Visibility render(GuiGraphics graphics, ToastComponent toasts, long visibleMillis) {
		draw(graphics, toasts.getMinecraft().font);
		visibility = visibleMillis >= SHOW_MILLIS * toasts.getNotificationDisplayTimeMultiplier() ? Visibility.HIDE : Visibility.SHOW;
		return visibility;
	}
	//?}

	private void draw(GuiGraphics graphics, Font font) {
		Gfx.sprite(graphics, BACKGROUND, 0, 0, width(), height());
		SteamPeople.drawAvatar(graphics, steamId, 8, 8, 16);
		int textWidth = width() - 32;
		Component title = Component.translatable("steamcraft.knock.title", CoopHosting.nameOf(CoopHosting.hostedWorld(), steamId));
		graphics.drawString(font, firstLine(font, title, textWidth), 30, 7, 0xFFFFFF00, false);
		graphics.drawString(font, firstLine(font, Component.translatable("steamcraft.knock.hint"), textWidth), 30, 18, 0xFFFFFFFF, false);
	}

	private static FormattedCharSequence firstLine(Font font, Component text, int width) {
		return font.split(text, width).stream().findFirst().orElse(FormattedCharSequence.EMPTY);
	}

	@Override
	public Object getToken() {
		return steamId;
	}
}
