package dev.coolclient.core.render;

import dev.coolclient.core.api.GuiRenderer;
import dev.coolclient.core.render.gpu.GpuDraw;

/**
 * Shape drawing. Rounded shapes, outlines, shadows, glows, circles and arcs are signed
 * distance fields evaluated in a shader ({@link GpuDraw}) when the platform exposes one; the
 * CPU path below (triangles with a one-device-pixel alpha fringe) is the fallback.
 */
public final class Draw {
    /** Per-vertex colour function. */
    public interface Shader {
        int color(float x, float y);
    }

    private static float[] px = new float[256];
    private static float[] py = new float[256];
    private static float[] nx = new float[256];
    private static float[] ny = new float[256];
    private static int count;

    private Draw() {}

    // ================================================================== basic rects

    public static void rect(GuiRenderer r, float x, float y, float w, float h, int c) {
        if (w <= 0 || h <= 0 || Colors.a(c) == 0) return;
        r.beginTriangles();
        quad(r, x, y, x + w, y, x + w, y + h, x, y + h, c, c, c, c);
        r.endTriangles();
    }

    public static void gradientV(GuiRenderer r, float x, float y, float w, float h, int top, int bottom) {
        r.beginTriangles();
        quad(r, x, y, x + w, y, x + w, y + h, x, y + h, top, top, bottom, bottom);
        r.endTriangles();
    }

    public static void gradientH(GuiRenderer r, float x, float y, float w, float h, int left, int right) {
        r.beginTriangles();
        quad(r, x, y, x + w, y, x + w, y + h, x, y + h, left, right, right, left);
        r.endTriangles();
    }

    /** Four-corner gradient: top-left, top-right, bottom-right, bottom-left. */
    public static void gradient4(GuiRenderer r, float x, float y, float w, float h, int tl, int tr, int br, int bl) {
        r.beginTriangles();
        quad(r, x, y, x + w, y, x + w, y + h, x, y + h, tl, tr, br, bl);
        r.endTriangles();
    }

    public static void outlineRect(GuiRenderer r, float x, float y, float w, float h, float t, int c) {
        r.beginTriangles();
        quad(r, x, y, x + w, y, x + w, y + t, x, y + t, c, c, c, c);
        quad(r, x, y + h - t, x + w, y + h - t, x + w, y + h, x, y + h, c, c, c, c);
        quad(r, x, y + t, x + t, y + t, x + t, y + h - t, x, y + h - t, c, c, c, c);
        quad(r, x + w - t, y + t, x + w, y + t, x + w, y + h - t, x + w - t, y + h - t, c, c, c, c);
        r.endTriangles();
    }

    // ================================================================== rounded shapes

    public static void rounded(GuiRenderer r, float x, float y, float w, float h, float radius, final int c) {
        if (w <= 0 || h <= 0 || Colors.a(c) == 0) return;
        if (radius <= 0.01f) { rect(r, x, y, w, h, c); return; }
        if (GpuDraw.shape(r, x, y, w, h, radius, radius, radius, radius, c, c, GpuDraw.GRAD_NONE, GpuDraw.FILL, 0)) return;
        roundedContour(r, x, y, w, h, radius, radius, radius, radius);
        fillContour(r, solid(c), aa(r));
    }

    public static void rounded(GuiRenderer r, float x, float y, float w, float h, float tl, float tr, float br, float bl, int c) {
        if (w <= 0 || h <= 0 || Colors.a(c) == 0) return;
        if (GpuDraw.shape(r, x, y, w, h, tl, tr, br, bl, c, c, GpuDraw.GRAD_NONE, GpuDraw.FILL, 0)) return;
        roundedContour(r, x, y, w, h, tl, tr, br, bl);
        fillContour(r, solid(c), aa(r));
    }

    public static void roundedGradient(GuiRenderer r, final float x, final float y, final float w, final float h,
                                       float radius, final int c1, final int c2, final boolean horizontal) {
        if (w <= 0 || h <= 0) return;
        if (GpuDraw.shape(r, x, y, w, h, radius, radius, radius, radius, c1, c2,
                horizontal ? GpuDraw.GRAD_H : GpuDraw.GRAD_V, GpuDraw.FILL, 0)) return;
        roundedContour(r, x, y, w, h, radius, radius, radius, radius);
        fillContour(r, new Shader() {
            @Override
            public int color(float vx, float vy) {
                float t = horizontal ? (vx - x) / w : (vy - y) / h;
                return Colors.lerp(c1, c2, t);
            }
        }, aa(r));
    }

