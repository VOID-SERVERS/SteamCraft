package dev.steamcraft.common.client.gui;

import dev.steamcraft.common.SteamCraftServer;
import dev.steamcraft.common.SteamRuntime;
import dev.steamcraft.common.client.CoopHosting;
import dev.steamcraft.core.access.Invites;
import dev.steamcraft.core.config.CoopAccess;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;

import java.util.List;
import java.util.Locale;

public final class HostSettingsScreen extends Screen {
	private static final int TEXT_WIDTH = 310;

	private final Screen parent;
	private final boolean change;
	private CoopAccess access;
	private boolean cheats;
	private GameType guestMode;
	private Difficulty difficulty;
	private Component error;

	private HostSettingsScreen(Screen parent, boolean change, CoopAccess access, boolean cheats, GameType guestMode, Difficulty difficulty) {
		super(Component.translatable(change ? "steamcraft.host.title.change" : "steamcraft.host.title"));
		this.parent = parent;
		this.change = change;
		this.access = access;
		this.cheats = cheats;
		this.guestMode = guestMode;
		this.difficulty = difficulty;
	}

	public static HostSettingsScreen forNew(Screen parent) {
		return new HostSettingsScreen(parent, false, SteamRuntime.config().coop.access, false, CoopHosting.guestMode(), worldDifficulty());
	}

	public static HostSettingsScreen forCurrent(Screen parent) {
		SteamCraftServer host = CoopHosting.hostedWorld();
		if (host == null) return forNew(parent);
		return new HostSettingsScreen(parent, true, host.access(), host.cheats(), CoopHosting.guestMode(), worldDifficulty());
	}

	private static Difficulty worldDifficulty() {
		IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
		return server != null ? server.getWorldData().getDifficulty() : Difficulty.NORMAL;
	}

	@Override
	protected void init() {
		int left = width / 2 - 155;
		int right = width / 2 + 5;
		int top = 70;
		addRenderableWidget(Gfx.<CoopAccess>cycle(FriendRows::access, access)
				.withValues(CoopAccess.values())
				.create(left, top, 150, 20, Component.translatable("steamcraft.host.access"), (b, value) -> access = value));
		addRenderableWidget(CycleButton.onOffBuilder(cheats)
				.create(right, top, 150, 20, Component.translatable("steamcraft.host.cheats"), (b, value) -> cheats = value));
		addRenderableWidget(Gfx.<GameType>cycle(GameType::getShortDisplayName, guestMode)
				.withValues(GameType.SURVIVAL, GameType.SPECTATOR, GameType.CREATIVE, GameType.ADVENTURE)
				.create(left, top + 24, 150, 20, Component.translatable("steamcraft.host.guest_mode"), (b, value) -> guestMode = value));
		CycleButton<Difficulty> difficultyButton = addRenderableWidget(Gfx.<Difficulty>cycle(Difficulty::getDisplayName, difficulty)
				.withValues(Difficulty.values())
				.create(right, top + 24, 150, 20, Component.translatable("options.difficulty"), (b, value) -> difficulty = value));
		IntegratedServer server = minecraft.getSingleplayerServer();
		if (server != null && server.getWorldData().isDifficultyLocked()) {
			difficultyButton.active = false;
			difficultyButton.setTooltip(Tooltip.create(Component.translatable("steamcraft.host.difficulty_locked")));
		}

		addRenderableWidget(Button.builder(Component.translatable(change ? "steamcraft.host.save" : "steamcraft.host.open"), b -> confirm())
				.bounds(left, height - 28, 150, 20).build());
		addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose()).bounds(right, height - 28, 150, 20).build());
	}

	private void confirm() {
		IntegratedServer server = minecraft.getSingleplayerServer();
		boolean locked = server != null && server.getWorldData().isDifficultyLocked();
		try {
			CoopHosting.open(access, cheats, guestMode, locked ? null : difficulty);
		} catch (CoopHosting.Failure e) {
			error = Component.translatable("steamcraft.host.failed." + e.problem.name().toLowerCase(Locale.ROOT));
			return;
		}
		minecraft.setScreen(parent);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		graphics.drawCenteredString(font, title, width / 2, 40, 0xFFFFFF);
		int y = 128;
		y = paragraph(graphics, Component.translatable("steamcraft.host.about_access." + access.id(), Invites.VALID_MINUTES), y, 0xFFC0C0C0);
		y = paragraph(graphics, Component.translatable(cheats ? "steamcraft.host.about_cheats.on" : "steamcraft.host.about_cheats.off"), y + 4, 0xFFC0C0C0);
		y = paragraph(graphics, Component.translatable("steamcraft.host.about_ops"), y + 4, 0xFF909090);
		if (error != null) paragraph(graphics, error.copy().withStyle(ChatFormatting.RED), y + 8, 0xFFFF5555);
	}

	private int paragraph(GuiGraphics graphics, Component text, int y, int color) {
		List<FormattedCharSequence> lines = font.split(text, TEXT_WIDTH);
		for (FormattedCharSequence line : lines) {
			graphics.drawCenteredString(font, line, width / 2, y, color);
			y += font.lineHeight + 1;
		}
		return y;
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}
}
