package dev.coolclient.core.render.gpu;

import dev.coolclient.core.api.Gpu;
import dev.coolclient.core.api.GuiRenderer;
import dev.coolclient.core.render.Colors;
import dev.coolclient.core.render.Theme;
import dev.coolclient.core.util.Time;

/**
 * GPU (shader) implementations of the UI primitives. Every method returns false when shaders
 * are unavailable or disabled, so {@link dev.coolclient.core.render.Draw} can fall back to its
 * CPU-tessellated geometry.
 */
public final class GpuDraw {
    public static final int FILL = 0, STROKE = 1, SHADOW = 2, GLOW = 3, ARC = 4, GLASS = 5;
    public static final int GRAD_NONE = 0, GRAD_H = 1, GRAD_V = 2, GRAD_RADIAL = 3, GRAD_DIAGONAL = 4;

    private static final float[] QUAD = new float[12];
    private static boolean enabled = !Boolean.getBoolean("coolclient.noshaders");

    private static Gpu backdropGpu;
    private static long backdropFrame = -1;
    private static int backdropTexture;

    private GpuDraw() {}

    public static void setEnabled(boolean on) { enabled = on && !Boolean.getBoolean("coolclient.noshaders"); }

    public static boolean enabled() { return enabled; }

    /** The renderer's GPU access, or null when shapes should use geometry. */
    public static Gpu gpu(GuiRenderer r) {
        return enabled ? r.gpu() : null;
    }

    /** True when the shape program is available on this renderer. */
    public static boolean available(GuiRenderer r) {
        Gpu g = gpu(r);
        return g != null && Shaders.shape(g) != null;
    }

    // ================================================================== shapes

    public static boolean shape(GuiRenderer r, float x, float y, float w, float h,
                                float tl, float tr, float br, float bl,
                                int c0, int c1, int gradient, int mode, float param) {
        return shape(r, x, y, w, h, tl, tr, br, bl, c0, c1, gradient, mode, param, -1f);
    }

    /** One SDF quad. {@code shine} (0..1) sweeps a highlight across the shape; negative = off. */
    public static boolean shape(GuiRenderer r, float x, float y, float w, float h,
                                float tl, float tr, float br, float bl,
                                int c0, int c1, int gradient, int mode, float param, float shine) {
        Gpu g = gpu(r);
        if (g == null) return false;
        Shaders.Program p = Shaders.shape(g);
        if (p == null) return false;
        float max = Math.min(w, h) / 2f;
        float margin = mode == SHADOW || mode == GLOW ? param + 2f : 1.5f;
        g.begin(p.id);
        try {
            matrix(g, p);
            g.uniform(p.u("uRect"), x + w / 2f, y + h / 2f, w / 2f, h / 2f);
            g.uniform(p.u("uRadii"), Math.min(tl, max), Math.min(tr, max), Math.min(br, max), Math.min(bl, max));
            color(g, p.u("uColor0"), c0);
            color(g, p.u("uColor1"), c1);
            g.uniform(p.u("uParams"), mode, param, gradient, shine);
            quad(g, x - margin, y - margin, x + w + margin, y + h + margin);
        } finally {
            g.end();
        }
        return true;
    }

    /** Arc stroke with round caps; angles in degrees, 0 = up, clockwise. */
    public static boolean arc(GuiRenderer r, float cx, float cy, float radius, float thickness,
                              float startDeg, float sweepDeg, int c) {
        Gpu g = gpu(r);
        if (g == null) return false;
        Shaders.Program p = Shaders.shape(g);
        if (p == null) return false;
        if (sweepDeg < 0) {
            startDeg += sweepDeg;
            sweepDeg = -sweepDeg;
        }
        float ext = radius + thickness + 1.5f;
        g.begin(p.id);
        try {
            matrix(g, p);
            g.uniform(p.u("uRect"), cx, cy, ext, ext);
            g.uniform(p.u("uArc"), radius, (float) Math.toRadians(startDeg), (float) Math.toRadians(Math.min(360f, sweepDeg)), 0f);
            color(g, p.u("uColor0"), c);
            g.uniform(p.u("uParams"), ARC, thickness, 0f, -1f);
            quad(g, cx - ext, cy - ext, cx + ext, cy + ext);
        } finally {
            g.end();
        }
        return true;
    }