    public static void roundedOutline(GuiRenderer r, float x, float y, float w, float h, float radius, float thickness, int c) {
        if (w <= 0 || h <= 0 || Colors.a(c) == 0) return;
        if (GpuDraw.shape(r, x, y, w, h, radius, radius, radius, radius, c, c, GpuDraw.GRAD_NONE, GpuDraw.STROKE, thickness)) return;
        roundedContour(r, x + thickness / 2f, y + thickness / 2f, w - thickness, h - thickness,
                Math.max(0, radius - thickness / 2f), Math.max(0, radius - thickness / 2f),
                Math.max(0, radius - thickness / 2f), Math.max(0, radius - thickness / 2f));
        strokeContour(r, thickness, solid(c), aa(r), true);
    }

    public static void roundedOutlineGradient(GuiRenderer r, final float x, final float y, final float w, float h,
                                              float radius, float thickness, final int c1, final int c2) {
        if (GpuDraw.shape(r, x, y, w, h, radius, radius, radius, radius, c1, c2, GpuDraw.GRAD_H, GpuDraw.STROKE, thickness)) return;
        float half = thickness / 2f;
        float rr = Math.max(0, radius - half);
        roundedContour(r, x + half, y + half, w - thickness, h - thickness, rr, rr, rr, rr);
        strokeContour(r, thickness, new Shader() {
            @Override
            public int color(float vx, float vy) {
                return Colors.lerp(c1, c2, (vx - x) / w);
            }
        }, aa(r), true);
    }

    /** Soft drop shadow around a rounded rectangle. */
    public static void shadow(GuiRenderer r, float x, float y, float w, float h, float radius, float blur, int c) {
        if (Colors.a(c) == 0 || blur <= 0) return;
        if (GpuDraw.shape(r, x, y, w, h, radius, radius, radius, radius, c, c, GpuDraw.GRAD_NONE, GpuDraw.SHADOW, blur)) return;
        // Solid core plus an OUTWARD fade. Offsetting a convex contour outward never overlaps;
        // a wide inward offset folds over at the corners and double-blends along the diagonals.
        float inset = Math.min(2f, Math.min(w, h) / 4f);
        float rr = Math.max(0, radius - inset);
        roundedContour(r, x + inset, y + inset, w - inset * 2, h - inset * 2, rr, rr, rr, rr);
        fillContour(r, solid(c), aa(r));
        fringeOnly(r, solid(c), blur);
    }

    /** Outer glow: a shadow in the given colour that fades outward from the shape edge. */
    public static void glow(GuiRenderer r, float x, float y, float w, float h, float radius, float size, int c) {
        if (Colors.a(c) == 0 || size <= 0) return;
        if (GpuDraw.shape(r, x, y, w, h, radius, radius, radius, radius, c, c, GpuDraw.GRAD_NONE, GpuDraw.GLOW, size)) return;
        roundedContour(r, x, y, w, h, radius, radius, radius, radius);
        fringeOnly(r, solid(c), size);
    }

    public static void circle(GuiRenderer r, float cx, float cy, float radius, int c) {
        if (radius <= 0 || Colors.a(c) == 0) return;
        if (GpuDraw.shape(r, cx - radius, cy - radius, radius * 2, radius * 2, radius, radius, radius, radius,
                c, c, GpuDraw.GRAD_NONE, GpuDraw.FILL, 0)) return;
        circleContour(r, cx, cy, radius);
        fillContour(r, solid(c), aa(r));
    }

    public static void ring(GuiRenderer r, float cx, float cy, float radius, float thickness, int c) {
        if (radius <= 0 || Colors.a(c) == 0) return;
        if (GpuDraw.shape(r, cx - radius, cy - radius, radius * 2, radius * 2, radius, radius, radius, radius,
                c, c, GpuDraw.GRAD_NONE, GpuDraw.STROKE, thickness)) return;
        circleContour(r, cx, cy, radius - thickness / 2f);
        strokeContour(r, thickness, solid(c), aa(r), true);
    }

    /** Circular arc stroke; angles in degrees, 0 = up, clockwise. */
    public static void arc(GuiRenderer r, float cx, float cy, float radius, float thickness, float startDeg, float sweepDeg, int c) {
        if (Math.abs(sweepDeg) < 0.1f || Colors.a(c) == 0) return;
        if (GpuDraw.arc(r, cx, cy, radius, thickness, startDeg, sweepDeg, c)) return;
        int seg = Math.max(4, Math.min(120, (int) (Math.abs(sweepDeg) / 6f)));
        ensure(seg + 1);
        count = 0;
        for (int i = 0; i <= seg; i++) {
            double a = Math.toRadians(startDeg + sweepDeg * i / seg);
            px[count] = cx + (float) Math.sin(a) * radius;
            py[count] = cy - (float) Math.cos(a) * radius;
            count++;
        }
        computeNormalsOpen();
        strokeContour(r, thickness, solid(c), aa(r), false);
    }

