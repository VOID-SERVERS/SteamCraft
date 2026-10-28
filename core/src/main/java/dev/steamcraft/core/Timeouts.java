package dev.steamcraft.core;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class Timeouts {
	private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread thread = new Thread(r, "steamcraft-timeouts");
		thread.setDaemon(true);
		return thread;
	});

	private Timeouts() {
	}

	public static <T> CompletableFuture<T> completeOnTimeout(CompletableFuture<T> future, long delay, TimeUnit unit, Supplier<T> fallback) {
		if (!future.isDone()) {
			SCHEDULER.schedule(() -> {
				if (!future.isDone()) future.complete(fallback.get());
			}, delay, unit);
		}
		return future;
	}

	public static void schedule(Runnable task, long delay, TimeUnit unit) {
		SCHEDULER.schedule(task, delay, unit);
	}
}
