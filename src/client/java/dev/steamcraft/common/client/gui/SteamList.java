package dev.steamcraft.common.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.steamcraft.common.client.SteamPeople;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
//? if >=1.21.9
/*import net.minecraft.client.input.MouseButtonEvent;*/
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public final class SteamList extends AbstractWidget {
	private static final long REFRESH_MILLIS = 500;
	private static final int HEADER_HEIGHT = 14;
	private static final int PERSON_HEIGHT = 24;
	private static final int AVATAR = 16;
	private static final int BUTTON_HEIGHT = 14;
	private static final int MENU_ITEM_HEIGHT = 13;
	private static final ResourceLocation BUTTON = ResourceLocation.withDefaultNamespace("widget/button");
	private static final ResourceLocation BUTTON_DISABLED = ResourceLocation.withDefaultNamespace("widget/button_disabled");
	private static final ResourceLocation BUTTON_HIGHLIGHTED = ResourceLocation.withDefaultNamespace("widget/button_highlighted");

	public sealed interface Row permits Header, Note, Person {
	}

	public record Header(Component text, Runnable toggle, boolean collapsed) implements Row {
		public Header(Component text) {
			this(text, null, false);
		}
	}

	public record Note(Component text) implements Row {
	}

	public record Person(long steamId, PlayerSkin face, Component name, Component detail, int dotColor,
						 Component tooltip, Action action, List<MenuItem> menu) implements Row {
	}

	public record Action(Component label, boolean active, Component tooltip, Runnable onPress) {
	}

	public record MenuItem(Component label, Runnable onPress) {
	}

	private record OpenMenu(int x, int y, int width, List<MenuItem> items) {
		int height() {
			return items.size() * MENU_ITEM_HEIGHT + 4;
		}

		boolean contains(double mouseX, double mouseY) {
			return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height();
		}
	}

	private final Supplier<List<Row>> source;
	private final Font font;
	private List<Row> rows = Collections.emptyList();
	private long refreshedAt;
	private double scroll;
	private OpenMenu menu;

	public SteamList(Font font, Supplier<List<Row>> source) {
		super(0, 0, 100, 100, CommonComponents.EMPTY);
		this.font = font;
		this.source = source;
	}

	public void refresh() {
		refreshedAt = 0;
	}

	private void refreshIfDue() {
		long now = System.currentTimeMillis();
		if (now - refreshedAt < REFRESH_MILLIS) return;
		refreshedAt = now;
		rows = source.get();
		scroll = Mth.clamp(scroll, 0, maxScroll());
	}

	private int rowHeight(Row row) {
		if (row instanceof Header) return HEADER_HEIGHT;
		if (row instanceof Note note) return font.split(note.text(), textWidth()).size() * font.lineHeight + 6;
		return PERSON_HEIGHT;
	}

	private int textWidth() {
		return width - 10;
	}

	private int contentHeight() {
		int height = 2;
		for (Row row : rows) height += rowHeight(row);
		return height;
	}

	private double maxScroll() {
		return Math.max(0, contentHeight() - height);
	}

	@Override
	protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		refreshIfDue();
		graphics.fill(getX(), getY(), getRight(), getBottom(), 0xA0000000);
		graphics.enableScissor(getX(), getY(), getRight(), getBottom());
		boolean overList = menu == null && isMouseOver(mouseX, mouseY);
		Component tooltip = null;
		int y = getY() + 2 - (int) scroll;
		for (Row row : rows) {
			int height = rowHeight(row);
			if (y + height > getY() && y < getBottom()) {
				boolean hovered = overList && mouseY >= y && mouseY < y + height;
				Component hint = renderRow(graphics, row, y, hovered, mouseX, mouseY);
				if (hint != null) tooltip = hint;
			}
			y += height;
		}
		graphics.disableScissor();
		if (maxScroll() > 0) {
			int barHeight = Math.max(16, height * height / contentHeight());
			int barY = getY() + (int) ((height - barHeight) * (scroll / maxScroll()));
			graphics.fill(getRight() - 3, barY, getRight() - 1, barY + barHeight, 0x80FFFFFF);
		}
		if (menu != null) Gfx.overlay(overlay -> renderMenu(overlay, mouseX, mouseY));
		if (tooltip != null) Gfx.tooltip(graphics, font, font.split(tooltip, 200), mouseX, mouseY);
	}

	private Component renderRow(GuiGraphics graphics, Row row, int y, boolean hovered, int mouseX, int mouseY) {
		int left = getX() + 4;
		if (row instanceof Header header) {
			Component text = header.toggle() == null ? header.text()
					: Component.literal(header.collapsed() ? "▶ " : "▼ ").append(header.text());
			graphics.drawString(font, text, left, y + 4, hovered && header.toggle() != null ? 0xFFFFFFA0 : 0xFFA0A0A0, false);
			return null;
		}
		if (row instanceof Note note) {
			int lineY = y + 3;
			for (FormattedCharSequence line : font.split(note.text(), textWidth())) {
				graphics.drawString(font, line, left, lineY, 0xFFC0C0C0, false);
				lineY += font.lineHeight;
			}
			return null;
		}
		Person person = (Person) row;
		if (hovered) graphics.fill(getX() + 1, y, getRight() - 1, y + PERSON_HEIGHT, 0x30FFFFFF);
		int avatarY = y + (PERSON_HEIGHT - AVATAR) / 2;
		if (person.steamId() != 0) {
			SteamPeople.drawAvatar(graphics, person.steamId(), left, avatarY, AVATAR);
		} else if (person.face() != null) {
			PlayerFaceRenderer.draw(graphics, person.face(), left, avatarY, AVATAR);
		}
		if (person.dotColor() != 0) {
			graphics.fill(left + AVATAR - 3, avatarY + AVATAR - 3, left + AVATAR + 1, avatarY + AVATAR + 1, 0xFF000000);
			graphics.fill(left + AVATAR - 2, avatarY + AVATAR - 2, left + AVATAR, avatarY + AVATAR, person.dotColor());
		}
		int textLeft = left + AVATAR + 5;
		int textRight = getRight() - 6;
		Component tooltip = null;
		Action action = person.action();
		if (action != null) {
			int buttonWidth = buttonWidth(action);
			int buttonX = textRight - buttonWidth;
			int buttonY = y + (PERSON_HEIGHT - BUTTON_HEIGHT) / 2;
			boolean overButton = hovered && mouseX >= buttonX && mouseX < buttonX + buttonWidth && mouseY >= buttonY && mouseY < buttonY + BUTTON_HEIGHT;
			Gfx.sprite(graphics, !action.active() ? BUTTON_DISABLED : overButton ? BUTTON_HIGHLIGHTED : BUTTON, buttonX, buttonY, buttonWidth, BUTTON_HEIGHT);
			graphics.drawCenteredString(font, action.label(), buttonX + buttonWidth / 2, buttonY + 3, action.active() ? 0xFFFFFFFF : 0xFFA0A0A0);
			if (overButton) tooltip = action.tooltip();
			textRight = buttonX - 4;
		}
		int maxText = textRight - textLeft;
		graphics.drawString(font, ellipsize(person.name(), maxText), textLeft, y + 3, 0xFFFFFFFF, false);
		graphics.drawString(font, ellipsize(person.detail(), maxText), textLeft, y + 13, 0xFF9A9A9A, false);
		if (tooltip == null && hovered) {
			if (person.tooltip() != null) {
				tooltip = person.name().copy().append("\n").append(person.tooltip());
			} else if (font.width(person.name()) > maxText || font.width(person.detail()) > maxText) {
				tooltip = person.name().copy().append("\n").append(person.detail());
			}
		}
		return tooltip;
	}

	private int buttonWidth(Action action) {
		return font.width(action.label()) + 10;
	}

	private FormattedCharSequence ellipsize(Component text, int maxWidth) {
		return Gfx.fit(font, text, maxWidth);
	}

	private void renderMenu(GuiGraphics graphics, int mouseX, int mouseY) {
		int x = menu.x();
		int y = menu.y();
		graphics.fill(x, y, x + menu.width(), y + menu.height(), 0xFF505050);
		graphics.fill(x + 1, y + 1, x + menu.width() - 1, y + menu.height() - 1, 0xF0101010);
		int itemY = y + 2;
		for (MenuItem item : menu.items()) {
			if (mouseX >= x && mouseX < x + menu.width() && mouseY >= itemY && mouseY < itemY + MENU_ITEM_HEIGHT) {
				graphics.fill(x + 1, itemY, x + menu.width() - 1, itemY + MENU_ITEM_HEIGHT, 0xFF2F4F7F);
			}
			graphics.drawString(font, item.label(), x + 5, itemY + 3, 0xFFFFFFFF, false);
			itemY += MENU_ITEM_HEIGHT;
		}
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		return menu != null && menu.contains(mouseX, mouseY) || super.isMouseOver(mouseX, mouseY);
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
		if (menu != null) {
			OpenMenu open = menu;
			menu = null;
			if (!open.contains(mouseX, mouseY)) return super.isMouseOver(mouseX, mouseY);
			int index = (int) ((mouseY - open.y() - 2) / MENU_ITEM_HEIGHT);
			if (index >= 0 && index < open.items().size()) {
				click();
				open.items().get(index).onPress().run();
				refresh();
			}
			return true;
		}
		if (!active || !visible || !super.isMouseOver(mouseX, mouseY)) return false;
		int y = getY() + 2 - (int) scroll;
		for (Row row : rows) {
			int height = rowHeight(row);
			if (mouseY >= y && mouseY < y + height) return clickRow(row, y, mouseX, mouseY, button);
			y += height;
		}
		return true;
	}

	private boolean clickRow(Row row, int y, double mouseX, double mouseY, int button) {
		if (row instanceof Header header && button == InputConstants.MOUSE_BUTTON_LEFT && header.toggle() != null) {
			click();
			header.toggle().run();
			refresh();
			return true;
		}
		if (!(row instanceof Person person)) return true;
		if (button == InputConstants.MOUSE_BUTTON_RIGHT && !person.menu().isEmpty()) {
			openMenu(person.menu(), (int) mouseX, (int) mouseY);
			return true;
		}
		Action action = person.action();
		if (button != InputConstants.MOUSE_BUTTON_LEFT || action == null || !action.active()) return true;
		int buttonWidth = buttonWidth(action);
		int buttonX = getRight() - 6 - buttonWidth;
		int buttonY = y + (PERSON_HEIGHT - BUTTON_HEIGHT) / 2;
		if (mouseX >= buttonX && mouseX < buttonX + buttonWidth && mouseY >= buttonY && mouseY < buttonY + BUTTON_HEIGHT) {
			click();
			action.onPress().run();
			refresh();
		}
		return true;
	}

	private void openMenu(List<MenuItem> items, int mouseX, int mouseY) {
		int width = 0;
		for (MenuItem item : items) width = Math.max(width, font.width(item.label()));
		width += 10;
		OpenMenu sized = new OpenMenu(0, 0, width, items);
		int x = Mth.clamp(mouseX, getX(), Math.max(getX(), getRight() - width));
		int y = Mth.clamp(mouseY, getY(), Math.max(getY(), getBottom() - sized.height()));
		menu = new OpenMenu(x, y, width, items);
	}

	private static void click() {
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (!super.isMouseOver(mouseX, mouseY)) return false;
		menu = null;
		scroll = Mth.clamp(scroll - scrollY * PERSON_HEIGHT, 0, maxScroll());
		return true;
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		output.add(NarratedElementType.TITLE, getMessage());
	}
}
