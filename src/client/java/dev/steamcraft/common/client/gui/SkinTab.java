package dev.steamcraft.common.client.gui;

import dev.steamcraft.common.SteamRuntime;
import dev.steamcraft.common.client.SkinChoice;
import dev.steamcraft.common.client.Skins;
import dev.steamcraft.core.config.SkinModel;
import dev.steamcraft.core.config.SkinSource;
import dev.steamcraft.core.identity.PlayerUuids;
import dev.steamcraft.core.skin.SkinImages;
import dev.steamcraft.core.steam.SteamClient;
import net.minecraft.ChatFormatting;
//? if >=26.3 {
/*import com.mojang.blaze3d.Blaze3D;
*///?} else {
import net.minecraft.Util;
//?}
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.PlayerSkinWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.tabs.Tab;
//? if >=26.2 {
/*import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.Layout;
*///?}
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

final class SkinTab implements Tab {
	//? if >=26.2
	/*private static final Layout EMPTY_LAYOUT = new GridLayout();*/
	private static final ResourceLocation PREVIEW_SKIN = ResourceLocation.fromNamespaceAndPath("steamcraft", "preview/skin");
	private static final ResourceLocation PREVIEW_CAPE = ResourceLocation.fromNamespaceAndPath("steamcraft", "preview/cape");
	private static final int PREVIEW_WIDTH = 110;
	private static final int MAX_WIDTH = 380;

	private final Minecraft minecraft = Minecraft.getInstance();
	private final PlayerSkinWidget model;
	private final CapePreview cape;
	private final CycleButton<SkinSource> skinSource;
	private final Dropdown<Path> skinFile;
	private final CycleButton<SkinModel> arms;
	private final CycleButton<SkinSource> capeSource;
	private final Dropdown<Path> capeFile;
	private final Button openSkins;
	private final Button openCapes;
	private final MultiLineTextWidget status;
	private final MultiLineTextWidget hint;
	private Supplier<PlayerSkin> preview;

	SkinTab() {
		model = new PlayerSkinWidget(PREVIEW_WIDTH, 150, minecraft.getEntityModels(), () -> preview.get());
		cape = new CapePreview();
		skinSource = Gfx.<SkinSource>cycle(source -> Component.translatable("steamcraft.skin.source." + id(source)), SkinChoice.current().source)
				.withValues(SkinSource.MOJANG, SkinSource.FILE, SkinSource.NONE)
				.create(0, 0, 200, 20, Component.translatable("steamcraft.skin.skin"), (b, value) -> skinSourceChanged(value));
		skinFile = new Dropdown<>(minecraft.font, Component.translatable("steamcraft.skin.file"), () -> filesForList(true),
				file -> Component.literal(file.getFileName().toString()), file -> pickFile(file, true))
				.icon((graphics, file, x, y, size) -> SkinThumbnails.drawFace(graphics, file, x, y, size));
		arms = Gfx.<SkinModel>cycle(value -> Component.translatable("steamcraft.skin.arms." + id(value)), SkinChoice.current().model)
				.withValues(SkinModel.CLASSIC, SkinModel.SLIM)
				.create(0, 0, 200, 20, Component.translatable("steamcraft.skin.arms"), (b, value) -> armsChanged(value));
		capeSource = Gfx.<SkinSource>cycle(source -> Component.translatable("steamcraft.skin.cape_source." + id(source)), SkinChoice.current().cape)
				.withValues(SkinSource.MOJANG, SkinSource.FILE, SkinSource.NONE)
				.create(0, 0, 200, 20, Component.translatable("steamcraft.skin.cape"), (b, value) -> capeSourceChanged(value));
		capeFile = new Dropdown<>(minecraft.font, Component.translatable("steamcraft.skin.cape_file"), () -> filesForList(false),
				file -> Component.literal(file.getFileName().toString()), file -> pickFile(file, false))
				.icon((graphics, file, x, y, size) -> SkinThumbnails.drawCape(graphics, file, x + 2, y, size));
		openSkins = Button.builder(Component.translatable("steamcraft.skin.open_skins"), b -> openFolder(true)).build();
		openCapes = Button.builder(Component.translatable("steamcraft.skin.open_capes"), b -> openFolder(false)).build();
		status = new MultiLineTextWidget(CommonComponents.EMPTY, minecraft.font);
		hint = new MultiLineTextWidget(Component.translatable("steamcraft.skin.drop_hint").withStyle(ChatFormatting.GRAY), minecraft.font);
		update();
	}

	private static String id(Enum<?> value) {
		return value.name().toLowerCase(Locale.ROOT);
	}

	@Override
	public Component getTabTitle() {
		return Component.translatable("steamcraft.tab.skin");
	}

	//? if >=1.21.6 {
	/*@Override
	public Component getTabExtraNarration() {
		return Component.empty();
	}
	*///?}

