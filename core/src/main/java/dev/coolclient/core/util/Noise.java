package dev.coolclient.core.util;

/** Fast hash-based value noise for procedural textures. */
public final class Noise {
    private Noise() {}

    public static float hash(int x, int y) {
        int h = x * 374761393 + y * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return (h & 0xFFFFFF) / (float) 0xFFFFFF;
    }

    public static float hash(int x, int y, int seed) {
        return hash(x + seed * 7919, y - seed * 104729);
    }

    private static float smooth(float t) { return t * t * (3f - 2f * t); }

    /** Smooth value noise in [0,1]. */
    public static float value(float x, float y) {
        int xi = (int) Math.floor(x), yi = (int) Math.floor(y);
        float xf = x - xi, yf = y - yi;
        float a = hash(xi, yi), b = hash(xi + 1, yi), c = hash(xi, yi + 1), d = hash(xi + 1, yi + 1);
        float u = smooth(xf), v = smooth(yf);
        return (a + (b - a) * u) + ((c + (d - c) * u) - (a + (b - a) * u)) * v;
    }

    /** Fractal noise in roughly [0,1]. */
    public static float fbm(float x, float y, int octaves) {
        float sum = 0, amp = 0.5f, norm = 0;
        for (int i = 0; i < octaves; i++) {
            sum += value(x, y) * amp;
            norm += amp;
            x *= 2.03f;
            y *= 2.03f;
            amp *= 0.5f;
        }
        return sum / norm;
    }
}
