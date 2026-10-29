package dev.coolclient.core.cosmetic.cape;

import dev.coolclient.core.render.Colors;
import dev.coolclient.core.util.Noise;

/** All built-in cape designs. */
public final class CapeStyles {
    private CapeStyles() {}

    private static float clamp01(float v) { return v < 0 ? 0 : (v > 1 ? 1 : v); }

    private static float frac(float v) { return v - (float) Math.floor(v); }

    /** Twinkling star field; returns star brightness 0..1. */
    private static float stars(float u, float v, float t, float density, int seed) {
        int gx = (int) (u * 40), gy = (int) (v * 64);
        float h = Noise.hash(gx, gy, seed);
        if (h < 1f - density) return 0f;
        float tw = 0.5f + 0.5f * (float) Math.sin(t * (2 + h * 4) + h * 40);
        return tw * (h - (1f - density)) / density;
    }

    private static int add(int base, int rgb, float k) {
        k = clamp01(k);
        return Colors.argb(255,
                Math.min(255, Colors.r(base) + (int) (Colors.r(rgb) * k)),
                Math.min(255, Colors.g(base) + (int) (Colors.g(rgb) * k)),
                Math.min(255, Colors.b(base) + (int) (Colors.b(rgb) * k)));
    }

    /** Dancing northern-lights curtains over a starry night. */
    public static final CapeStyle AURORA = (u, v, t, tint) -> {
        int c = Colors.lerp(0xFF040714, 0xFF0B1A2E, v);
        c = add(c, 0xFFFFFFFF, stars(u, v, t, 0.03f, 1) * 0.9f);
        int[] cols = {0xFF2DE2C1, 0xFF5BFF9A, 0xFF8A5CFF};
        for (int i = 0; i < 3; i++) {
            float center = 0.5f + 0.32f * (float) Math.sin(v * 3.1f + t * (0.45f + i * 0.17f) + i * 2.1f)
                    + 0.12f * (Noise.value(v * 3 + i * 5, t * 0.4f) - 0.5f);
            float width = 0.12f + 0.06f * (float) Math.sin(t * 0.7f + i);
            float d = (u - center) / width;
            float curtain = (float) Math.exp(-d * d);
            float rays = 0.55f + 0.45f * Noise.fbm(u * 9 + i * 3, v * 1.5f - t * 0.6f, 2);
            float fadeV = clamp01(1.2f - v * 0.9f) * clamp01(v * 3.5f);
            int col = Colors.lerp(cols[i], cols[(i + 1) % 3], v);
            c = add(c, col, curtain * rays * fadeV * 0.9f);
        }
        return c;
    };

    /** Swirling nebula with stars. */
    public static final CapeStyle GALAXY = (u, v, t, tint) -> {
        float cx = u - 0.5f, cy = (v - 0.5f) * 1.6f;
        float ang = (float) Math.atan2(cy, cx), rad = (float) Math.sqrt(cx * cx + cy * cy);
        float swirl = ang + rad * 4f - t * 0.25f;
        float n = Noise.fbm((float) Math.cos(swirl) * rad * 4 + 10, (float) Math.sin(swirl) * rad * 4 + 10, 4);
        int c = Colors.lerp(0xFF05030F, 0xFF1A0B3A, clamp01(n * 1.4f - 0.2f));
        c = Colors.lerp(c, 0xFF6A2BD9, clamp01((n - 0.5f) * 2.4f));
        c = Colors.lerp(c, 0xFFFF6FD8, clamp01((n - 0.68f) * 3.0f));
        c = add(c, 0xFF9AD8FF, clamp01(0.25f - rad) * 2.5f);
        return add(c, 0xFFFFFFFF, stars(u, v, t, 0.05f, 7));
    };

    /** Rising flames. */
    public static final CapeStyle INFERNO = (u, v, t, tint) -> {
        float n = Noise.fbm(u * 5f, v * 4f + t * 1.6f, 4);
        float heat = clamp01(n * 1.5f * (float) Math.pow(v, 0.8) + v * 0.35f - 0.1f);
        int c;
        if (heat < 0.35f) c = Colors.lerp(0xFF0A0202, 0xFF6B0A05, heat / 0.35f);
        else if (heat < 0.65f) c = Colors.lerp(0xFF6B0A05, 0xFFFF5A12, (heat - 0.35f) / 0.3f);
        else if (heat < 0.85f) c = Colors.lerp(0xFFFF5A12, 0xFFFFC23A, (heat - 0.65f) / 0.2f);
        else c = Colors.lerp(0xFFFFC23A, 0xFFFFF4C8, (heat - 0.85f) / 0.15f);
        float ember = Noise.hash((int) (u * 30), (int) (v * 48 + t * 20)) > 0.992f ? 1f : 0f;
        return add(c, 0xFFFFD27A, ember);
    };

