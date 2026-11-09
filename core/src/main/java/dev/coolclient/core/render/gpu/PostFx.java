package dev.coolclient.core.render.gpu;

import dev.coolclient.core.api.Gpu;
import dev.coolclient.core.api.GuiRenderer;

/**
 * Full-screen effects applied once the world is drawn and before the vanilla HUD: colour grading
 * and frame-blending motion blur. Both draw a GUI-sized quad with {@code post.fsh}.
 */
public final class PostFx {
    private static final int BLUR_SLOT = 1;
    private static boolean blurPrimed;

    private PostFx() {}

    private static void fullScreen(Gpu g, Shaders.Program p, GuiRenderer r) {
        GpuDraw.matrix(g, p);
        g.uniform(p.u("uViewport"), g.framebufferWidth(), g.framebufferHeight(), 0f, 0f);
        GpuDraw.quad(g, 0, 0, r.width(), r.height());
    }

    /** Grades the frame so far. Saturation/contrast/brightness are factors (1 = unchanged), hue in degrees. */
    public static void grade(GuiRenderer r, float saturation, float contrast, float brightness, float hueDegrees) {
        Gpu g = GpuDraw.gpu(r);
        if (g == null) return;
        Shaders.Program p = Shaders.post(g);
        if (p == null) return;
        // A fresh copy (not the cached glass backdrop: that may hold an older frame).
        GpuDraw.invalidateBackdrop();
        int tex = g.captureBackdrop();
        if (tex == 0) return;
        g.begin(p.id);
        try {
            g.bindTexture(0, tex);
            g.uniformInt(p.u("uFrame"), 0);
            g.uniformInt(p.u("uMode"), 0);
            g.uniform(p.u("uGrade"), saturation, contrast, brightness, (float) Math.toRadians(hueDegrees));
            fullScreen(g, p, r);
        } finally {
            g.end();
        }
        // Glass drawn later this frame should blur the graded picture.
        GpuDraw.invalidateBackdrop();
    }

    /** Blends last frame over this one with {@code amount} (0..1), then keeps this frame for next time. */
    public static void blur(GuiRenderer r, float amount) {
        Gpu g = GpuDraw.gpu(r);
        if (g == null) return;
        Shaders.Program p = Shaders.post(g);
        if (p == null) return;
        if (blurPrimed) {
            int prev = g.keptFrame(BLUR_SLOT);
            if (prev != 0) {
                g.begin(p.id);
                try {
                    g.bindTexture(0, prev);
                    g.uniformInt(p.u("uFrame"), 0);
                    g.uniformInt(p.u("uMode"), 1);
                    g.uniform(p.u("uGrade"), Math.max(0f, Math.min(0.95f, amount)), 0f, 0f, 0f);
                    fullScreen(g, p, r);
                } finally {
                    g.end();
                }
            }
        }
        blurPrimed = g.copyFrame(BLUR_SLOT) != 0;
        GpuDraw.invalidateBackdrop();
    }

    /** Forget the kept frame (motion blur turned off), so turning it on again doesn't flash an old frame. */
    public static void resetBlur() { blurPrimed = false; }
}
