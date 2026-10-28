package dev.steamcraft.common.client.mixin;

import dev.steamcraft.common.client.gui.SteamMenuButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JoinMultiplayerScreen.class)
abstract class JoinMultiplayerScreenMixin extends Screen {
	private static final int BUTTONS_HALF_WIDTH = 154;
	private static final int BUTTONS_TOP = 54;
	private static final int BUTTONS_HEIGHT = 44;
	private static final int GAP = 8;
	private static final int MIN_WIDTH = 72;
	private static final int MAX_WIDTH = 120;

	protected JoinMultiplayerScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void steamcraft$addSteamButton(CallbackInfo ci) {
		int left = width / 2 + BUTTONS_HALF_WIDTH + GAP;
		int buttonWidth = Math.min(MAX_WIDTH, width - GAP - left);
		if (buttonWidth >= MIN_WIDTH) {
			addRenderableWidget(new SteamMenuButton(this, width - GAP - buttonWidth, height - BUTTONS_TOP, buttonWidth, BUTTONS_HEIGHT));
		} else {
			addRenderableWidget(new SteamMenuButton(this, width - GAP - 90, 6, 90, 20));
		}
	}
}
