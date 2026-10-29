package dev.coolclient.core.cosmetic.model;

import dev.coolclient.core.api.ModelRenderer;
import dev.coolclient.core.cosmetic.Cosmetic;
import dev.coolclient.core.cosmetic.CosmeticContext;
import dev.coolclient.core.cosmetic.CosmeticType;
import dev.coolclient.core.cosmetic.Rarity;
import dev.coolclient.core.render.Colors;

/** Flapping wings attached to the player's back. */
public final class WingsCosmetic extends Cosmetic {
    public enum Style { DRAGON, ANGEL, DEMON, BUTTERFLY, ENERGY }

    private final Style style;

    public WingsCosmetic(String id, String name, String description, Rarity rarity, Style style, int color) {
        super(id, name, description, CosmeticType.WINGS, rarity, color, true, true);
        this.style = style;
    }

    @Override
    public void render(ModelRenderer mr, CosmeticContext ctx, int color) {
        float t = ctx.seconds();
        float speed = ctx.flying ? 6f : (style == Style.BUTTERFLY ? 3.2f : 1.6f);
        float amp = ctx.flying ? 32f : 14f;
        float flap = 18f + amp * (float) (0.5 + 0.5 * Math.sin(t * speed));
        boolean emissive = style == Style.ENERGY || style == Style.DEMON;
        float back = ctx.wearingChestplate ? 3.2f : 2.4f;
        mr.begin(ctx.white, emissive);
        for (int side = 0; side < 2; side++) {
            mr.push();
            mr.translate(0, ctx.sneaking ? 3.5f : 2.5f, back);
            if (side == 1) mr.scale(-1, 1, 1);
            mr.translate(1.2f, 0, 0);
            mr.rotate(-flap, 0, 1, 0);
            mr.rotate(-12f, 0, 0, 1);
            switch (style) {
                case ANGEL: feathers(mr, color, 1f, t); break;
                case ENERGY: feathers(mr, color, 0.72f + 0.2f * (float) Math.sin(t * 3), t); break;
                case BUTTERFLY: butterfly(mr, color, t); break;
                default: membrane(mr, color, style == Style.DEMON); break;
            }
            mr.pop();
        }
        mr.end();
    }

    private void membrane(ModelRenderer mr, int color, boolean demon) {
        float[] angles = {78f, 52f, 28f, 6f, -14f};
        float[] lengths = {15f, 19f, 18f, 15f, 11f};
        int bone = demon ? 0xFF1A0508 : Mesh.shade(color, 0.45f);
        int root = demon ? 0xEE3A0610 : Mesh.alpha(Mesh.shade(color, 0.75f), 0.95f);
        int tip = demon ? 0xDDFF2A2A : Mesh.alpha(color, 0.9f);
        float[] tx = new float[angles.length], ty = new float[angles.length];
        for (int i = 0; i < angles.length; i++) {
            double a = Math.toRadians(angles[i]);
            tx[i] = (float) Math.cos(a) * lengths[i];
            ty[i] = -(float) Math.sin(a) * lengths[i];
        }
        for (int i = 0; i + 1 < angles.length; i++) {
            float sx = (tx[i] + tx[i + 1]) * 0.5f * 0.72f, sy = (ty[i] + ty[i + 1]) * 0.5f * 0.72f;
            Mesh.quad(mr, 0, 0, 0, tx[i], ty[i], 0.6f, sx, sy, 0.3f, tx[i + 1], ty[i + 1], 0.6f, root, tip, Colors.lerp(root, tip, 0.6f), tip);
        }
        for (int i = 0; i < angles.length; i++) {
            Mesh.beam(mr, 0, 0, 0, tx[i], ty[i], 0.6f, 0.35f, bone, bone);
            if (demon) {
                // Claw at each finger tip.
                Mesh.beam(mr, tx[i], ty[i], 0.6f, tx[i] * 1.12f, ty[i] * 1.12f - 0.8f, 0.6f, 0.2f, bone, 0xFFFF3A3A);
            }
        }
        Mesh.beam(mr, 0, 0, 0, tx[0] * 0.6f, ty[0] * 0.6f - 2f, 0.2f, 0.55f, bone, bone);
    }

    private void feathers(ModelRenderer mr, int color, float alpha, float t) {
        int[] rows = {6, 7, 8};
        float[] len = {6f, 11f, 17f};
        float[] inner = {0f, 3f, 6f};
        for (int row = 0; row < 3; row++) {
            int n = rows[row];
            for (int i = 0; i < n; i++) {
                float f = i / (float) (n - 1);
                double a = Math.toRadians(85 - f * 105);
                float base = inner[row] + f * 4f;
                float bx = (float) Math.cos(a) * base * 0.7f + f * 8f;
                float by = -(float) Math.sin(a) * base * 0.4f - (1f - f) * 6f;
                float l = len[row] * (0.75f + 0.25f * (float) Math.sin(f * Math.PI));
                float wave = (float) Math.sin(t * 2 + i * 0.6f + row) * 0.35f;
                float ex = bx + (float) Math.cos(a - 0.5) * l * 0.55f, ey = by + l * 0.85f;
                float z = 0.4f + row * 0.15f + wave * 0.2f;
                int cRoot = Mesh.alpha(Mesh.shade(color, 0.92f - row * 0.06f), alpha);
                int cTip = Mesh.alpha(Colors.lerp(color, 0xFF9FB6FF, 0.25f + row * 0.1f), alpha * 0.95f);
                float w = 1.4f + row * 0.3f;
                Mesh.quad(mr, bx - w, by, z, ex - w * 0.4f, ey, z + 0.2f, ex + w * 0.4f, ey + 0.4f, z + 0.2f, bx + w, by, z,
                        cRoot, cTip, cTip, cRoot);
            }
        }
    }

    private void butterfly(ModelRenderer mr, int color, float t) {
        int c2 = Colors.lerp(color, 0xFFFFFFFF, 0.35f);
        lobe(mr, 0, -1, 13f, 11f, 18, -55f, color, c2);
        lobe(mr, 0, 1, 9f, 7f, 14, 35f, Mesh.shade(color, 0.85f), c2);
        // Spots
        float pulse = 0.8f + 0.2f * (float) Math.sin(t * 2);
        lobe(mr, 6.5f, -6.5f, 2.2f, 2.2f, 10, 0, Mesh.alpha(0xFFFFFFFF, pulse), Mesh.alpha(0xFFFFFFFF, 0.4f));
    }

    private void lobe(ModelRenderer mr, float cx, float cy, float rx, float ry, int seg, float tilt, int inner, int outer) {
        double tr = Math.toRadians(tilt);
        float ox = cx + (float) Math.cos(tr) * rx * 0.6f, oy = cy + (float) Math.sin(tr) * ry * 0.6f;
        for (int i = 0; i < seg; i++) {
            double a0 = Math.PI * 2 * i / seg, a1 = Math.PI * 2 * (i + 1) / seg;
            float x0 = ox + (float) Math.cos(a0) * rx * 0.6f, y0 = oy + (float) Math.sin(a0) * ry * 0.6f;
            float x1 = ox + (float) Math.cos(a1) * rx * 0.6f, y1 = oy + (float) Math.sin(a1) * ry * 0.6f;
            Mesh.tri(mr, ox, oy, 0.5f, x0, y0, 0.5f, x1, y1, 0.5f, inner, outer, outer);
        }
    }
}