	//? if >=26.2 {
	/*@Override
	public Layout getLayout() {
		return EMPTY_LAYOUT;
	}
	*///?}

	@Override
	public void visitChildren(Consumer<AbstractWidget> visitor) {
		visitor.accept(skinFile);
		visitor.accept(capeFile);
		visitor.accept(model);
		visitor.accept(cape);
		visitor.accept(skinSource);
		visitor.accept(arms);
		visitor.accept(capeSource);
		visitor.accept(openSkins);
		visitor.accept(openCapes);
		visitor.accept(status);
		visitor.accept(hint);
	}

	@Override
	public void doLayout(ScreenRectangle area) {
		int width = Math.min(MAX_WIDTH, area.width() - 20);
		int left = area.left() + (area.width() - width) / 2;
		int top = area.top() + 10;
		int modelHeight = Math.min(150, area.height() - 20);
		model.setRectangle(PREVIEW_WIDTH - 24, modelHeight, left, top);
		cape.setRectangle(20, 32, left + PREVIEW_WIDTH - 22, top + modelHeight - 32);

		int x = left + PREVIEW_WIDTH + 8;
		int controls = width - PREVIEW_WIDTH - 8;
		int y = top;
		for (AbstractWidget widget : new AbstractWidget[]{skinSource, skinFile, arms, capeSource, capeFile}) {
			widget.setRectangle(controls, 20, x, y);
			y += 24;
		}
		int half = (controls - 4) / 2;
		openSkins.setRectangle(half, 20, x, y);
		openCapes.setRectangle(controls - half - 4, 20, x + half + 4, y);
		y += 24;
		skinFile.setLimits(area.top(), area.bottom());
		capeFile.setLimits(area.top(), area.bottom());
		status.setMaxWidth(controls);
		status.setPosition(x, y + 2);
		hint.setMaxWidth(controls);
		hint.setPosition(x, area.bottom() - 4 - 2 * minecraft.font.lineHeight);
	}

	private void skinSourceChanged(SkinSource source) {
		if (source == SkinSource.FILE && !Files.isRegularFile(SkinChoice.copyOf(true))) {
			List<Path> files = SkinChoice.folderPngs(true);
			if (files.isEmpty()) {
				skinSource.setValue(SkinChoice.current().source);
				error(Component.translatable("steamcraft.skin.no_skins"));
				return;
			}
			if (!useFile(files.get(0), true)) {
				skinSource.setValue(SkinChoice.current().source);
				return;
			}
		} else {
			SkinChoice.setSkinSource(source);
		}
		saveAndUpload(false);
	}

	private void capeSourceChanged(SkinSource source) {
		if (source == SkinSource.FILE && !Files.isRegularFile(SkinChoice.copyOf(false))) {
			List<Path> files = SkinChoice.folderPngs(false);
			if (files.isEmpty()) {
				capeSource.setValue(SkinChoice.current().cape);
				error(Component.translatable("steamcraft.skin.no_capes"));
				return;
			}
			if (!useFile(files.get(0), false)) {
				capeSource.setValue(SkinChoice.current().cape);
				return;
			}
		} else {
			SkinChoice.setCapeSource(source);
		}
		saveAndUpload(true);
	}

	private void armsChanged(SkinModel value) {
		SkinChoice.setModel(value);
		saveAndUpload(false);
	}

	private static List<Path> filesForList(boolean skin) {
		List<Path> files = SkinChoice.folderPngs(skin);
		SkinThumbnails.prepare(files);
		return files;
	}

	private void pickFile(Path file, boolean skin) {
		if (useFile(file, skin)) saveAndUpload(!skin);
	}

	private boolean useFile(Path file, boolean skin) {
		try {
			if (skin) {
				SkinChoice.useSkinFile(file);
			} else {
				SkinChoice.useCapeFile(file);
			}
			return true;
		} catch (IllegalArgumentException e) {
			error(Component.translatable("steamcraft.skin.bad_file", file.getFileName().toString(), e.getMessage()));
		} catch (IOException e) {
			error(Component.translatable("steamcraft.skin.bad_file", file.getFileName().toString(), e.getMessage()));
		}
		return false;
	}

	void filesDropped(List<Path> files, boolean skin) {
		Path dir = SkinChoice.dir(skin);
		Path first = null;
		Component problem = null;
		for (Path file : files) {
			String name = file.getFileName().toString();
			if (!name.toLowerCase(Locale.ROOT).endsWith(".png") || !Files.isRegularFile(file)) {
				if (problem == null) problem = Component.translatable("steamcraft.skin.not_png");
				continue;
			}
			try {
				byte[] png = Files.readAllBytes(file);
				if (skin) {
					SkinImages.skin(png, SkinImages.MAX_FILE_BYTES);
				} else {
					SkinImages.cape(png, SkinImages.MAX_FILE_BYTES);
				}
				Files.createDirectories(dir);
				Path copy = dir.resolve(name);
				if (!copy.toAbsolutePath().equals(file.toAbsolutePath())) Files.copy(file, copy, StandardCopyOption.REPLACE_EXISTING);
				if (first == null) first = copy;
			} catch (IllegalArgumentException e) {
				if (problem == null) problem = Component.translatable("steamcraft.skin.bad_file", name, e.getMessage());
			} catch (IOException e) {
				if (problem == null) problem = Component.translatable("steamcraft.skin.copy_failed", e.getMessage());
			}
		}
		if (first != null && useFile(first, skin)) saveAndUpload(!skin);
		if (problem != null) error(problem);
	}

