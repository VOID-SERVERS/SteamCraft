package dev.steamcraft.common.client;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

final class ChatEvents {
	private ChatEvents() {
	}

	static ClickEvent copy(String text) {
		//? if >=1.21.5 {
		/*return new ClickEvent.CopyToClipboard(text);
		*///?} else {
		return new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, text);
		//?}
	}

	static HoverEvent showText(Component text) {
		//? if >=1.21.5 {
		/*return new HoverEvent.ShowText(text);
		*///?} else {
		return new HoverEvent(HoverEvent.Action.SHOW_TEXT, text);
		//?}
	}
}
