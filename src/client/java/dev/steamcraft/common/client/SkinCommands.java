package dev.steamcraft.common.client;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.steamcraft.core.config.SkinModel;
import dev.steamcraft.core.config.SkinSource;
import dev.steamcraft.core.config.SteamCraftConfig;
import dev.steamcraft.common.SteamRuntime;
import dev.steamcraft.common.Platform;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

final class SkinCommands<S> {
	private final CommandFeedback<S> feedback;

	SkinCommands(CommandFeedback<S> feedback) {
		this.feedback = feedback;
	}

	LiteralArgumentBuilder<S> skin() {
		return literal("skin")
				.executes(this::show)
				.then(literal("mojang").executes(c -> setSkin(c, SkinSource.MOJANG, "Your skin now comes from your Minecraft account.")))
				.then(literal("file").then(argument("path", StringArgumentType.greedyString())
						.suggests((c, b) -> pngFiles(b, true))
						.executes(c -> pickFile(c, true))))
				.then(literal("model").then(argument("model", StringArgumentType.word())
						.suggests((c, b) -> SharedSuggestionProvider.suggest(new String[]{"classic", "slim"}, b))
						.executes(this::model)))
				.then(literal("reset").executes(c -> setSkin(c, SkinSource.NONE, "Skin reset: others see the default skin.")));
	}

	LiteralArgumentBuilder<S> cape() {
		return literal("cape")
				.executes(this::show)
				.then(literal("mojang").executes(c -> setCape(c, SkinSource.MOJANG, "Your cape now comes from your Minecraft account.")))
				.then(literal("file").then(argument("path", StringArgumentType.greedyString())
						.suggests((c, b) -> pngFiles(b, false))
						.executes(c -> pickFile(c, false))))
				.then(literal("reset").executes(c -> setCape(c, SkinSource.NONE, "Cape removed.")));
	}

	private int show(CommandContext<S> c) {
		SteamCraftConfig.Skin config = SteamRuntime.config().skin;
		String skin;
		switch (config.source) {
			case MOJANG:
				skin = "from your Minecraft account";
				break;
			case FILE:
				skin = "from a file, " + armsName(config.model) + " arms";
				break;
			default:
				skin = "default";
		}
		String cape = config.cape == SkinSource.MOJANG ? "from your Minecraft account" : config.cape == SkinSource.FILE ? "from a file" : "none";
		feedback.info(c.getSource(), Component.literal("Skin: " + skin + ". Cape: " + cape + "."));
		feedback.info(c.getSource(), Component.literal("Others see them on Steam-mode servers and in co-op worlds.").withStyle(ChatFormatting.GRAY));
		return 1;
	}

	private int setSkin(CommandContext<S> c, SkinSource source, String message) {
		SkinChoice.setSkinSource(source);
		return saveAndUpload(c, message, false);
	}

	private int setCape(CommandContext<S> c, SkinSource source, String message) {
		SkinChoice.setCapeSource(source);
		return saveAndUpload(c, message, true);
	}

	private int pickFile(CommandContext<S> c, boolean skin) {
		String typed = StringArgumentType.getString(c, "path");
		Path file = resolve(typed);
		if (!Files.isRegularFile(file)) return fail(c, "No file at " + file + ".");
		String name = file.getFileName().toString();
		try {
			if (!skin) {
				SkinChoice.useCapeFile(file);
				return saveAndUpload(c, "Cape set from " + name + ".", true);
			}
			SkinModel model = SkinChoice.useSkinFile(file);
			String other = model == SkinModel.SLIM ? "classic" : "slim";
			return saveAndUpload(c, "Skin set from " + name + " with " + armsName(model)
					+ " arms. If the arms look wrong, use /steam skin model " + other + ".", false);
		} catch (IllegalArgumentException e) {
			return fail(c, e.getMessage());
		} catch (IOException e) {
			return fail(c, "Could not read or copy " + file + ": " + e.getMessage());
		}
	}

	private int model(CommandContext<S> c) {
		String typed = StringArgumentType.getString(c, "model").toLowerCase(Locale.ROOT);
		SkinModel model;
		if (typed.equals("classic")) model = SkinModel.CLASSIC;
		else if (typed.equals("slim")) model = SkinModel.SLIM;
		else return fail(c, "Unknown arm width. Use classic or slim.");
		SkinChoice.setModel(model);
		if (SkinChoice.current().source != SkinSource.FILE) {
			SteamRuntime.saveConfig();
			feedback.info(c.getSource(), Component.literal("Arms set to " + armsName(model)
					+ ". This applies to a skin from a file; a Minecraft account skin brings its own."));
			return 1;
		}
		return saveAndUpload(c, "Arms set to " + armsName(model) + ".", false);
	}

	private int saveAndUpload(CommandContext<S> c, String message, boolean capeChanged) {
		S source = c.getSource();
		feedback.info(source, Component.literal(message).withStyle(ChatFormatting.GREEN));
		SteamCraftConfig.Skin config = SkinChoice.current();
		SkinChoice.saveAndUpload(sent -> {
			if (!capeChanged && config.source == SkinSource.MOJANG && sent.skin == null) {
				feedback.error(source, Component.literal("Your Minecraft account has no skin to share: the launcher is not signed in with a"
						+ " licensed account, or Mojang could not be reached. Others see the default skin."));
			}
			if (capeChanged && config.cape == SkinSource.MOJANG && sent.cape == null) {
				feedback.error(source, Component.literal("Your Minecraft account has no cape to share."));
			}
		});
		return 1;
	}

	private static String armsName(SkinModel model) {
		return model == SkinModel.SLIM ? "slim" : "classic";
	}

	private static Path resolve(String typed) {
		String path = typed.trim();
		if (path.length() >= 2 && path.startsWith("\"") && path.endsWith("\"")) path = path.substring(1, path.length() - 1);
		if (path.equals("~") || path.startsWith("~/")) path = System.getProperty("user.home") + path.substring(1);
		Path file = Paths.get(path);
		return file.isAbsolute() ? file : Platform.get().gameDir().resolve(file).normalize();
	}

	private static CompletableFuture<Suggestions> pngFiles(SuggestionsBuilder builder, boolean skin) {
		String dir = skin ? SkinChoice.SKINS_DIR : SkinChoice.CAPES_DIR;
		return SharedSuggestionProvider.suggest(SkinChoice.folderPngs(skin).stream().map(p -> dir + "/" + p.getFileName()), builder);
	}

	private LiteralArgumentBuilder<S> literal(String name) {
		return LiteralArgumentBuilder.literal(name);
	}

	private <T> RequiredArgumentBuilder<S, T> argument(String name, ArgumentType<T> type) {
		return RequiredArgumentBuilder.argument(name, type);
	}

	private int fail(CommandContext<S> c, String message) {
		feedback.error(c.getSource(), Component.literal(message));
		return 0;
	}
}