    /** Rolling waves with caustics. */
    public static final CapeStyle OCEAN = (u, v, t, tint) -> {
        float w = (float) Math.sin(u * 7 + t * 1.3f + Noise.value(u * 3, v * 3 + t * 0.3f) * 4) * 0.5f + 0.5f;
        float depth = clamp01(v * 0.9f + 0.1f);
        int c = Colors.lerp(0xFF1FB6D6, 0xFF04173A, depth);
        float caustic = (float) Math.pow(Math.abs(Math.sin(Noise.fbm(u * 6 + t * 0.2f, v * 6 - t * 0.35f, 3) * 12)), 6);
        c = add(c, 0xFFBDF6FF, caustic * (1f - depth) * 0.8f);
        float crest = clamp01((w - 0.85f) * 6f) * (1f - depth);
        return add(c, 0xFFFFFFFF, crest * 0.7f);
    };

    /** Smooth flowing spectrum. */
    public static final CapeStyle RAINBOW = (u, v, t, tint) -> {
        float h = frac(u * 0.35f + v * 0.65f - t * 0.12f);
        float band = 0.85f + 0.15f * (float) Math.sin((u + v) * 18 - t * 3);
        return Colors.brighten(Colors.hsb(h, 0.72f, 1f), band);
    };

    /** Falling green code columns. */
    public static final CapeStyle MATRIX = (u, v, t, tint) -> {
        int col = (int) (u * 20);
        int row = (int) (v * 32);
        float speed = 0.35f + Noise.hash(col, 3) * 0.8f;
        float head = frac(t * speed + Noise.hash(col, 9));
        float d = frac(head - v);
        float trail = d < 0.55f ? (float) Math.exp(-d * 7f) : 0f;
        boolean glyph = Noise.hash(col, row + (int) (t * 7 + col)) > 0.35f;
        int c = 0xFF020A04;
        if (glyph) c = add(c, 0xFF22FF66, trail);
        if (d < 0.03f && glyph) c = add(c, 0xFFCCFFDD, 1f);
        return c;
    };

    /** Retro synthwave sun over a moving grid. */
    public static final CapeStyle SYNTHWAVE = (u, v, t, tint) -> {
        float horizon = 0.62f;
        if (v < horizon) {
            int sky = Colors.lerp(0xFF1A0638, 0xFFFF4F8B, clamp01(v / horizon));
            float dx = u - 0.5f, dy = (v - 0.42f) * 1.6f;
            float r = (float) Math.sqrt(dx * dx + dy * dy);
            if (r < 0.3f) {
                boolean stripe = v > 0.42f && frac((v - 0.42f) * 22f - t * 0.6f) < 0.35f;
                if (!stripe) return Colors.lerp(0xFFFFE066, 0xFFFF5E9C, clamp01((v - 0.15f) / 0.35f));
            }
            return add(sky, 0xFFFFFFFF, stars(u, v, t, 0.02f, 3) * clamp01(1f - v * 2f));
        }
        float gv = (v - horizon) / (1f - horizon);
        float z = 1f / (gv + 0.05f);
        float gx = (u - 0.5f) * z;
        float lineX = Math.abs(frac(gx * 1.2f) - 0.5f);
        float lineZ = Math.abs(frac(z * 0.5f + t * 0.8f) - 0.5f);
        float line = Math.max(clamp01(1f - lineX * 18f / z), clamp01(1f - lineZ * 30f / z));
        return add(0xFF12031F, 0xFFFF3DD2, line);
    };

    /** A violet vortex pulling into darkness. */
    public static final CapeStyle VOID = (u, v, t, tint) -> {
        float cx = u - 0.5f, cy = (v - 0.45f) * 1.6f;
        float rad = (float) Math.sqrt(cx * cx + cy * cy);
        float ang = (float) Math.atan2(cy, cx);
        float arms = (float) Math.sin(ang * 3 + rad * 14 - t * 2.2f) * 0.5f + 0.5f;
        float n = Noise.fbm(ang * 2 + t * 0.2f, rad * 5 - t * 0.5f, 3);
        float glow = clamp01(arms * n * 1.6f) * clamp01(rad * 3.5f) * clamp01(1.2f - rad);
        int c = Colors.lerp(0xFF000000, 0xFF2A0C52, clamp01(rad * 1.5f));
        c = add(c, 0xFFB14DFF, glow);
        if (rad < 0.09f) c = 0xFF000000;
        return add(c, 0xFFFFFFFF, stars(u, v, t, 0.02f, 11) * clamp01(rad * 2));
    };

