package dev.coolclient.core.gui.screen;

import dev.coolclient.core.api.GuiRenderer;
import dev.coolclient.core.render.Colors;
import dev.coolclient.core.render.Draw;
import dev.coolclient.core.render.Theme;
import dev.coolclient.core.render.gpu.GpuDraw;
import dev.coolclient.core.util.Noise;

/**
 * The animated menu background: the theme's shader background ({@link Backgrounds}) when the
 * GPU path is available, otherwise a night sky with aurora ribbons drawn with triangles.
 */
public final class MenuBackground {
    private MenuBackground() {}

    public static void draw(GuiRenderer r, float w, float h, float t) {
        if (GpuDraw.background(r, Theme.background, 0, 0, w, h, t, 1f, 0f, Theme.accentA, Theme.accentB, Theme.PANEL)) return;
        drawFallback(r, w, h, t);
    }

    private static void drawFallback(GuiRenderer r, float w, float h, float t) {
        Draw.gradientV(r, 0, 0, w, h, 0xFF03050E, 0xFF0B1630);

        // Stars
        for (int i = 0; i < 140; i++) {
            float sx = Noise.hash(i, 1) * w, sy = Noise.hash(i, 2) * h * 0.75f;
            float tw = 0.4f + 0.6f * (float) Math.abs(Math.sin(t * (0.6f + Noise.hash(i, 3) * 2f) + i));
            float s = 0.35f + Noise.hash(i, 4) * 0.6f;
            Draw.rect(r, sx, sy, s, s, Colors.withAlpha(0xFFFFFF, (int) (200 * tw)));
        }

        // CoolClient ribbons
        int[][] palettes = {{Theme.accentA, 0xFF5BFF9A}, {Theme.accentB, Theme.accentA}, {0xFF5BFF9A, Theme.accentB}};
        int slices = 72;
        for (int k = 0; k < 3; k++) {
            float baseY = h * (0.26f + k * 0.07f);
            float amp = h * (0.05f + k * 0.015f);
            r.beginTriangles();
            float prevX = 0, prevC = 0, prevTop = 0, prevBot = 0;
            int prevCol = 0;
            for (int i = 0; i <= slices; i++) {
                float f = i / (float) slices;
                float x = f * w;
                float wave = (float) Math.sin(f * 5.2f + t * (0.22f + k * 0.07f) + k * 1.7f) * amp
                        + (Noise.value(f * 4f + k * 9f, t * 0.12f) - 0.5f) * amp * 1.6f;
                float c = baseY + wave;
                float height = h * (0.16f + 0.07f * (float) Math.sin(f * 3.1f + t * 0.3f + k));
                float top = c - height, bot = c + height * 0.18f;
                float intensity = 0.5f + 0.5f * Noise.value(f * 7f + k * 3f, t * 0.35f);
                int col = Colors.lerp(palettes[k][0], palettes[k][1], f);
                col = Colors.withAlpha(col, (int) (110 * intensity));
                if (i > 0) {
                    int clear = Colors.withAlpha(prevCol, 0), clear2 = Colors.withAlpha(col, 0);
                    // Upper fade: transparent top -> bright core
                    r.vertex(prevX, prevTop, clear);
                    r.vertex(x, top, clear2);
                    r.vertex(x, c, col);
                    r.vertex(prevX, prevTop, clear);
                    r.vertex(x, c, col);
                    r.vertex(prevX, prevC, prevCol);
                    // Lower fade
                    r.vertex(prevX, prevC, prevCol);
                    r.vertex(x, c, col);
                    r.vertex(x, bot, clear2);
                    r.vertex(prevX, prevC, prevCol);
                    r.vertex(x, bot, clear2);
                    r.vertex(prevX, prevBot, clear);
                }
                prevX = x;
                prevC = c;
                prevTop = top;
                prevBot = bot;
                prevCol = col;
            }
            r.endTriangles();
        }

        // Mountains
        mountains(r, w, h, h * 0.70f, h * 0.12f, 0xFF0A1022, 3);
        mountains(r, w, h, h * 0.80f, h * 0.10f, 0xFF060A16, 11);
        Draw.gradientV(r, 0, h * 0.6f, w, h * 0.4f, 0x00000000, 0x80000000);
    }

    private static void mountains(GuiRenderer r, float w, float h, float baseY, float amp, int color, int seed) {
        int n = 24;
        r.beginTriangles();
        float px = 0, py = baseY;
        for (int i = 0; i <= n; i++) {
            float x = i / (float) n * w;
            float y = baseY - (Noise.fbm(i * 0.45f + seed, seed, 3) - 0.3f) * amp * 2f;
            if (i > 0) {
                r.vertex(px, py, color);
                r.vertex(x, y, color);
                r.vertex(x, h, color);
                r.vertex(px, py, color);
                r.vertex(x, h, color);
                r.vertex(px, h, color);
            }
            px = x;
            py = y;
        }
        r.endTriangles();
    }
}