	private void openFolder(boolean skin) {
		Path dir = SkinChoice.dir(skin);
		try {
			Files.createDirectories(dir);
		} catch (IOException e) {
			error(Component.translatable("steamcraft.skin.copy_failed", e.getMessage()));
			return;
		}
		//? if >=26.3 {
		/*Blaze3D.openPath(dir);
		*///?} else {
		Util.getPlatform().openPath(dir);
		//?}
	}

	private void saveAndUpload(boolean capeChanged) {
		status.setMessage(Component.translatable("steamcraft.skin.saved").withStyle(ChatFormatting.GREEN));
		SkinChoice.saveAndUpload(sent -> {
			if (!capeChanged && SkinChoice.current().source == SkinSource.MOJANG && sent.skin == null) {
				error(Component.translatable("steamcraft.skin.no_account_skin"));
			} else if (capeChanged && SkinChoice.current().cape == SkinSource.MOJANG && sent.cape == null) {
				error(Component.translatable("steamcraft.skin.no_account_cape"));
			}
		});
		update();
	}

	private void error(Component message) {
		status.setMessage(message.copy().withStyle(ChatFormatting.RED));
	}

	private void update() {
		SkinSource source = SkinChoice.current().source;
		SkinSource capeChoice = SkinChoice.current().cape;
		skinSource.setValue(source);
		capeSource.setValue(capeChoice);
		arms.setValue(SkinChoice.current().model);
		arms.active = source == SkinSource.FILE;
		arms.setTooltip(source == SkinSource.FILE ? null : Tooltip.create(Component.translatable("steamcraft.skin.arms.only_file")));
		skinFile.active = source == SkinSource.FILE;
		showFile(skinFile, SkinChoice.current().file, true);
		capeFile.active = capeChoice == SkinSource.FILE;
		showFile(capeFile, SkinChoice.current().capeFile, false);
		boolean account = SkinChoice.hasAccount();
		skinSource.setTooltip(account ? null : Tooltip.create(Component.translatable("steamcraft.skin.no_account")));
		capeSource.setTooltip(account ? null : Tooltip.create(Component.translatable("steamcraft.skin.no_account")));
		updatePreview();
	}

	private static void showFile(Dropdown<Path> dropdown, String name, boolean skin) {
		Path file = name != null ? SkinChoice.dir(skin).resolve(name) : null;
		if (file != null && Files.isRegularFile(file)) {
			SkinThumbnails.prepare(List.of(file));
			dropdown.setSelected(file, CommonComponents.EMPTY);
		} else {
			dropdown.setSelected(null, Component.translatable(name != null ? "steamcraft.skin.file.current" : "steamcraft.skin.file.none"));
		}
	}

	private void updatePreview() {
		SkinSource source = SkinChoice.current().source;
		Supplier<PlayerSkin> account = Skins.account(minecraft);
		PlayerSkin fallback = defaultSkin();
		ResourceLocation fileSkin = source == SkinSource.FILE ? SkinThumbnails.loadPreview(SkinChoice.copyOf(true), PREVIEW_SKIN) : null;
		ResourceLocation fileCape = SkinChoice.current().cape == SkinSource.FILE ? SkinThumbnails.loadPreview(SkinChoice.copyOf(false), PREVIEW_CAPE) : null;
		boolean slim = SkinChoice.current().model == SkinModel.SLIM;
		preview = () -> {
			PlayerSkin base = source == SkinSource.MOJANG && SkinChoice.hasAccount() ? account.get() : fallback;
			ResourceLocation texture = fileSkin != null ? fileSkin : Skins.body(base);
			boolean arms = fileSkin != null ? slim : Skins.slim(base);
			return Skins.of(texture, Skins.cape(base), Skins.elytra(base), arms);
		};
		SkinSource capeChoice = SkinChoice.current().cape;
		cape.texture = () -> capeChoice == SkinSource.FILE ? fileCape
				: capeChoice == SkinSource.MOJANG ? Skins.cape(account.get()) : null;
	}

	static PlayerSkin defaultSkin() {
		Minecraft minecraft = Minecraft.getInstance();
		SteamClient steam = SteamRuntime.client();
		return steam != null ? DefaultPlayerSkin.get(PlayerUuids.fromSteam(steam.localSteamId())) : DefaultPlayerSkin.get(minecraft.getGameProfile());
	}
}