    /** Soft pink sky with drifting cherry petals. */
    public static final CapeStyle SAKURA = (u, v, t, tint) -> {
        int c = Colors.lerp(0xFFFFD9E8, 0xFFB8C8FF, v);
        for (int i = 0; i < 9; i++) {
            float px = frac(Noise.hash(i, 1) + t * (0.03f + Noise.hash(i, 2) * 0.04f)
                    + 0.05f * (float) Math.sin(t * 1.3f + i));
            float py = frac(Noise.hash(i, 3) + t * (0.08f + Noise.hash(i, 4) * 0.06f));
            float dx = (u - px) * 1.0f, dy = (v - py) * 1.6f;
            float rot = t * 2 + i;
            float rx = dx * (float) Math.cos(rot) - dy * (float) Math.sin(rot);
            float ry = dx * (float) Math.sin(rot) + dy * (float) Math.cos(rot);
            float e = (rx * rx) / 0.0016f + (ry * ry) / 0.0006f;
            if (e < 1f) c = Colors.lerp(c, 0xFFFF7FB0, (1f - e) * 0.9f + 0.1f);
        }
        float branch = Math.abs(v - (0.15f + 0.1f * (float) Math.sin(u * 5))) < 0.012f + u * 0.01f ? 1f : 0f;
        return Colors.lerp(c, 0xFF5A3A3A, branch * clamp01(1.3f - u * 1.4f));
    };

    /** Storm clouds with periodic lightning strikes. */
    public static final CapeStyle STORM = (u, v, t, tint) -> {
        float n = Noise.fbm(u * 3 + t * 0.15f, v * 3, 4);
        int c = Colors.lerp(0xFF0A0F1C, 0xFF3A4660, n);
        float cycle = frac(t / 3.2f);
        float flash = cycle < 0.06f ? 1f - cycle / 0.06f : 0f;
        int strike = (int) Math.floor(t / 3.2f);
        float boltX = 0.3f + Noise.hash(strike, 5) * 0.4f;
        float path = boltX + (Noise.fbm(v * 6, strike * 3.1f, 3) - 0.5f) * 0.35f;
        float bolt = clamp01(1f - Math.abs(u - path) * 60f) * flash * (v < 0.9f ? 1f : 0f);
        c = add(c, 0xFF8FB3FF, flash * 0.35f * n);
        return add(c, 0xFFE8F0FF, bolt);
    };

    /** Two-colour gradient from the player's chosen tint, with a moving sheen. */
    public static final CapeStyle GRADIENT = (u, v, t, tint) -> {
        int dark = Colors.brighten(tint, 0.25f);
        int c = Colors.lerp(tint, dark, v);
        float sheen = clamp01(1f - Math.abs(frac(u * 0.6f + v * 0.4f - t * 0.25f) - 0.5f) * 10f);
        return add(c, 0xFFFFFFFF, sheen * 0.25f);
    };

    /** Founder cape: black and gold, a pulsing five-point star and a slow gold sheen. */
    public static final CapeStyle FOUNDER = (u, v, t, tint) -> {
        int base = Colors.lerp(0xFF17120A, 0xFF070604, v);
        float edge = Math.min(Math.min(u, 1 - u) * 10f / 1.4f, Math.min(v, 1 - v) * 16f / 1.4f);
        float sheen = clamp01(1f - Math.abs(frac(u * 0.7f + v * 0.5f - t * 0.18f) - 0.5f) * 6f);
        int gold = Colors.lerp(0xFFB8862B, 0xFFFFE08A, sheen);
        if (edge < 1f) return Colors.lerp(gold, base, edge);
        float x = (u - 0.5f) * 10f, y = (v - 0.40f) * 16f;
        double a = Math.atan2(x, -y);
        double r = Math.sqrt(x * x + y * y);
        double star = 2.7 * (0.42 + 0.58 * Math.pow(Math.abs(Math.cos(a * 2.5)), 2.5));
        if (r < star) {
            float pulse = 0.85f + 0.15f * (float) Math.sin(t * 2.0f);
            return Colors.brighten(gold, pulse);
        }
        float halo = clamp01(1f - (float) (r - star) / 3.5f);
        int c = add(base, 0xFFFFC83A, halo * halo * 0.35f);
        return add(c, 0xFFFFE9B0, stars(u, v, t, 0.04f, 77));
    };

    /** Branded cape: deep navy, accent border and a glowing chevron emblem. */
    public static final CapeStyle COOL_LOGO = (u, v, t, tint) -> {
        int base = Colors.lerp(0xFF0E1426, 0xFF070A14, v);
        float edge = Math.min(Math.min(u, 1 - u) * 10f / 1.6f, Math.min(v, 1 - v) * 16f / 1.6f);
        float g = frac(u * 0.5f + v * 0.5f - t * 0.2f);
        int accent = Colors.lerp(0xFF2DE2C1, 0xFF8A5CFF, Math.abs(g - 0.5f) * 2f);
        if (edge < 1f) return Colors.lerp(accent, base, edge);
        // chevron "A"
        float x = (u - 0.5f) * 10f, y = (v - 0.42f) * 16f;
        float arm = Math.abs(Math.abs(x) * 1.6f + y - 1.5f);
        boolean inA = y > -4f && y < 3.2f && arm < 0.9f;
        boolean bar = Math.abs(y - 1.2f) < 0.45f && Math.abs(x) < 1.6f;
        if (inA || bar) {
            float pulse = 0.85f + 0.15f * (float) Math.sin(t * 2.4f);
            return Colors.brighten(accent, pulse);
        }
        float halo = clamp01(1f - (x * x + y * y) / 30f);
        return add(base, accent, halo * 0.18f);
    };
}