    /**
     * Frosted glass: blurs whatever was drawn behind the rectangle, then mixes in {@code tint}
     * by the tint's alpha. {@code opacity} fades the whole panel (for open animations).
     */
    public static boolean glass(GuiRenderer r, float x, float y, float w, float h, float radius,
                                int tint, float opacity, float blur) {
        Gpu g = gpu(r);
        if (g == null) return false;
        Shaders.Program p = Shaders.shape(g);
        if (p == null) return false;
        int tex = backdrop(g);
        if (tex == 0) return false;
        float max = Math.min(w, h) / 2f;
        float rr = Math.min(radius, max);
        g.begin(p.id);
        try {
            matrix(g, p);
            g.bindTexture(0, tex);
            g.uniformInt(p.u("uBackdrop"), 0);
            g.uniform(p.u("uViewport"), g.framebufferWidth(), g.framebufferHeight(), blur, Theme.light ? 0.012f : 0.02f);
            g.uniform(p.u("uRect"), x + w / 2f, y + h / 2f, w / 2f, h / 2f);
            g.uniform(p.u("uRadii"), rr, rr, rr, rr);
            color(g, p.u("uColor0"), tint);
            g.uniform(p.u("uColor1"), 1f, 1f, 1f, Math.max(0f, Math.min(1f, opacity)));
            g.uniform(p.u("uParams"), GLASS, 0f, 0f, -1f);
            quad(g, x - 1.5f, y - 1.5f, x + w + 1.5f, y + h + 1.5f);
        } finally {
            g.end();
        }
        return true;
    }

    /** Captures the frame so far once per frame, for every glass panel drawn after it. */
    private static int backdrop(Gpu g) {
        long frame = Time.frameIndex();
        if (g != backdropGpu || frame != backdropFrame) {
            backdropGpu = g;
            backdropFrame = frame;
            backdropTexture = g.captureBackdrop();
        }
        return backdropTexture;
    }

    /** Captures the backdrop right now (once per frame), ahead of any glass drawn later. */
    public static void captureBackdrop(GuiRenderer r) {
        Gpu g = gpu(r);
        if (g != null && Shaders.shape(g) != null) backdrop(g);
    }

    /** Forces the next glass panel to re-capture (after drawing something it should blur). */
    public static void invalidateBackdrop() { backdropFrame = -1; }

    // ================================================================== backgrounds and skies

    public static boolean background(GuiRenderer r, Backgrounds.Style style, float x, float y, float w, float h,
                                     float time, float alpha, float radius, int accentA, int accentB, int base) {
        Gpu g = gpu(r);
        if (g == null || style == null) return false;
        Shaders.Program p = Shaders.background(g, style);
        if (p == null) return false;
        g.begin(p.id);
        try {
            matrix(g, p);
            g.uniform(p.u("uRect"), x, y, w, h);
            g.uniform(p.u("uTime"), (float) (time % 3600.0), alpha, radius, 0f);
            color(g, p.u("uAccentA"), accentA);
            color(g, p.u("uAccentB"), accentB);
            color(g, p.u("uBase"), base);
            quad(g, x, y, x + w, y + h);
        } finally {
            g.end();
        }
        return true;
    }

    /**
     * Renders a skybox into a GUI rectangle through a virtual camera. {@code imageTexture} is a
     * core texture handle for the Custom Image sky (0 otherwise).
     */
    public static boolean skyPreview(GuiRenderer r, SkyStyle style, float x, float y, float w, float h, float radius,
                                     float yaw, float pitch, float time, int imageTexture) {
        Gpu g = gpu(r);
        if (g == null) return false;
        Shaders.Program p = Shaders.sky(g, style, true);
        if (p == null) return false;
        g.begin(p.id);
        try {
            matrix(g, p);
            g.uniform(p.u("uRect"), x, y, w, h);
            g.uniform(p.u("uCam"), yaw, pitch, (float) Math.tan(Math.toRadians(35)), radius);
            float[] sun = SkyRenderer.sunDirection(SkyRenderer.previewTimeOfDay(time));
            g.uniform(p.u("uSun"), sun[0], sun[1], sun[2], SkyRenderer.daylight(sun[1]));
            g.uniform(p.u("uSky"), (float) (time % 3600.0), 0f, 1f, 1f);
            if (imageTexture != 0) {
                g.bindTexture(0, g.glTexture(imageTexture));
                g.uniformInt(p.u("uImage"), 0);
            }
            quad(g, x, y, x + w, y + h);
        } finally {
            g.end();
        }
        return true;
    }

    // ================================================================== helpers

    private static void matrix(Gpu g, Shaders.Program p) {
        float[] m = g.guiMatrix();
        if (m != null) g.uniformMatrix(p.u("uMVP"), m);
    }

    static void color(Gpu g, int loc, int c) {
        g.uniform(loc, Colors.r(c) / 255f, Colors.g(c) / 255f, Colors.b(c) / 255f, Colors.a(c) / 255f);
    }

    private static void quad(Gpu g, float x0, float y0, float x1, float y1) {
        float[] q = QUAD;
        q[0] = x0; q[1] = y0;
        q[2] = x0; q[3] = y1;
        q[4] = x1; q[5] = y1;
        q[6] = x0; q[7] = y0;
        q[8] = x1; q[9] = y1;
        q[10] = x1; q[11] = y0;
        g.draw(q, 6);
    }
}
