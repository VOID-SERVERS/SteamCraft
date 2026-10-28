package dev.steamcraft.common.client;

import net.minecraft.network.chat.Component;

public interface CommandFeedback<S> {
	void info(S source, Component message);

	void error(S source, Component message);
}
