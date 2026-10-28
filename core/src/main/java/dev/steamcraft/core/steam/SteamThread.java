package dev.steamcraft.core.steam;

import dev.steamcraft.core.Log;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.LockSupport;

public final class SteamThread implements Executor {
	private static final long IDLE_NANOS = TimeUnit.MILLISECONDS.toNanos(1);

	private final ConcurrentLinkedQueue<Runnable> tasks = new ConcurrentLinkedQueue<>();
	private final List<Runnable> tickers = new CopyOnWriteArrayList<>();
	private final Thread thread;
	private volatile boolean running = true;
	private volatile Runnable shutdownHook;

	public SteamThread(String name) {
		this.thread = new Thread(this::loop, name);
		this.thread.setDaemon(true);
		this.thread.start();
	}

	@Override
	public void execute(Runnable task) {
		tasks.add(task);
		LockSupport.unpark(thread);
	}

	public <T> CompletableFuture<T> submit(Callable<T> task) {
		CompletableFuture<T> future = new CompletableFuture<>();
		execute(() -> {
			try {
				future.complete(task.call());
			} catch (Throwable e) {
				future.completeExceptionally(e);
			}
		});
		return future;
	}

	public <T> T call(Callable<T> task) {
		try {
			if (isCurrent()) return task.call();
			return submit(task).get(5, TimeUnit.SECONDS);
		} catch (ExecutionException e) {
			throw new IllegalStateException(e.getCause());
		} catch (TimeoutException e) {
			throw new IllegalStateException("Steam thread did not respond", e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	public void addTicker(Runnable ticker) {
		tickers.add(ticker);
	}

	public boolean isCurrent() {
		return Thread.currentThread() == thread;
	}

	public void stop(Runnable hook) {
		shutdownHook = hook;
		running = false;
		LockSupport.unpark(thread);
		if (!isCurrent()) {
			try {
				thread.join(3000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	private void loop() {
		while (running) {
			Runnable task;
			while ((task = tasks.poll()) != null) run(task);
			for (Runnable ticker : tickers) run(ticker);
			if (tasks.isEmpty()) LockSupport.parkNanos(IDLE_NANOS);
		}
		Runnable task;
		while ((task = tasks.poll()) != null) run(task);
		if (shutdownHook != null) run(shutdownHook);
	}

	private static void run(Runnable task) {
		try {
			task.run();
		} catch (Throwable e) {
			Log.warn("Steam thread task failed", e);
		}
	}
}
