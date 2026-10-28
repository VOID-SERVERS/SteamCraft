package dev.steamcraft.common.client.gui;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
//? if <1.21.5
import com.mojang.blaze3d.systems.RenderSystem;
//? if >=1.21.6 {
/*import net.minecraft.client.renderer.RenderPipelines;
*///?} else if >=1.21.2 {
/*import net.minecraft.client.renderer.RenderType;
*///?}
//? if >=1.21.2
/*import net.minecraft.util.ARGB;*/

public final class Gfx {
	private static final List<Consumer<GuiGraphics>> OVERLAYS = new ArrayList<>();

	private Gfx() {
	}

	public static void blit(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height,
							float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
		//? if >=1.21.6 {
		/*graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, regionWidth, regionHeight, textureWidth, textureHeight);
		*///?} else if >=1.21.2 {
		/*graphics.blit(RenderType::guiTextured, texture, x, y, u, v, width, height, regionWidth, regionHeight, textureWidth, textureHeight);
		*///?} else {
		graphics.blit(texture, x, y, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight);
		//?}
	}

	public static void sprite(GuiGraphics graphics, ResourceLocation sprite, int x, int y, int width, int height) {
		//? if >=1.21.6 {
		/*graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height);
		*///?} else if >=1.21.2 {
		/*graphics.blitSprite(RenderType::guiTextured, sprite, x, y, width, height);
		*///?} else {
		graphics.blitSprite(sprite, x, y, width, height);
		//?}
	}

	public static void outline(GuiGraphics graphics, int x, int y, int width, int height, int color) {
		//? if >=1.21.11 {
		/*graphics.renderOutline(x, y, width, height, color);
		*///?} else if >=1.21.9 {
		/*graphics.submitOutline(x, y, width, height, color);
		*///?} else {
		graphics.renderOutline(x, y, width, height, color);
		//?}
	}

	public static <T> CycleButton.Builder<T> cycle(Function<T, Component> label, T initial) {
		//? if >=1.21.11 {
		/*return CycleButton.builder(label, initial);
		*///?} else {
		return CycleButton.builder(label).withInitialValue(initial);
		//?}
	}

	public static void label(GuiGraphics graphics, Font font, Component text, int left, int top, int right, int bottom, int color) {
		int y = top + (bottom - top - font.lineHeight) / 2 + 1;
		graphics.drawString(font, fit(font, text, right - left), left, y, color);
	}

	public static FormattedCharSequence fit(Font font, Component text, int maxWidth) {
		if (font.width(text) <= maxWidth) return text.getVisualOrderText();
		FormattedText cut = font.substrByWidth(text, Math.max(0, maxWidth - font.width("...")));
		return Language.getInstance().getVisualOrder(FormattedText.composite(cut, FormattedText.of("...")));
	}

	public static void tooltip(GuiGraphics graphics, Font font, List<FormattedCharSequence> lines, int x, int y) {
		//? if >=1.21.6 {
		/*graphics.setTooltipForNextFrame(lines, x, y);
		*///?} else {
		graphics.renderTooltip(font, lines, x, y);
		//?}
	}

	public static void overlay(Consumer<GuiGraphics> draw) {
		OVERLAYS.add(draw);
	}

	public static void drawOverlays(GuiGraphics graphics) {
		if (OVERLAYS.isEmpty()) return;
		List<Consumer<GuiGraphics>> pending = new ArrayList<>(OVERLAYS);
		OVERLAYS.clear();
		//? if >=1.21.6 {
		/*graphics.nextStratum();
		for (Consumer<GuiGraphics> draw : pending) draw.accept(graphics);
		*///?} else {
		graphics.pose().pushPose();
		graphics.pose().translate(0, 0, 300);
		for (Consumer<GuiGraphics> draw : pending) draw.accept(graphics);
		graphics.pose().popPose();
		//?}
	}

	public static void setPixel(NativeImage image, int x, int y, int abgr) {
		//? if >=1.21.2 {
		/*image.setPixel(x, y, ARGB.fromABGR(abgr));
		*///?} else {
		image.setPixelRGBA(x, y, abgr);
		//?}
	}

	public static DynamicTexture texture(String name, NativeImage image) {
		//? if >=1.21.5 {
		/*return new DynamicTexture(() -> name, image);
		*///?} else {
		return new DynamicTexture(image);
		//?}
	}

	public static void blended(Runnable draw) {
		//? if >=1.21.5 {
		/*draw.run();
		*///?} else {
		RenderSystem.enableBlend();
		draw.run();
		RenderSystem.disableBlend();
		//?}
	}
}
