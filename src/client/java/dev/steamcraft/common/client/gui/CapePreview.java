package dev.steamcraft.common.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

final class CapePreview extends AbstractWidget {
	Supplier<ResourceLocation> texture = () -> null;

	CapePreview() {
		super(0, 0, 20, 32, CommonComponents.EMPTY);
		active = false;
	}

	@Override
	protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		ResourceLocation id = texture.get();
		if (id == null) return;
		Gfx.blit(graphics, id, getX(), getY(), getWidth(), getHeight(), 12, 1, 10, 16, 64, 32);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
	}
}
