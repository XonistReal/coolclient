package dev.coolclient.core.util;

/** Logging facade. The platform swaps the sink for its own logger (log4j/slf4j). */
public final class Log {
    public interface Sink {
        void log(String level, String message, Throwable t);
    }

    private static Sink sink = new Sink() {
        @Override
        public void log(String level, String message, Throwable t) {
            System.out.println("[CoolClient/" + level + "] " + message);
            if (t != null) t.printStackTrace(System.out);
        }
    };

    private Log() {}

    public static void setSink(Sink s) { if (s != null) sink = s; }

    public static void info(String msg) { sink.log("INFO", msg, null); }
    public static void warn(String msg) { sink.log("WARN", msg, null); }
    public static void warn(String msg, Throwable t) { sink.log("WARN", msg, t); }
    public static void error(String msg, Throwable t) { sink.log("ERROR", msg, t); }
}