    public static void line(GuiRenderer r, float x1, float y1, float x2, float y2, float width, int c) {
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1e-4f || Colors.a(c) == 0) return;
        float ox = -dy / len * width / 2f, oy = dx / len * width / 2f;
        r.beginTriangles();
        quad(r, x1 + ox, y1 + oy, x2 + ox, y2 + oy, x2 - ox, y2 - oy, x1 - ox, y1 - oy, c, c, c, c);
        r.endTriangles();
    }

    /** Open polyline through {x0,y0,x1,y1,...} with round joints. */
    public static void polyline(GuiRenderer r, float[] pts, float width, int c) {
        for (int i = 0; i + 3 < pts.length; i += 2) {
            line(r, pts[i], pts[i + 1], pts[i + 2], pts[i + 3], width, c);
        }
        for (int i = 2; i + 3 < pts.length; i += 2) {
            circle(r, pts[i], pts[i + 1], width / 2f, c);
        }
    }

    public static void triangle(GuiRenderer r, float x1, float y1, float x2, float y2, float x3, float y3, int c) {
        r.beginTriangles();
        r.vertex(x1, y1, c);
        r.vertex(x2, y2, c);
        r.vertex(x3, y3, c);
        r.endTriangles();
    }

    /** Rounded rect with a gradient in any {@link GpuDraw} GRAD_* direction (diagonal and radial need shaders). */
    public static void roundedGradient(GuiRenderer r, float x, float y, float w, float h, float radius,
                                       int c1, int c2, int gradient) {
        if (w <= 0 || h <= 0) return;
        if (GpuDraw.shape(r, x, y, w, h, radius, radius, radius, radius, c1, c2, gradient, GpuDraw.FILL, 0)) return;
        roundedGradient(r, x, y, w, h, radius, c1, c2, gradient != GpuDraw.GRAD_V);
    }

    /** Accent-gradient rounded rect with a highlight sweeping across it ({@code phase} 0..1). */
    public static void shine(GuiRenderer r, float x, float y, float w, float h, float radius, int c1, int c2, float phase) {
        if (w <= 0 || h <= 0) return;
        if (GpuDraw.shape(r, x, y, w, h, radius, radius, radius, radius, c1, c2, GpuDraw.GRAD_H, GpuDraw.FILL, 0, phase)) return;
        roundedGradient(r, x, y, w, h, radius, c1, c2, true);
    }

    /**
     * Frosted glass: blurs what's already drawn behind the shape and tints it with
     * {@code tint} (whose alpha is the tint strength). Falls back to a flat tint.
     */
    public static void glass(GuiRenderer r, float x, float y, float w, float h, float radius, int tint, float opacity) {
        if (w <= 0 || h <= 0 || opacity <= 0.003f) return;
        if (GpuDraw.glass(r, x, y, w, h, radius, tint, opacity, Theme.blur)) return;
        rounded(r, x, y, w, h, radius, Colors.fade(Colors.withAlpha(tint, Math.max(Colors.a(tint), 200)), opacity));
    }

    /**
     * Snapshots the frame for glass panels now, so shadows drawn next don't end up inside the
     * blur. Call after the screen's backdrop and before panel shadows.
     */
    public static void beginPanels(GuiRenderer r) {
        if (Theme.glass) GpuDraw.captureBackdrop(r);
    }

    /** A menu panel: frosted glass in the theme's panel colour when enabled, else flat. */
    public static void panel(GuiRenderer r, float x, float y, float w, float h, float radius, float opacity) {
        if (Theme.glass) {
            int tint = Colors.withAlpha(Theme.PANEL, Math.round(255 * Theme.uiOpacity * 0.72f));
            if (GpuDraw.glass(r, x, y, w, h, radius, tint, opacity, Theme.blur)) return;
        }
        rounded(r, x, y, w, h, radius, Colors.fade(Theme.panel(), opacity));
    }

    // ================================================================== texture

    public static void texture(GuiRenderer r, int tex, float x, float y, float w, float h,
                               float u0, float v0, float u1, float v1, int color, boolean linear) {
        r.beginTextured(tex, linear);
        r.vertexUV(x, y, u0, v0, color);
        r.vertexUV(x, y + h, u0, v1, color);
        r.vertexUV(x + w, y + h, u1, v1, color);
        r.vertexUV(x + w, y, u1, v0, color);
        r.endTextured();
    }

    // ================================================================== internals

    private static float aa(GuiRenderer r) {
        return (float) (1.0 / Math.max(1.0, r.scaleFactor()));
    }

    private static Shader solid(final int c) {
        return new Shader() {
            @Override
            public int color(float x, float y) {
                return c;
            }
        };
    }

    private static void quad(GuiRenderer r, float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4,
                             int c1, int c2, int c3, int c4) {
        r.vertex(x1, y1, c1);
        r.vertex(x2, y2, c2);
        r.vertex(x3, y3, c3);
        r.vertex(x1, y1, c1);
        r.vertex(x3, y3, c3);
        r.vertex(x4, y4, c4);
    }

    private static void ensure(int n) {
        if (px.length >= n) return;
        int size = Math.max(n, px.length * 2);
        px = new float[size];
        py = new float[size];
        nx = new float[size];
        ny = new float[size];
    }

    private static int segments(GuiRenderer r, float radius) {
        double s = radius * r.scaleFactor();
        return Math.max(2, Math.min(24, (int) Math.ceil(s * 0.6)));
    }

    /** Builds a clockwise (screen space) rounded-rect contour into px/py. */
    private static void roundedContour(GuiRenderer r, float x, float y, float w, float h,
                                       float tl, float tr, float br, float bl) {
        float max = Math.min(w, h) / 2f;
        tl = Math.min(tl, max);
        tr = Math.min(tr, max);
        br = Math.min(br, max);
        bl = Math.min(bl, max);
        ensure(4 * 26);
        count = 0;
        corner(r, x + tl, y + tl, tl, 180, 270);
        corner(r, x + w - tr, y + tr, tr, 270, 360);
        corner(r, x + w - br, y + h - br, br, 0, 90);
        corner(r, x + bl, y + h - bl, bl, 90, 180);
        computeNormalsClosed();
    }

    private static void corner(GuiRenderer r, float cx, float cy, float radius, float a0, float a1) {
        if (radius <= 0.01f) {
            px[count] = cx;
            py[count] = cy;
            count++;
            return;
        }
        int seg = segments(r, radius);
        for (int i = 0; i <= seg; i++) {
            double a = Math.toRadians(a0 + (a1 - a0) * i / seg);
            px[count] = cx + (float) Math.cos(a) * radius;
            py[count] = cy + (float) Math.sin(a) * radius;
            count++;
        }
    }

    private static void circleContour(GuiRenderer r, float cx, float cy, float radius) {
        int seg = Math.max(12, Math.min(96, (int) (radius * r.scaleFactor() * 1.2)));
        ensure(seg);
        count = 0;
        for (int i = 0; i < seg; i++) {
            double a = Math.PI * 2 * i / seg;
            px[count] = cx + (float) Math.cos(a) * radius;
            py[count] = cy + (float) Math.sin(a) * radius;
            count++;
        }
        computeNormalsClosed();
    }

    private static void computeNormalsClosed() {
        // Remove duplicate consecutive points (zero-radius corners) to keep normals stable.
        int w = 0;
        for (int i = 0; i < count; i++) {
            int prev = w == 0 ? -1 : w - 1;
            if (prev >= 0 && Math.abs(px[i] - px[prev]) < 1e-4f && Math.abs(py[i] - py[prev]) < 1e-4f) continue;
            px[w] = px[i];
            py[w] = py[i];
            w++;
        }
        if (w > 1 && Math.abs(px[0] - px[w - 1]) < 1e-4f && Math.abs(py[0] - py[w - 1]) < 1e-4f) w--;
        count = w;
        for (int i = 0; i < count; i++) {
            int p = (i - 1 + count) % count, n = (i + 1) % count;
            float e1x = px[i] - px[p], e1y = py[i] - py[p];
            float e2x = px[n] - px[i], e2y = py[n] - py[i];
            float l1 = len(e1x, e1y), l2 = len(e2x, e2y);
            // Outward normal for a clockwise contour in y-down space is (dy, -dx).
            float n1x = e1y / l1, n1y = -e1x / l1;
            float n2x = e2y / l2, n2y = -e2x / l2;
            float ax = n1x + n2x, ay = n1y + n2y;
            float al = len(ax, ay);
            if (al < 1e-4f) { ax = n2x; ay = n2y; al = 1f; }
            ax /= al;
            ay /= al;
            // Miter scale so the offset edge stays parallel; capped for sharp corners.
            float dot = ax * n2x + ay * n2y;
            float scale = dot > 0.25f ? 1f / dot : 4f;
            nx[i] = ax * scale;
            ny[i] = ay * scale;
        }
    }

    private static void computeNormalsOpen() {
        for (int i = 0; i < count; i++) {
            int p = Math.max(0, i - 1), n = Math.min(count - 1, i + 1);
            float dx = px[n] - px[p], dy = py[n] - py[p];
            float l = len(dx, dy);
            nx[i] = dy / l;
            ny[i] = -dx / l;
        }
    }

    private static float len(float x, float y) {
        float l = (float) Math.sqrt(x * x + y * y);
        return l < 1e-6f ? 1e-6f : l;
    }

    /** Fills the current contour with an anti-aliased fringe of the given width. */
    private static void fillContour(GuiRenderer r, Shader shader, float fringe) {
        if (count < 3) return;
        float half = fringe / 2f;
        float cx = 0, cy = 0;
        for (int i = 0; i < count; i++) {
            cx += px[i];
            cy += py[i];
        }
        cx /= count;
        cy /= count;
        int cc = shader.color(cx, cy);
        r.beginTriangles();
        for (int i = 0; i < count; i++) {
            int j = (i + 1) % count;
            float ix = px[i] - nx[i] * half, iy = py[i] - ny[i] * half;
            float jx = px[j] - nx[j] * half, jy = py[j] - ny[j] * half;
            int ci = shader.color(px[i], py[i]), cj = shader.color(px[j], py[j]);
            r.vertex(cx, cy, cc);
            r.vertex(ix, iy, ci);
            r.vertex(jx, jy, cj);
            float ox = px[i] + nx[i] * half, oy = py[i] + ny[i] * half;
            float qx = px[j] + nx[j] * half, qy = py[j] + ny[j] * half;
            quad(r, ix, iy, jx, jy, qx, qy, ox, oy, ci, cj, Colors.withAlpha(cj, 0), Colors.withAlpha(ci, 0));
        }
        r.endTriangles();
    }

    /** Draws only an outward fading band (used for glows). */
    private static void fringeOnly(GuiRenderer r, Shader shader, float width) {
        if (count < 3) return;
        r.beginTriangles();
        for (int i = 0; i < count; i++) {
            int j = (i + 1) % count;
            int ci = shader.color(px[i], py[i]), cj = shader.color(px[j], py[j]);
            quad(r, px[i], py[i], px[j], py[j], px[j] + nx[j] * width, py[j] + ny[j] * width,
                    px[i] + nx[i] * width, py[i] + ny[i] * width, ci, cj, Colors.withAlpha(cj, 0), Colors.withAlpha(ci, 0));
        }
        r.endTriangles();
    }

    /** Strokes the current contour centred on it with AA on both sides. */
    private static void strokeContour(GuiRenderer r, float thickness, Shader shader, float fringe, boolean closed) {
        if (count < 2) return;
        float h = thickness / 2f;
        float inner = Math.max(0f, h - fringe / 2f);
        float outer = h + fringe / 2f;
        int n = closed ? count : count - 1;
        r.beginTriangles();
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % count;
            int ci = shader.color(px[i], py[i]), cj = shader.color(px[j], py[j]);
            int ti = Colors.withAlpha(ci, 0), tj = Colors.withAlpha(cj, 0);
            // core band
            quad(r, px[i] + nx[i] * inner, py[i] + ny[i] * inner, px[j] + nx[j] * inner, py[j] + ny[j] * inner,
                    px[j] - nx[j] * inner, py[j] - ny[j] * inner, px[i] - nx[i] * inner, py[i] - ny[i] * inner,
                    ci, cj, cj, ci);
            // outer fringe
            quad(r, px[i] + nx[i] * inner, py[i] + ny[i] * inner, px[j] + nx[j] * inner, py[j] + ny[j] * inner,
                    px[j] + nx[j] * outer, py[j] + ny[j] * outer, px[i] + nx[i] * outer, py[i] + ny[i] * outer,
                    ci, cj, tj, ti);
            // inner fringe
            quad(r, px[i] - nx[i] * inner, py[i] - ny[i] * inner, px[j] - nx[j] * inner, py[j] - ny[j] * inner,
                    px[j] - nx[j] * outer, py[j] - ny[j] * outer, px[i] - nx[i] * outer, py[i] - ny[i] * outer,
                    ci, cj, tj, ti);
        }
        r.endTriangles();
    }
}
