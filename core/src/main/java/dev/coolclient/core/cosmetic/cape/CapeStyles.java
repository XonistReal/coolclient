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

    // ------------------------------------------------------------------ Media capes (creators)

    /** Cape lettering: a small pixel font. Each row holds the glyph's pixels, leftmost in the highest bit. */
    private static final String GLYPHS = "ADEILMNORW ";
    private static final int[] GLYPH_WIDTH = {5, 5, 5, 3, 5, 5, 5, 5, 5, 5, 2};
    private static final int[][] FONT = {
        {0b01110, 0b10001, 0b10001, 0b11111, 0b10001, 0b10001, 0b10001}, // A
        {0b11110, 0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b11110}, // D
        {0b11111, 0b10000, 0b10000, 0b11110, 0b10000, 0b10000, 0b11111}, // E
        {0b111, 0b010, 0b010, 0b010, 0b010, 0b010, 0b111},               // I
        {0b10000, 0b10000, 0b10000, 0b10000, 0b10000, 0b10000, 0b11111}, // L
        {0b10001, 0b11011, 0b10101, 0b10101, 0b10001, 0b10001, 0b10001}, // M
        {0b10001, 0b11001, 0b10101, 0b10011, 0b10001, 0b10001, 0b10001}, // N
        {0b01110, 0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b01110}, // O
        {0b11110, 0b10001, 0b10001, 0b11110, 0b10100, 0b10010, 0b10001}, // R
        {0b10001, 0b10001, 0b10001, 0b10101, 0b10101, 0b11011, 0b10001}, // W
        {0, 0, 0, 0, 0, 0, 0},                                            // space
    };

    private static int textWidth(String s) {
        int w = -1;
        for (int i = 0; i < s.length(); i++) w += GLYPH_WIDTH[GLYPHS.indexOf(s.charAt(i))] + 1;
        return w;
    }

    /**
     * Whether (u, v) is on a lit pixel of {@code s}, centred across the cape with its top row at
     * texture row {@code row}. One font pixel is one texture pixel (the face is 40 x 64).
     */
    private static boolean lettering(String s, float u, float v, int row) {
        int y = (int) Math.floor(v * 64f) - row;
        if (y < 0 || y >= 7) return false;
        int x = (int) Math.floor(u * 40f) - (40 - textWidth(s)) / 2;
        for (int i = 0; i < s.length() && x >= 0; i++) {
            int g = GLYPHS.indexOf(s.charAt(i));
            int w = GLYPH_WIDTH[g];
            if (x < w) return ((FONT[g][y] >> (w - 1 - x)) & 1) != 0;
            x -= w + 1;
        }
        return false;
    }

    /** Signed distance (in cape pixels) to a rounded box centred on the origin; negative inside. */
    private static float roundBox(float x, float y, float halfW, float halfH, float radius) {
        float qx = Math.abs(x) - halfW + radius, qy = Math.abs(y) - halfH + radius;
        float ox = Math.max(qx, 0), oy = Math.max(qy, 0);
        return (float) Math.sqrt(ox * ox + oy * oy) + Math.min(Math.max(qx, qy), 0) - radius;
    }

    /** Media: broadcast red, a round white play button with a slow shine, and MEDIA lettering. */
    public static final CapeStyle MEDIA = (u, v, t, tint) -> {
        int c = Colors.lerp(0xFFD0142C, 0xFF4A040E, v);
        c = Colors.lerp(c, 0xFF2A0207, clamp01(Math.abs(u - 0.5f) * 1.4f - 0.3f));
        float edge = Math.min(Math.min(u, 1 - u) * 10f, Math.min(v, 1 - v) * 16f);
        float sheen = clamp01(1f - Math.abs(frac(u * 0.6f + v * 0.4f - t * 0.22f) - 0.5f) * 7f);
        if (edge < 0.5f) return Colors.lerp(0xFFF4C9CE, 0xFFFFFFFF, sheen);
        float x = (u - 0.5f) * 10f, y = v * 16f - 6.0f;
        float d = (float) Math.sqrt(x * x + y * y) - 2.8f;
        if (d < 0) {
            float tx = x - 0.2f; // centres the triangle's mass in the circle
            if (tx > -1.1f && Math.abs(y) < 1.3f * (1.5f - tx) / 2.6f) return 0xFFD8142B;
            float pulse = 0.93f + 0.07f * (float) Math.sin(t * 2.2f);
            return Colors.brighten(Colors.lerp(0xFFEDEDF2, 0xFFFFFFFF, sheen), pulse);
        }
        float glow = clamp01(1f - d / 2.2f);
        c = add(c, 0xFFFF8A96, glow * glow * 0.35f);
        if (lettering("MEDIA", u, v, 41)) return 0xFFFFFFFF;
        if (lettering("MEDIA", u - 1f / 40f, v - 1f / 64f, 41)) return 0xFF3A0309;
        float bar = clamp01(1f - Math.abs(frac(v * 1.2f - t * 0.3f) - 0.5f) * 14f);
        return add(c, 0xFFFFFFFF, bar * 0.07f);
    };

    /** On Air: studio black, a glowing ON AIR sign that flickers now and then, and broadcast rings. */
    public static final CapeStyle ON_AIR = (u, v, t, tint) -> {
        int c = Colors.lerp(0xFF17171D, 0xFF060608, v);
        float cycle = frac(t / 4.3f);
        float on = cycle > 0.90f && cycle < 0.93f ? 0.35f : 1f;
        float edge = Math.min(Math.min(u, 1 - u) * 10f, Math.min(v, 1 - v) * 16f);
        if (edge < 0.4f) return Colors.lerp(0xFF3A060C, 0xFFE0182D, on * 0.8f);
        float x = (u - 0.5f) * 10f;
        float d = roundBox(x, v * 16f - 3.125f, 4.4f, 1.45f, 0.6f);
        if (d < 0) {
            if (lettering("ON AIR", u, v, 9)) return Colors.lerp(0xFF7A1018, 0xFFFFF2F4, on);
            return Colors.lerp(0xFF5A0810, 0xFFE41A33, on);
        }
        float glow = clamp01(1f - d / 1.8f);
        c = add(c, 0xFFFF2D4A, glow * glow * 0.5f * on);
        float dy = v * 16f - 10.5f;
        float r = (float) Math.sqrt(x * x + dy * dy);
        if (r < 0.75f) return Colors.brighten(0xFFFF2D4A, 0.85f + 0.15f * (float) Math.sin(t * 4f));
        float ring = 0;
        for (int i = 0; i < 3; i++) {
            float rr = 0.9f + frac(t * 0.45f + i / 3f) * 5.5f;
            ring += clamp01(1f - Math.abs(r - rr) * 2.6f) * (1f - (rr - 0.9f) / 5.5f);
        }
        return add(c, 0xFFFF4A60, ring * 0.9f);
    };

    /** Premiere: a film strip down the sides, spotlights sweeping a gold-lit stage, a star on top. */
    public static final CapeStyle PREMIERE = (u, v, t, tint) -> {
        float x = (u - 0.5f) * 10f, y = v * 16f;
        float side = Math.min(u, 1 - u) * 10f;
        if (side < 1.6f) {
            float hy = frac((y - t * 1.6f) / 2f);
            boolean hole = side > 0.45f && side < 1.2f && hy > 0.25f && hy < 0.75f;
            return hole ? 0xFFEDE6D6 : 0xFF0B0A0C;
        }
        int c = Colors.lerp(0xFF1B0B33, 0xFF07040E, clamp01(v * 1.1f));
        c = add(c, 0xFFFFB547, clamp01((v - 0.72f) * 3f) * 0.35f);
        for (int i = 0; i < 2; i++) {
            float ox = i == 0 ? -3.4f : 3.4f;
            float ang = (float) Math.atan2(x - ox, 16.5f - y);
            float aim = (i == 0 ? 0.35f : -0.35f) + 0.32f * (float) Math.sin(t * 0.9f + i * 2.2f);
            float beam = clamp01(1f - Math.abs(ang - aim) / 0.16f);
            float near = clamp01(1f - (16.5f - y) / 17f);
            c = add(c, i == 0 ? 0xFFFFE9B0 : 0xFFB9A2FF, beam * beam * (0.25f + 0.45f * near));
        }
        float sy = y - 4.2f;
        double a = Math.atan2(x, -sy);
        double rad = Math.sqrt(x * x + sy * sy);
        double star = 1.6 * (0.42 + 0.58 * Math.pow(Math.abs(Math.cos(a * 2.5)), 2.5));
        if (rad < star) return Colors.brighten(0xFFFFC83A, 0.88f + 0.12f * (float) Math.sin(t * 3f));
        float halo = clamp01(1f - (float) (rad - star) / 2.2f);
        c = add(c, 0xFFFFC83A, halo * halo * 0.3f);
        return add(c, 0xFFFFFFFF, stars(u, v, t, 0.025f, 41) * 0.6f);
    };

    private static final float[] CROWN_TIPS = {-2.6f, 0f, 2.6f};
    private static final float[] CROWN_JEWELS = {-1.6f, 0f, 1.6f};

    /** Owner: midnight navy, the brand's teal-to-purple light around the edge, a gold crown and OWNER lettering. */
    public static final CapeStyle OWNER = (u, v, t, tint) -> {
        int c = Colors.lerp(0xFF0E1426, 0xFF05070D, v);
        float edge = Math.min(Math.min(u, 1 - u) * 10f, Math.min(v, 1 - v) * 16f);
        float flow = frac(u * 0.5f + v * 0.5f - t * 0.25f);
        int brand = Colors.lerp(0xFF2DE2C1, 0xFF8A5CFF, Math.abs(flow - 0.5f) * 2f);
        if (edge < 0.55f) return brand;
        c = add(c, brand, clamp01(1f - (edge - 0.55f) / 1.6f) * 0.28f);
        float x = (u - 0.5f) * 10f, y = v * 16f;
        float ax = Math.abs(x);
        float top = 3.7f + Math.min(ax, Math.abs(ax - 2.6f)) * 1.55f;
        boolean body = ax < 2.9f && y > top && y < 6.9f;
        boolean band = ax < 3.1f && y >= 6.9f && y < 7.9f;
        if (body || band) {
            for (float jx : CROWN_JEWELS) {
                float dx = x - jx, dy = y - 7.4f;
                if (dx * dx + dy * dy < 0.17f) return jx == 0 ? 0xFF2DE2C1 : 0xFF8A5CFF;
            }
            float sheen = clamp01(1f - Math.abs(frac(x * 0.08f + y * 0.05f - t * 0.3f) - 0.5f) * 8f);
            return Colors.lerp(0xFFC9901E, 0xFFFFE27A, clamp01((7.9f - y) / 4.2f * 0.6f + sheen * 0.5f));
        }
        for (float jx : CROWN_TIPS) {
            float dx = x - jx, dy = y - 3.45f;
            if (dx * dx + dy * dy < 0.22f) return Colors.brighten(0xFFFFE27A, 0.9f + 0.1f * (float) Math.sin(t * 3f + jx));
        }
        float gx = x / 4.5f, gy = (y - 5.8f) / 4f;
        c = add(c, 0xFFFFC83A, clamp01(1f - (gx * gx + gy * gy)) * 0.18f);
        if (lettering("OWNER", u, v, 41)) return 0xFFFFFFFF;
        if (lettering("OWNER", u - 1f / 40f, v - 1f / 64f, 41)) return 0xFF05070D;
        return add(c, 0xFFFFFFFF, stars(u, v, t, 0.03f, 13) * 0.7f);
    };

    // ------------------------------------------------------------------ Thanksgiving drop (seasonal)

    private static final int[] LEAF_COLORS = {0xFFE8471C, 0xFFF29A1F, 0xFFF7C843, 0xFFB5301A, 0xFFD9661C, 0xFFA8541E};

    /** Maple leaf outline: radius (relative) at angle a from the tip; five lobes, the lowest two smaller. */
    private static float mapleLobe(float a) {
        float c = (float) Math.pow(Math.abs(Math.cos(a * 2.5f)), 1.5);
        float lobe = 0.5f + 0.5f * c;
        float aa = Math.abs(a);
        if (aa > 1.9f) lobe *= 1f - 0.4f * clamp01((aa - 1.9f) / 1.2f);
        return lobe;
    }

    /** Autumn: a warm late-afternoon sky, maple leaves tumbling down and a pile of them at the bottom. */
    public static final CapeStyle AUTUMN = (u, v, t, tint) -> {
        float px = u * 40f, py = v * 64f;
        boolean shadow = false;
        for (int i = 6; i >= 0; i--) {
            float h = Noise.hash(i, 2);
            float lx0 = 0.12f + 0.76f * Noise.hash(i, 1) + 0.06f * (float) Math.sin(t * (0.9f + h * 0.6f) + i * 1.7f);
            float ly0 = -0.12f + 1.24f * frac(Noise.hash(i, 3) + t * (0.045f + Noise.hash(i, 4) * 0.035f));
            float lx = px - lx0 * 40f, ly = py - ly0 * 64f;
            if (Math.abs(lx) > 10f || Math.abs(ly) > 10f) continue;
            float size = 3.3f + Noise.hash(i, 5) * 1.6f;
            float rot = t * (0.6f + Noise.hash(i, 6)) * (i % 2 == 0 ? 1f : -1f) + i * 2f;
            float flip = 0.35f + 0.65f * Math.abs((float) Math.cos(t * (0.8f + Noise.hash(i, 7)) + i));
            float cr = (float) Math.cos(rot), sr = (float) Math.sin(rot);
            for (int pass = 0; pass < 2; pass++) {
                // Pass 0 is the leaf, pass 1 its shadow one pixel down and right.
                float qx = lx - pass, qy = ly - pass;
                float rx = (qx * cr - qy * sr) / flip, ry = qx * sr + qy * cr;
                float r = (float) Math.sqrt(rx * rx + ry * ry);
                float shape = size * mapleLobe((float) Math.atan2(rx, -ry));
                boolean stem = Math.abs(rx) < 0.45f && ry > 0 && ry < size * 1.3f;
                if (r < shape || stem) {
                    if (pass == 1) {
                        shadow = true;
                        break;
                    }
                    int col = LEAF_COLORS[(int) (Noise.hash(i, 8) * LEAF_COLORS.length) % LEAF_COLORS.length];
                    if (stem && r >= shape) return Colors.brighten(col, 0.55f);
                    if (r > shape - 0.7f) return Colors.brighten(col, 0.6f);
                    int c = Colors.lerp(Colors.brighten(col, 1.2f), col, r / shape);
                    if (Math.abs(rx) < 0.35f && ry < 0) c = Colors.brighten(c, 0.8f);
                    return c;
                }
            }
        }
        // Sky, with a soft sun up and to the right.
        int c = Colors.lerp(0xFFE58A34, 0xFFA8401C, clamp01(v * 1.25f));
        c = Colors.lerp(c, 0xFF3C160C, clamp01((v - 0.58f) * 2.2f));
        float sx = (u - 0.72f) * 10f, sy = (v - 0.12f) * 16f;
        float sun = clamp01(1f - (sx * sx + sy * sy) / 22f);
        c = add(c, 0xFFFFD27A, sun * sun * 0.45f);
        c = Colors.lerp(c, Colors.brighten(c, 1.07f), Noise.fbm(u * 3 + t * 0.05f, v * 3, 2) * 0.5f);
        // Leaf pile along the bottom.
        if (v > 0.885f + 0.025f * (float) Math.sin(u * 9f + 1f)) {
            int cell = (int) (Noise.hash((int) (px / 2), (int) (py / 2), 5) * LEAF_COLORS.length) % LEAF_COLORS.length;
            c = Colors.brighten(LEAF_COLORS[cell], 0.7f + 0.2f * Noise.hash((int) px, (int) py, 9));
        }
        return shadow ? Colors.brighten(c, 0.84f) : c;
    };

    private static final int[] FEATHERS = {0xFFB8321F, 0xFFE0731F, 0xFFF2B33A, 0xFF9A5426, 0xFFCF4A1E};

    /** Gobbler: a cartoon turkey in front of its fanned tail. It gobbles (bobs its head) now and then. */
    public static final CapeStyle GOBBLER = (u, v, t, tint) -> {
        float x = u * 40f, y = v * 64f;
        // Head with eyes, beak and wattle.
        float ph = frac(t / 5f);
        float bob = ph < 0.3f ? (float) Math.sin(ph / 0.3f * Math.PI * 3) * 1.0f : 0f;
        float hx = 20f, hy = 36.5f + bob;
        float swing = (float) Math.sin(t * 3f) * 0.4f + bob * 0.4f;
        float wx = x - (21.4f + swing), wy = (y - (41.6f + bob)) / 2.0f;
        if (wx * wx / 1.2f + wy * wy < 1f) return Colors.lerp(0xFFE8333F, 0xFFB3121F, clamp01(wy * 0.5f + 0.5f));
        if (y > hy + 1.6f && y < hy + 4.4f && Math.abs(x - hx) < (hy + 4.4f - y) * 0.55f) return 0xFFF5A623;
        boolean blink = frac(t / 3.7f) > 0.96f;
        for (int s = -1; s <= 1; s += 2) {
            float ex = x - (hx + s * 1.9f), ey = y - (hy - 0.6f);
            if (blink && Math.abs(ey) < 0.5f && Math.abs(ex) < 1.1f) return 0xFF1A0E08;
            if (!blink && ex * ex + ey * ey < 1.6f) return ex * ex + ey * ey < 0.45f ? 0xFF120A06 : 0xFFF8F2E8;
        }
        float hd = (float) Math.sqrt((x - hx) * (x - hx) + (y - hy) * (y - hy));
        if (hd < 4.2f) return Colors.lerp(0xFFDDA16A, 0xFFB97A45, clamp01((y - hy + 4f) / 8f));
        if (hd < 4.9f) return 0xFF2A140A;
        // Body: scalloped brown feathers, lighter in the middle.
        float bx = (x - 20f) / 8.5f, by = (y - 50.5f) / 9.5f;
        float e = bx * bx + by * by;
        if (e < 1f) {
            int c = Colors.lerp(0xFF9A5A2E, 0xFF6E3A1C, clamp01(e * 1.2f));
            int row = (int) Math.floor((y - 40f) / 3f);
            float fx = frac((x + (row % 2) * 1.5f) / 3f) - 0.5f, fy = frac((y - 40f) / 3f);
            if (fy > 0.72f - fx * fx * 1.6f) c = Colors.brighten(c, 0.78f);
            return c;
        }
        if (e < 1.18f) return 0xFF2A140A;
        // Feet.
        for (int s = -1; s <= 1; s += 2) {
            float fx = x - (20f + s * 3.5f);
            if ((Math.abs(fx) < 0.6f && y > 58.5f && y < 61.6f) || (Math.abs(fx) < 2.2f && y >= 61.6f && y < 62.8f)) return 0xFFF09A2A;
        }
        // Tail fan.
        float dx = x - 20f, dy = 47f - y;
        float r = (float) Math.sqrt(dx * dx + dy * dy);
        float a = (float) Math.atan2(dx, dy) * (1f - 0.04f * (float) Math.sin(t * 0.9f)) + 0.06f * (float) Math.sin(t * 1.4f);
        int n = 9;
        float span = 1.95f, fw = span * 2 / n;
        float pos = (a + span) / fw;
        int k = (int) Math.floor(pos);
        int bg = Colors.lerp(0xFF3A1B0E, 0xFF1C0C06, v);
        bg = add(bg, 0xFFFF9A3C, clamp01(1f - r / 34f) * 0.22f);
        if (k < 0 || k >= n) return bg;
        float fa = pos - k - 0.5f;
        float len = 41f - Math.abs(k - (n - 1) / 2f) * 1.2f + Noise.hash(k, 4) * 2f;
        float rmax = len - 7f * (2 * fa) * (2 * fa);
        if (r > rmax + 0.7f) return bg;
        if (Math.abs(fa) > 0.44f || r > rmax) return 0xFF2A140A;
        float rel = r / rmax;
        int col = FEATHERS[Math.abs(k - (n - 1) / 2) % FEATHERS.length];
        int c;
        if (rel > 0.87f) c = Colors.lerp(0xFFF6E8CC, 0xFFE2CBA0, (rel - 0.87f) / 0.13f);
        else if (rel > 0.79f) c = 0xFF3A1E10;
        else c = Colors.lerp(Colors.brighten(col, 0.55f), col, clamp01(rel * 1.6f));
        c = Colors.brighten(c, 1f - 0.25f * (fa / 0.44f) * (fa / 0.44f));
        if (Math.abs(fa) < 0.05f && rel < 0.79f) c = Colors.brighten(c, 0.8f);
        float sheen = clamp01(1f - Math.abs(frac(a * 0.5f - t * 0.3f) - 0.5f) * 8f);
        return add(c, 0xFFFFFFFF, sheen * 0.15f * (rel < 0.79f ? 1f : 0.3f));
    };

    /** x, y, half-width, half-height of the two pumpkins on the Harvest Moon cape (cape pixels). */
    private static final float[][] PUMPKINS = {{8f, 60.2f, 3.4f, 2.5f}, {31.5f, 61f, 2.8f, 2.1f}};
    private static final float[] CORN_SHOCKS = {10f, 29f};

    /** Harvest Moon: a big orange moon over the fields, corn shocks, wheat in the wind and two pumpkins. */
    public static final CapeStyle HARVEST_MOON = (u, v, t, tint) -> {
        float x = u * 40f, y = v * 64f;
        // Pumpkins in front.
        for (float[] p : PUMPKINS) {
            float ex = (x - p[0]) / p[2], ey = (y - p[1]) / p[3];
            if (Math.abs(x - p[0] - 0.3f) < 0.5f && y > p[1] - p[3] - 1.3f && y < p[1] - p[3] + 0.4f) return 0xFF4A6A22;
            if (ex * ex + ey * ey < 1f) {
                int c = Colors.lerp(0xFFF59A33, 0xFFC4521A, clamp01(ey * 0.5f + 0.5f + ex * ex * 0.3f));
                if (Math.abs((float) Math.cos(ex * 3.2f)) < 0.25f) c = Colors.brighten(c, 0.75f);
                return c;
            }
        }
        // Wheat in the wind.
        if (y > 52f) {
            int s = (int) Math.floor(x / 2.5f);
            float hs = 6f + Noise.hash(s, 11) * 5f;
            float sway = (float) Math.sin(t * 1.6f + s * 0.7f) * (64f - y) * 0.06f;
            float sx = s * 2.5f + 1.2f + sway;
            if (y > 64f - hs) {
                boolean head = y < 64f - hs + 2.6f;
                if (Math.abs(x - sx) < (head ? 0.9f : 0.45f)) return head ? 0xFF9A6A26 : 0xFF3A2410;
            }
        }
        // Hills and corn shocks, lit from behind by the moon.
        float hill = 50f + 2.2f * (float) Math.sin(x * 0.12f + 1f) + 1.2f * (float) Math.sin(x * 0.31f);
        for (float sx : CORN_SHOCKS) {
            float base = 50f + 2.2f * (float) Math.sin(sx * 0.12f + 1f) + 1.2f * (float) Math.sin(sx * 0.31f) + 0.5f;
            float top = base - 9f;
            if (y > top && y < base) {
                float half = (y - top) / 9f * 3.6f;
                if (y > top + 3.2f && y < top + 4.0f) half += 0.4f;
                if (Math.abs(x - sx) < half) return Math.abs(x - sx) > half - 0.6f || y < top + 0.8f ? 0xFF6A3424 : 0xFF1E0F14;
            }
        }
        if (y > hill) return y - hill < 0.8f ? 0xFF5A2A28 : Colors.lerp(0xFF1E0E14, 0xFF0E060A, clamp01((y - hill) / 12f));
        // A V of geese flying across now and then.
        float gp = frac(t / 14f);
        float lead = -6f + gp * 56f, gy = 33f - gp * 5f;
        for (int j = 0; j < 5; j++) {
            int rank = (j + 1) / 2, side = j % 2 == 0 ? 1 : -1;
            float bx = x - (lead - rank * 4.2f), by = y - (gy + side * rank * 2.6f);
            float flap = (float) Math.sin(t * 7f + j * 1.3f) * 0.7f;
            if (Math.abs(bx) < 1.7f && Math.abs(by + Math.abs(bx) * 0.55f - 0.3f + flap * (Math.abs(bx) / 1.7f)) < 0.42f) return 0xFF140A18;
        }
        // Sky.
        int c = Colors.lerp(0xFF0A0B26, 0xFF2A1640, clamp01(y / 40f));
        c = Colors.lerp(c, 0xFF7A3048, clamp01((y - 30f) / 20f));
        c = add(c, 0xFFFFFFFF, stars(u, v, t, 0.03f, 23) * clamp01(1f - y / 36f) * 0.8f);
        float md = (float) Math.sqrt((x - 20f) * (x - 20f) + (y - 20f) * (y - 20f));
        float moonR = 10.5f;
        if (md < moonR) {
            c = Colors.lerp(0xFFFFC56A, 0xFFF0781E, clamp01((md / moonR) * (md / moonR) * 0.8f + (y - 20f) / moonR * 0.25f + 0.2f));
            float n = Noise.fbm(x * 0.25f + 3f, y * 0.25f + 7f, 3);
            c = Colors.lerp(c, Colors.brighten(c, 0.78f), clamp01((n - 0.52f) * 3f));
            if (md > moonR - 0.8f) c = Colors.brighten(c, 0.9f);
        } else {
            float g = clamp01(1f - (md - moonR) / 9f);
            c = add(c, 0xFFFF9A3C, g * g * 0.45f);
        }
        // Thin clouds drifting across the moon.
        float cloud = clamp01((Noise.fbm(x * 0.08f - t * 0.15f, y * 0.35f, 3) - 0.5f) * 3f) * (float) Math.exp(-((y - 25f) / 3.5f) * ((y - 25f) / 3.5f));
        return Colors.lerp(c, md < moonR + 2f ? 0xFF9A4A3A : 0xFF2A1638, cloud * 0.5f);
    };
}
