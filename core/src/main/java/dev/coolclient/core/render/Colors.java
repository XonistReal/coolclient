package dev.coolclient.core.render;

import dev.coolclient.core.util.Time;

/** ARGB colour helpers. */
public final class Colors {
    public static final int WHITE = 0xFFFFFFFF;
    public static final int BLACK = 0xFF000000;
    public static final int TRANSPARENT = 0x00000000;

    private Colors() {}

    public static int a(int c) { return (c >>> 24) & 0xFF; }

    public static int r(int c) { return (c >> 16) & 0xFF; }

    public static int g(int c) { return (c >> 8) & 0xFF; }

    public static int b(int c) { return c & 0xFF; }

    public static int argb(int a, int r, int g, int b) {
        return (clamp(a) << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    public static int rgb(int r, int g, int b) { return argb(255, r, g, b); }

    private static int clamp(int v) { return v < 0 ? 0 : (v > 255 ? 255 : v); }

    public static int withAlpha(int c, int alpha) { return (clamp(alpha) << 24) | (c & 0x00FFFFFF); }

    /** Multiplies the existing alpha by {@code f} (0..1). */
    public static int fade(int c, float f) {
        return withAlpha(c, Math.round(a(c) * Math.max(0f, Math.min(1f, f))));
    }

    public static int lerp(int c1, int c2, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return argb(
                Math.round(a(c1) + (a(c2) - a(c1)) * t),
                Math.round(r(c1) + (r(c2) - r(c1)) * t),
                Math.round(g(c1) + (g(c2) - g(c1)) * t),
                Math.round(b(c1) + (b(c2) - b(c1)) * t));
    }

    public static int brighten(int c, float f) {
        return argb(a(c), Math.round(r(c) * f), Math.round(g(c) * f), Math.round(b(c) * f));
    }

    public static int mix(int base, int overlay, float t) {
        return withAlpha(lerp(base, overlay, t), a(base));
    }

    /** HSB → ARGB (opaque). h,s,b in 0..1. */
    public static int hsb(float h, float s, float v) {
        h = h - (float) Math.floor(h);
        float r, g, b;
        int i = (int) (h * 6f);
        float f = h * 6f - i;
        float p = v * (1f - s), q = v * (1f - f * s), t = v * (1f - (1f - f) * s);
        switch (i % 6) {
            case 0: r = v; g = t; b = p; break;
            case 1: r = q; g = v; b = p; break;
            case 2: r = p; g = v; b = t; break;
            case 3: r = p; g = q; b = v; break;
            case 4: r = t; g = p; b = v; break;
            default: r = v; g = p; b = q; break;
        }
        return rgb(Math.round(r * 255), Math.round(g * 255), Math.round(b * 255));
    }

    /** ARGB → {h, s, b}. */
    public static float[] toHsb(int c) {
        float r = r(c) / 255f, g = g(c) / 255f, b = b(c) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        float h = 0f, s = max == 0 ? 0 : (max - min) / max;
        if (max != min) {
            float d = max - min;
            if (max == r) h = (g - b) / d + (g < b ? 6 : 0);
            else if (max == g) h = (b - r) / d + 2;
            else h = (r - g) / d + 4;
            h /= 6f;
        }
        return new float[]{h, s, max};
    }

    /** Animated rainbow colour. {@code offset} shifts the phase (e.g. by x position). */
    public static int chroma(double speed, float offset) {
        double t = Time.seconds() * 0.25 * speed + offset;
        return hsb((float) (t - Math.floor(t)), 0.75f, 1f);
    }

    public static float luminance(int c) {
        return (0.2126f * r(c) + 0.7152f * g(c) + 0.0722f * b(c)) / 255f;
    }

    public static String hex(int c, boolean alpha) {
        return alpha ? String.format("#%08X", c) : String.format("#%06X", c & 0xFFFFFF);
    }

    public static int parseHex(String s, int fallback) {
        try {
            String h = s.trim();
            if (h.startsWith("#")) h = h.substring(1);
            if (h.length() == 6) return 0xFF000000 | Integer.parseInt(h, 16);
            if (h.length() == 8) return (int) Long.parseLong(h, 16);
        } catch (NumberFormatException ignored) {
        }
        return fallback;
    }
}
