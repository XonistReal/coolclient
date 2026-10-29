package dev.coolclient.v1_21_4;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import dev.coolclient.core.modules.visual.HandChams;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * Hand Chams for 1.21: wraps the buffer source used for YOUR first-person arm/item only and
 * recolours, flattens, makes glowing, or outlines (wireframe) the geometry written through it.
 */
final class ChamsBuffers implements MultiBufferSource {
    private final MultiBufferSource delegate;
    private final HandChams chams;
    private final int color;

    ChamsBuffers(MultiBufferSource delegate, HandChams chams, int color) {
        this.delegate = delegate;
        this.chams = chams;
        this.color = color;
    }

    private static ResourceLocation white() {
        return Platform21.TEXTURES.location(Platform21.TEXTURES.white());
    }

    @Override
    public VertexConsumer getBuffer(RenderType type) {
        if (chams.wireframe()) {
            VertexConsumer lines = new LineConsumer(delegate.getBuffer(RenderType.lines()), color | 0xFF000000);
            VertexConsumer fill = new TintConsumer(delegate.getBuffer(type), color, false, false);
            return VertexMultiConsumer.create(lines, fill);
        }
        if (chams.glow()) {
            return new TintConsumer(delegate.getBuffer(RenderType.entityTranslucentEmissive(white())), color, true, true);
        }
        if (!chams.textured()) {
            return new TintConsumer(delegate.getBuffer(RenderType.entityTranslucent(white())), color, true, false);
        }
        return new TintConsumer(delegate.getBuffer(type), color, false, false);
    }

    /** Multiplies (or replaces) vertex colours; optionally forces full brightness. */
    static final class TintConsumer implements VertexConsumer {
        private final VertexConsumer d;
        private final int cr, cg, cb, ca;
        private final boolean replace;
        private final boolean fullbright;

        TintConsumer(VertexConsumer d, int argb, boolean replace, boolean fullbright) {
            this.d = d;
            this.ca = (argb >>> 24) & 0xFF;
            this.cr = (argb >> 16) & 0xFF;
            this.cg = (argb >> 8) & 0xFF;
            this.cb = argb & 0xFF;
            this.replace = replace;
            this.fullbright = fullbright;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            d.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            if (replace) d.setColor(cr, cg, cb, ca);
            else d.setColor(r * cr / 255, g * cg / 255, b * cb / 255, a * ca / 255);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            d.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            d.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            if (fullbright) d.setUv2(240, 240); else d.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            d.setNormal(x, y, z);
            return this;
        }
    }

    /** Turns incoming quads into their four edges on a lines buffer. */
    static final class LineConsumer implements VertexConsumer {
        private final VertexConsumer lines;
        private final int color;
        private final float[] q = new float[12];
        private int n;

        LineConsumer(VertexConsumer lines, int color) {
            this.lines = lines;
            this.color = color;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            q[n * 3] = x;
            q[n * 3 + 1] = y;
            q[n * 3 + 2] = z;
            n++;
            if (n == 4) {
                for (int i = 0; i < 4; i++) edge(i, (i + 1) % 4);
                n = 0;
            }
            return this;
        }

        private void edge(int a, int b) {
            float dx = q[b * 3] - q[a * 3], dy = q[b * 3 + 1] - q[a * 3 + 1], dz = q[b * 3 + 2] - q[a * 3 + 2];
            float l = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (l < 1e-6f) return;
            dx /= l;
            dy /= l;
            dz /= l;
            lines.addVertex(q[a * 3], q[a * 3 + 1], q[a * 3 + 2]).setColor(color).setNormal(dx, dy, dz);
            lines.addVertex(q[b * 3], q[b * 3 + 1], q[b * 3 + 2]).setColor(color).setNormal(dx, dy, dz);
        }

        @Override public VertexConsumer setColor(int r, int g, int b, int a) { return this; }

        @Override public VertexConsumer setUv(float u, float v) { return this; }

        @Override public VertexConsumer setUv1(int u, int v) { return this; }

        @Override public VertexConsumer setUv2(int u, int v) { return this; }

        @Override public VertexConsumer setNormal(float x, float y, float z) { return this; }
    }
}
