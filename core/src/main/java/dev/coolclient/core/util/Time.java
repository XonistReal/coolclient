package dev.coolclient.core.util;

/** Monotonic clock helpers. */
public final class Time {
    private static final long START = System.nanoTime();
    private static long lastFrame = System.nanoTime();
    private static float frameDelta = 0.016f;
    private static long frameIndex;

    private Time() {}

    public static long millis() { return (System.nanoTime() - START) / 1_000_000L; }

    public static double seconds() { return (System.nanoTime() - START) / 1_000_000_000.0; }

    /** Called once per rendered frame by the core. */
    public static void frame() {
        long now = System.nanoTime();
        frameDelta = Math.min(0.25f, (now - lastFrame) / 1_000_000_000f);
        lastFrame = now;
        frameIndex++;
    }

    /** Seconds since the previous rendered frame (capped at 0.25s). */
    public static float delta() { return frameDelta; }

    /** Increments once per rendered frame. */
    public static long frameIndex() { return frameIndex; }
}
