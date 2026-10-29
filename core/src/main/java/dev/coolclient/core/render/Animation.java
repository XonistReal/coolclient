package dev.coolclient.core.render;

import dev.coolclient.core.util.Time;

/**
 * A value that eases towards a target over a fixed duration. Time-based, so it behaves the
 * same at any frame rate.
 */
public final class Animation {
    private final float durationMs;
    private final Easing easing;
    private float from;
    private float to;
    private long start;

    public Animation(float durationMs, Easing easing, float initial) {
        this.durationMs = durationMs;
        this.easing = easing;
        this.from = initial;
        this.to = initial;
        this.start = Time.millis() - (long) durationMs;
    }

    public Animation(float durationMs, float initial) {
        this(durationMs, Easing.OUT_CUBIC, initial);
    }

    public void target(float target) {
        if (target == to) return;
        from = get();
        to = target;
        start = Time.millis();
    }

    public void target(boolean on) { target(on ? 1f : 0f); }

    public void snap(float value) {
        from = value;
        to = value;
        start = Time.millis() - (long) durationMs;
    }

    public float target() { return to; }

    public float get() {
        float t = durationMs <= 0 ? 1f : (Time.millis() - start) / durationMs;
        if (t >= 1f) return to;
        if (t <= 0f) return from;
        return from + (to - from) * easing.apply(t);
    }

    public boolean done() { return get() == to; }
}
