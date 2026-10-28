package dev.steamcraft.common.client.gui;

import dev.steamcraft.common.client.SkinChoice;
import dev.steamcraft.common.client.Skins;
import dev.steamcraft.core.config.SkinModel;
import dev.steamcraft.core.config.SkinSource;
import dev.steamcraft.core.config.SteamCraftConfig;
import dev.steamcraft.core.skin.SkinImages;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.PlayerSkinWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class SkinPanel {
	public static final int WIDTH = 140;
	private static final int PADDING = 5;
	private static final int MODEL_HEIGHT = 96;
	private static final ResourceLocation PREVIEW_SKIN = ResourceLocation.fromNamespaceAndPath("steamcraft", "preview/pause_skin");
	private static final ResourceLocation PREVIEW_CAPE = ResourceLocation.fromNamespaceAndPath("steamcraft", "preview/pause_cape");

	private record Cape(SkinSource source, Path file) {
	}

	private final Minecraft minecraft = Minecraft.getInstance();
	private final Font font = minecraft.font;
	private final PlayerSkin fallback = SkinTab.defaultSkin();
	private Dropdown<Path> skins;
	private Dropdown<Cape> capes;
	private CapePreview capePreview;
	private MultiLineTextWidget error;
	private Button apply;

	private Path wornSkin;
	private Cape wornCape;
	private Path pickedSkin;
	private Cape pickedCape;
	private boolean skinOk = true;
	private boolean capeOk = true;
	private ResourceLocation skinTexture;
	private boolean slim;

	private SkinPanel() {
	}

	public static boolean isShown() {
		return SkinChoice.current().source == SkinSource.FILE;
	}

	public static int height() {
		return PADDING + Minecraft.getInstance().font.lineHeight + 4 + MODEL_HEIGHT + 6 + 24 + 24 + 20 + PADDING;
	}

	public static void addTo(ScreenRectangle area, Consumer<AbstractWidget> add) {
		new SkinPanel().build(area, add);
	}

	private void build(ScreenRectangle area, Consumer<AbstractWidget> add) {
		SteamCraftConfig.Skin config = SkinChoice.current();
		wornSkin = existing(SkinChoice.dir(true), config.file);
		wornCape = config.cape == SkinSource.FILE ? new Cape(SkinSource.FILE, existing(SkinChoice.dir(false), config.capeFile))
				: new Cape(config.cape, null);
		pickedSkin = wornSkin;
		pickedCape = wornCape;

		int x = area.left() + PADDING;
		int width = area.width() - 2 * PADDING;
		int y = area.top() + PADDING + font.lineHeight + 4;

		PlayerSkinWidget model = new PlayerSkinWidget(MODEL_HEIGHT * 3 / 5, MODEL_HEIGHT, minecraft.getEntityModels(), this::previewSkin);
		model.setPosition(x + (width - model.getWidth()) / 2, y);
		capePreview = new CapePreview();
		capePreview.setRectangle(20, 32, x + width - 22, y + (MODEL_HEIGHT - 32) / 2);
		y += MODEL_HEIGHT + 6;

		int screenHeight = minecraft.getWindow().getGuiScaledHeight();
		skins = new Dropdown<>(font, Component.translatable("steamcraft.skin.skin"), SkinPanel::skinOptions,
				file -> Component.literal(file.getFileName().toString()), this::pickSkin)
				.icon((graphics, file, iconX, iconY, size) -> SkinThumbnails.drawFace(graphics, file, iconX, iconY, size));
		skins.setRectangle(width, 20, x, y);
		skins.setLimits(0, screenHeight);
		y += 24;
		capes = new Dropdown<>(font, Component.translatable("steamcraft.skin.cape"), this::capeOptions, SkinPanel::capeLabel, this::pickCape)
				.icon((graphics, cape, iconX, iconY, size) -> {
					if (cape.file() != null) SkinThumbnails.drawCape(graphics, cape.file(), iconX + 2, iconY, size);
				});
		capes.setRectangle(width, 20, x, y);
		capes.setLimits(0, screenHeight);
		y += 24;
		apply = Button.builder(Component.translatable("steamcraft.pause.apply"), b -> apply()).bounds(x, y, width, 20).build();

		error = new MultiLineTextWidget(CommonComponents.EMPTY, font);
		error.setMaxWidth(area.width());
		error.setPosition(area.left(), area.bottom() + 4);

		add.accept(new Frame(area));
		add.accept(skins);
		add.accept(capes);
		add.accept(model);
		add.accept(capePreview);
		add.accept(error);
		add.accept(apply);
		pickSkin(pickedSkin);
		pickCape(pickedCape);
	}

	private static Path existing(Path dir, String name) {
		if (name == null) return null;
		Path file = dir.resolve(name);
		return Files.isRegularFile(file) ? file : null;
	}

	private static List<Path> skinOptions() {
		List<Path> files = SkinChoice.folderPngs(true);
		SkinThumbnails.prepare(files);
		return files;
	}

	private List<Cape> capeOptions() {
		List<Cape> options = new ArrayList<>();
		options.add(new Cape(SkinSource.NONE, null));
		if (SkinChoice.hasAccount() || wornCape.source() == SkinSource.MOJANG) options.add(new Cape(SkinSource.MOJANG, null));
		if (wornCape.source() == SkinSource.FILE && wornCape.file() == null) options.add(wornCape);
		List<Path> files = SkinChoice.folderPngs(false);
		SkinThumbnails.prepare(files);
		for (Path file : files) options.add(new Cape(SkinSource.FILE, file));
		return options;
	}

	private static Component capeLabel(Cape cape) {
		if (cape.source() == SkinSource.NONE) return Component.translatable("steamcraft.skin.cape_source.none");
		if (cape.source() == SkinSource.MOJANG) return Component.translatable("steamcraft.skin.cape_source.mojang");
		return cape.file() != null ? Component.literal(cape.file().getFileName().toString()) : Component.translatable("steamcraft.skin.file.current");
	}

	private void pickSkin(Path file) {
		pickedSkin = file;
		skins.setSelected(file, Component.translatable("steamcraft.skin.file.current"));
		Path source = file != null ? file : SkinChoice.copyOf(true);
		skinOk = false;
		try {
			byte[] png = Files.readAllBytes(source);
			skinTexture = SkinThumbnails.loadPreview(SkinImages.skin(png, SkinImages.MAX_FILE_BYTES), PREVIEW_SKIN);
			slim = file != null ? SkinImages.looksSlim(png) : SkinChoice.current().model == SkinModel.SLIM;
			skinOk = true;
		} catch (IllegalArgumentException e) {
			skinTexture = null;
			showError(Component.translatable("steamcraft.skin.bad_file", source.getFileName().toString(), e.getMessage()));
		} catch (IOException e) {
			skinTexture = null;
			showError(Component.translatable("steamcraft.skin.bad_file", source.getFileName().toString(), e.getMessage()));
		}
		updateState();
	}

	private void pickCape(Cape cape) {
		pickedCape = cape;
		capes.setSelected(cape, CommonComponents.EMPTY);
		capeOk = true;
		ResourceLocation texture = null;
		if (cape.source() == SkinSource.FILE) {
			Path source = cape.file() != null ? cape.file() : SkinChoice.copyOf(false);
			try {
				texture = SkinThumbnails.loadPreview(SkinImages.cape(Files.readAllBytes(source), SkinImages.MAX_FILE_BYTES), PREVIEW_CAPE);
			} catch (IllegalArgumentException | IOException e) {
				capeOk = false;
				showError(Component.translatable("steamcraft.skin.bad_file", source.getFileName().toString(), e.getMessage()));
			}
		} else if (cape.source() == SkinSource.MOJANG) {
			texture = Skins.cape(Skins.account(minecraft).get());
		}
		ResourceLocation shown = texture;
		capePreview.texture = () -> shown;
		updateState();
	}

	private PlayerSkin previewSkin() {
		if (skinTexture == null) return fallback;
		return Skins.of(skinTexture, null, null, slim);
	}

	private void updateState() {
		boolean changed = !Objects.equals(pickedSkin, wornSkin) || !pickedCape.equals(wornCape);
		apply.active = changed && skinOk && capeOk;
		apply.setMessage(Component.translatable(changed ? "steamcraft.pause.apply" : "steamcraft.pause.applied"));
		if (skinOk && capeOk) error.setMessage(CommonComponents.EMPTY);
	}

	private void showError(Component message) {
		error.setMessage(message.copy().withStyle(ChatFormatting.RED));
	}

	private void apply() {
		boolean capeChanged = !pickedCape.equals(wornCape);
		try {
			if (pickedSkin != null && !pickedSkin.equals(wornSkin)) SkinChoice.useSkinFile(pickedSkin);
			if (capeChanged) {
				if (pickedCape.source() == SkinSource.FILE && pickedCape.file() != null) {
					SkinChoice.useCapeFile(pickedCape.file());
				} else {
					SkinChoice.setCapeSource(pickedCape.source());
				}
			}
		} catch (IllegalArgumentException | IOException e) {
			showError(Component.literal(e.getMessage()));
			return;
		}
		wornSkin = pickedSkin;
		wornCape = pickedCape;
		SkinChoice.saveAndUpload(sent -> {
			if (capeChanged && SkinChoice.current().cape == SkinSource.MOJANG && sent.cape == null) {
				showError(Component.translatable("steamcraft.skin.no_account_cape"));
			}
		});
		updateState();
	}

	private final class Frame extends AbstractWidget {
		Frame(ScreenRectangle area) {
			super(area.left(), area.top(), area.width(), area.height(), CommonComponents.EMPTY);
			active = false;
		}

		@Override
		protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
			graphics.fill(getX(), getY(), getRight(), getBottom(), 0x80000000);
			Gfx.outline(graphics, getX(), getY(), getWidth(), getHeight(), 0x40FFFFFF);
			graphics.drawString(font, Component.translatable("steamcraft.pause.skin_title"), getX() + PADDING, getY() + PADDING, 0xFFFFFFFF);
		}

		@Override
		public boolean isMouseOver(double mouseX, double mouseY) {
			return false;
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
		}
	}
}
