package dev.steamcraft.core;

public final class Log {
	public interface Sink {
		void info(String message);

		void warn(String message, Throwable error);

		void debug(String message);
	}

	private static volatile Sink sink = new Sink() {
		@Override
		public void info(String message) {
			System.err.println("[SteamCraft] " + message);
		}

		@Override
		public void warn(String message, Throwable error) {
			System.err.println("[SteamCraft] WARN " + message);
			if (error != null) error.printStackTrace();
		}

		@Override
		public void debug(String message) {
		}
	};

	private Log() {
	}

	public static void setSink(Sink newSink) {
		sink = newSink;
	}

	public static void info(String message) {
		sink.info(message);
	}

	public static void warn(String message) {
		sink.warn(message, null);
	}

	public static void warn(String message, Throwable error) {
		sink.warn(message, error);
	}

	public static void debug(String message) {
		sink.debug(message);
	}
}
