package dev.steamcraft.common.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
//? if >=1.21.9
/*import net.minecraft.client.input.MouseButtonEvent;*/
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

final class Dropdown<T> extends AbstractWidget {
	interface Icon<T> {
		void draw(GuiGraphics graphics, T value, int x, int y, int size);
	}

	private static final WidgetSprites SPRITES = new WidgetSprites(ResourceLocation.withDefaultNamespace("widget/button"),
			ResourceLocation.withDefaultNamespace("widget/button_disabled"), ResourceLocation.withDefaultNamespace("widget/button_highlighted"));
	private static final int ROW = 16;
	private static final int ICON = 12;
	private static final int MAX_ROWS = 8;

	private final Font font;
	private final Component title;
	private final Supplier<List<T>> options;
	private final Function<T, Component> label;
	private final Consumer<T> onPick;
	private Icon<T> icon;
	private T selected;
	private Component placeholder = CommonComponents.EMPTY;
	private int limitTop = 0;
	private int limitBottom = Integer.MAX_VALUE;

	private boolean open;
	private List<T> shown = Collections.emptyList();
	private int scroll;
	private int listTop;
	private int listRows;

	Dropdown(Font font, Component title, Supplier<List<T>> options, Function<T, Component> label, Consumer<T> onPick) {
		super(0, 0, 100, 20, title);
		this.font = font;
		this.title = title;
		this.options = options;
		this.label = label;
		this.onPick = onPick;
	}

	Dropdown<T> icon(Icon<T> icon) {
		this.icon = icon;
		return this;
	}

	void setSelected(T value, Component placeholder) {
		this.selected = value;
		this.placeholder = placeholder;
		setMessage(Component.translatable("options.generic_value", title, value != null ? label.apply(value) : placeholder));
	}

	void setLimits(int top, int bottom) {
		limitTop = top;
		limitBottom = bottom;
	}

	boolean isOpen() {
		return open;
	}

	void close() {
		open = false;
	}

	private void openList() {
		shown = options.get();
		open = true;
		int below = (limitBottom - getBottom()) / ROW;
		int above = (getY() - limitTop) / ROW;
		int wanted = Math.min(MAX_ROWS, Math.max(1, shown.size()));
		boolean down = below >= wanted || below >= above;
		listRows = Math.max(1, Math.min(wanted, down ? below : above));
		listTop = down ? getBottom() : getY() - listRows * ROW;
		int index = shown.indexOf(selected);
		scroll = Mth.clamp(index - listRows / 2, 0, Math.max(0, shown.size() - listRows));
	}

	@Override
	protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		boolean overHeader = mouseX >= getX() && mouseX < getRight() && mouseY >= getY() && mouseY < getBottom();
		Gfx.sprite(graphics, SPRITES.get(active, open || overHeader && active), getX(), getY(), getWidth(), getHeight());
		int textLeft = getX() + 6;
		if (icon != null && selected != null) {
			icon.draw(graphics, selected, textLeft, getY() + (getHeight() - ICON) / 2, ICON);
			textLeft += ICON + 4;
		}
		int color = active ? 0xFFFFFFFF : 0xFFA0A0A0;
		Gfx.label(graphics, font, getMessage(), textLeft, getY(), getRight() - 14, getBottom(), color);
		graphics.drawString(font, open ? "▲" : "▼", getRight() - 11, getY() + (getHeight() - 8) / 2, color, false);
		if (open) Gfx.overlay(overlay -> renderList(overlay, mouseX, mouseY));
	}

	private void renderList(GuiGraphics graphics, int mouseX, int mouseY) {
		int bottom = listTop + listRows * ROW;
		graphics.fill(getX(), listTop, getRight(), bottom, 0xFF505050);
		graphics.fill(getX() + 1, listTop + (listTop < getY() ? 1 : 0), getRight() - 1, bottom - 1, 0xF0101010);
		if (shown.isEmpty()) {
			graphics.drawString(font, placeholder, getX() + 6, listTop + 4, 0xFFA0A0A0, false);
		}
		for (int row = 0; row < listRows && scroll + row < shown.size(); row++) {
			T value = shown.get(scroll + row);
			int y = listTop + row * ROW;
			boolean hovered = mouseX >= getX() && mouseX < getRight() && mouseY >= y && mouseY < y + ROW;
			if (hovered) graphics.fill(getX() + 1, y, getRight() - 1, y + ROW, 0xFF2F4F7F);
			int textLeft = getX() + 6;
			if (icon != null) {
				icon.draw(graphics, value, textLeft, y + (ROW - ICON) / 2, ICON);
				textLeft += ICON + 4;
			}
			boolean current = Objects.equals(value, selected);
			graphics.drawString(font, label.apply(value), textLeft, y + 4, current ? 0xFFFFFF60 : 0xFFFFFFFF, false);
		}
		if (shown.size() > listRows) {
			int barHeight = Math.max(6, listRows * ROW * listRows / shown.size());
			int barY = listTop + (listRows * ROW - barHeight) * scroll / Math.max(1, shown.size() - listRows);
			graphics.fill(getRight() - 3, barY, getRight() - 1, barY + barHeight, 0x80FFFFFF);
		}
	}

	private boolean overList(double mouseX, double mouseY) {
		return open && mouseX >= getX() && mouseX < getRight() && mouseY >= listTop && mouseY < listTop + listRows * ROW;
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		return overList(mouseX, mouseY) || super.isMouseOver(mouseX, mouseY);
	}

	//? if >=1.21.9 {
	/*@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		return click(event.x(), event.y(), event.button());
	}
	*///?} else {
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		return click(mouseX, mouseY, button);
	}
	//?}

	private boolean click(double mouseX, double mouseY, int button) {
		if (!active || !visible) return false;
		boolean overHeader = mouseX >= getX() && mouseX < getRight() && mouseY >= getY() && mouseY < getBottom();
		if (open) {
			if (overList(mouseX, mouseY)) {
				int index = scroll + (int) ((mouseY - listTop) / ROW);
				open = false;
				if (button == 0 && index < shown.size()) {
					playClick();
					onPick.accept(shown.get(index));
				}
				return true;
			}
			open = false;
			return overHeader;
		}
		if (button != 0 || !overHeader) return false;
		playClick();
		openList();
		return true;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (!overList(mouseX, mouseY)) return false;
		scroll = Mth.clamp(scroll - (int) Math.signum(scrollY), 0, Math.max(0, shown.size() - listRows));
		return true;
	}

	private static void playClick() {
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		output.add(NarratedElementType.TITLE, getMessage());
	}
}
