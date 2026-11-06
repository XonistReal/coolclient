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
                default: membrane(mr, color, style == Style.DEMON, t); break;
            }
            mr.pop();
        }
        mr.end();
    }

    /**
     * Bat-like wing: an arm up to the wrist, five finger bones fanning out from it, and a membrane
     * stretched between them with a scalloped trailing edge that ripples. Demon wings are black
     * and red, ragged at the edge, with glowing veins and claws.
     */
    private void membrane(ModelRenderer mr, int color, boolean demon, float t) {
        int bone = demon ? 0xFF1A0508 : Mesh.shade(color, 0.42f);
        int boneTip = demon ? 0xFF5A0A10 : Mesh.shade(color, 0.6f);
        int root = demon ? 0xF51A0306 : Mesh.alpha(Mesh.shade(color, 0.55f), 0.97f);
        int mid = demon ? 0xF0480810 : Mesh.alpha(color, 0.95f);
        int edge = demon ? 0xE6C8202A : Mesh.alpha(Colors.lerp(color, 0xFFFFFFFF, 0.28f), 0.88f);
        int claw = demon ? 0xFFFF3A3A : 0xFFE8DCC8;
        float ex = 3.6f, ey = -5.6f, wx = 8f, wy = -9.6f;
        float[] ang = {62f, 33f, 6f, -22f, -50f};
        float[] len = {8.5f, 12.5f, 14f, 12f, 9f};
        float[] fx = new float[5], fy = new float[5], fz = new float[5];
        for (int i = 0; i < 5; i++) {
            double r = Math.toRadians(ang[i]);
            fx[i] = wx + (float) Math.cos(r) * len[i];
            fy[i] = wy - (float) Math.sin(r) * len[i];
            fz[i] = 0.5f + (float) Math.sin(t * 2.6f + i * 0.8f) * 0.35f;
        }
        // Membrane between neighbouring fingers, sagging into scallops between the tips.
        for (int i = 0; i < 4; i++) {
            scallop(mr, wx, wy, 0.5f, fx[i], fy[i], fz[i], fx[i + 1], fy[i + 1], fz[i + 1], demon ? 0.3f + 0.12f * (i % 2) : 0.24f, demon, root, mid, edge, t, i);
        }
        // The inner panel from the last finger back down to the body.
        scallop(mr, 0, 0, 0.5f, fx[4], fy[4], fz[4], 0.8f, 11f, 0.5f, demon ? 0.32f : 0.26f, demon, root, mid, edge, t, 7);
        Mesh.tri(mr, 0, 0, 0.5f, wx, wy, 0.5f, fx[4], fy[4], fz[4], root, root, mid);
        // Bones: the arm, then the fingers, each with a claw.
        Mesh.cone(mr, 0, 0, 0.5f, 0.6f, ex, ey, 0.5f, 0.48f, 6, bone, bone);
        Mesh.cone(mr, ex, ey, 0.5f, 0.48f, wx, wy, 0.5f, 0.42f, 6, bone, bone);
        for (int i = 0; i < 5; i++) {
            Mesh.cone(mr, wx, wy, 0.5f, 0.34f, fx[i], fy[i], fz[i], 0.1f, 5, bone, boneTip);
            float dx = fx[i] - wx, dy = fy[i] - wy, dl = (float) Math.sqrt(dx * dx + dy * dy);
            if (demon || i < 2) {
                Mesh.cone(mr, fx[i], fy[i], fz[i], 0.14f, fx[i] + dx / dl * 1.1f, fy[i] + dy / dl * 1.1f - 0.3f, fz[i], 0f, 4, boneTip, claw);
            }
        }
        // Thumb claw at the wrist.
        Mesh.cone(mr, wx, wy, 0.5f, 0.3f, wx - 1.3f, wy - 2.3f, 0.5f, 0f, 5, bone, claw);
        if (demon) {
            // Spines along the arm.
            for (int i = 1; i <= 3; i++) {
                float f = i / 4f;
                float sx = wx * f, sy = wy * f;
                Mesh.cone(mr, sx, sy, 0.5f, 0.22f, sx - 0.6f, sy - 1.6f, 0.5f, 0f, 4, bone, claw);
            }
        }
    }

    /** A membrane panel: a fan from (ax, ay) to two tips, its outer edge sagging inward; demon panels get ragged edges and veins. */
    private static void scallop(ModelRenderer mr, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz,
                                float sag, boolean demon, int root, int mid, int edge, float t, int seed) {
        int n = 6;
        float px = ax, py = ay;
        float mx = (bx + cx) / 2f, my = (by + cy) / 2f;
        float qx = mx + (ax - mx) * sag * 2f, qy = my + (ay - my) * sag * 2f;
        float lx = bx, ly = by, lz = bz;
        for (int i = 1; i <= n; i++) {
            float f = i / (float) n;
            // Quadratic curve from b through the sagging control point q to c.
            float u = 1 - f;
            float x = u * u * bx + 2 * u * f * qx + f * f * cx;
            float y = u * u * by + 2 * u * f * qy + f * f * cy;
            float z = u * bz + f * cz + (float) Math.sin(t * 3f + f * 3f + seed) * 0.25f;
            if (demon && i < n && ((i + seed) % 3 == 0)) {
                // A tear: the edge dips toward the root.
                x += (ax - x) * 0.18f;
                y += (ay - y) * 0.18f;
            }
            int e0 = lx == bx && ly == by ? edge : Colors.lerp(edge, mid, 0.3f);
            Mesh.tri(mr, px, py, az, lx, ly, lz, x, y, z, root, e0, Colors.lerp(edge, mid, 0.3f));
            if (demon && i % 2 == 0) {
                Mesh.beam(mr, ax, ay, az + 0.06f, x, y, z + 0.06f, 0.05f, 0x00FF2A2A, 0xCCFF3A3A);
            }
            lx = x;
            ly = y;
            lz = z;
        }
    }

    /**
     * Layered feathered wing: long primaries at the tip, secondaries along the arm and two rows of
     * short coverts on top, every feather tapered with a rounded tip and a darker quill.
     */
    private void feathers(ModelRenderer mr, int color, float alpha, float t) {
        int tipColor = Colors.lerp(color, 0xFFFFFFFF, 0.55f);
        int shadow = Mesh.shade(color, 0.72f);
        // The arm: shoulder (0, 0) up to the wrist, then out to the tip.
        float wx = 8.5f, wy = -8.5f, tx = 19f, ty = -6.5f;
        // Primaries hang from wrist to tip, fanning out.
        for (int i = 0; i < 9; i++) {
            float f = i / 8f;
            float bx = wx + (tx - wx) * f, by = wy + (ty - wy) * f;
            double a = Math.toRadians(70 - f * 55);
            float len = 13f + f * 2f - f * f * 4f;
            float wave = (float) Math.sin(t * 2.2f + i * 0.5f) * 0.6f;
            feather(mr, bx, by, 0.25f + f * 0.05f, a, len, 1.25f, wave, Mesh.alpha(shadow, alpha), Mesh.alpha(tipColor, alpha));
        }
        // Secondaries hang from the shoulder to the wrist.
        for (int i = 0; i < 8; i++) {
            float f = i / 7f;
            float bx = wx * f, by = wy * f;
            double a = Math.toRadians(98 - f * 25);
            float len = 9f + f * 3.5f;
            float wave = (float) Math.sin(t * 2.2f + i * 0.5f + 2) * 0.45f;
            feather(mr, bx, by + 1f, 0.45f, a, len, 1.35f, wave, Mesh.alpha(shadow, alpha), Mesh.alpha(Colors.lerp(color, tipColor, 0.6f), alpha));
        }
        // Coverts: two rows of short feathers over the bases.
        for (int row = 0; row < 2; row++) {
            int n = 8 + row * 3;
            for (int i = 0; i < n; i++) {
                float f = i / (float) (n - 1);
                float bx, by;
                if (f < 0.55f) {
                    float g = f / 0.55f;
                    bx = wx * g;
                    by = wy * g;
                } else {
                    float g = (f - 0.55f) / 0.45f;
                    bx = wx + (tx - wx) * g * 0.7f;
                    by = wy + (ty - wy) * g * 0.7f;
                }
                double a = Math.toRadians(90 - f * 40);
                float len = row == 0 ? 3.6f : 6.2f;
                feather(mr, bx, by, 0.65f - row * 0.1f, a, len, 1.1f, 0, Mesh.alpha(Colors.lerp(color, 0xFFFFFFFF, 0.2f + row * 0.1f), alpha),
                        Mesh.alpha(tipColor, alpha));
            }
        }
        // The leading edge.
        Mesh.beam(mr, 0, 0, 0.7f, wx, wy, 0.7f, 0.55f, Mesh.alpha(color, alpha), Mesh.alpha(tipColor, alpha));
        Mesh.beam(mr, wx, wy, 0.7f, tx * 0.82f + wx * 0.18f, ty * 0.82f + wy * 0.18f, 0.7f, 0.45f, Mesh.alpha(tipColor, alpha), Mesh.alpha(tipColor, alpha));
    }

    /** One feather from (x, y), pointing along angle a (0 = outward, 90 = straight down). */
    private static void feather(ModelRenderer mr, float x, float y, float z, double a, float len, float w, float wave, int root, int tip) {
        float dx = (float) Math.cos(a), dy = (float) Math.sin(a);
        float px = -dy, py = dx;
        float mx = x + dx * len * 0.62f, my = y + dy * len * 0.62f;
        float ex = x + dx * len, ey = y + dy * len;
        float mz = z + wave * 0.5f, ez = z + wave;
        int mid = Colors.lerp(root, tip, 0.55f);
        Mesh.quad(mr, x - px * w * 0.35f, y - py * w * 0.35f, z, mx - px * w, my - py * w, mz,
                mx + px * w, my + py * w, mz, x + px * w * 0.35f, y + py * w * 0.35f, z, root, mid, mid, root);
        Mesh.quad(mr, mx - px * w, my - py * w, mz, ex - px * w * 0.45f, ey - py * w * 0.45f, ez,
                ex + px * w * 0.45f, ey + py * w * 0.45f, ez, mx + px * w, my + py * w, mz, mid, tip, tip, mid);
        Mesh.tri(mr, ex - px * w * 0.45f, ey - py * w * 0.45f, ez, ex + dx * w * 0.6f, ey + dy * w * 0.6f, ez,
                ex + px * w * 0.45f, ey + py * w * 0.45f, ez, tip, tip, tip);
        // Quill
        Mesh.quad(mr, x - px * 0.08f, y - py * 0.08f, z + 0.03f, ex - px * 0.05f, ey - py * 0.05f, ez + 0.03f,
                ex + px * 0.05f, ey + py * 0.05f, ez + 0.03f, x + px * 0.08f, y + py * 0.08f, z + 0.03f,
                Mesh.shade(root, 0.7f), Mesh.shade(tip, 0.85f), Mesh.shade(tip, 0.85f), Mesh.shade(root, 0.7f));
    }

    /**
     * Butterfly wings: a big forewing and a rounder hindwing, dark at the body, the colour across
     * the middle, a black margin with white dots, veins, and an eye spot on the hindwing.
     */
    private void butterfly(ModelRenderer mr, int color, float t) {
        int body = Mesh.shade(color, 0.35f), light = Colors.lerp(color, 0xFFFFFFFF, 0.35f);
        float shimmer = 0.85f + 0.15f * (float) Math.sin(t * 3);
        wingPanel(mr, 0, -1, 15f, 12f, -50f, body, Colors.lerp(color, light, 0.3f * shimmer), true);
        wingPanel(mr, 0, 1, 10.5f, 9f, 40f, body, Mesh.shade(color, 0.9f), false);
        // Eye spot on the hindwing.
        double ea = Math.toRadians(40);
        float ex = (float) Math.cos(ea) * 5.5f, ey = 1 + (float) Math.sin(ea) * 5f;
        // Patterns go on both faces so they show from the front and from behind.
        for (int side = -1; side <= 1; side += 2) {
            disc(mr, ex, ey, 0.5f + side * 0.12f, 1.8f, 0xFF140A1E, 0xFF140A1E);
            disc(mr, ex, ey, 0.5f + side * 0.16f, 1.15f, light, color);
            disc(mr, ex - 0.3f, ey - 0.3f, 0.5f + side * 0.2f, 0.4f, 0xFFFFFFFF, 0xFFFFFFFF);
        }
    }

    /** One wing: a fan from the root out to an oval edge, then a black margin band dotted with white. */
    private void wingPanel(ModelRenderer mr, float cx, float cy, float rx, float ry, float tilt, int inner, int mid, boolean fore) {
        int seg = 22;
        double tr = Math.toRadians(tilt);
        float ox = cx + (float) Math.cos(tr) * rx * 0.55f, oy = cy + (float) Math.sin(tr) * ry * 0.55f;
        int margin = 0xFF120A16;
        float[] px = new float[seg + 1], py = new float[seg + 1];
        for (int i = 0; i <= seg; i++) {
            double a = Math.PI * 2 * i / seg;
            // Forewings are pointier at the far corner.
            float bulge = fore ? 1f + 0.18f * (float) Math.pow(Math.max(0, Math.cos(a - tr)), 4) : 1f;
            px[i] = ox + (float) Math.cos(a) * rx * 0.55f * bulge;
            py[i] = oy + (float) Math.sin(a) * ry * 0.55f * bulge;
        }
        for (int i = 0; i < seg; i++) {
            float ix0 = cx + (px[i] - cx) * 0.8f, iy0 = cy + (py[i] - cy) * 0.8f;
            float ix1 = cx + (px[i + 1] - cx) * 0.8f, iy1 = cy + (py[i + 1] - cy) * 0.8f;
            Mesh.tri(mr, cx, cy, 0.5f, ix0, iy0, 0.5f, ix1, iy1, 0.5f, inner, mid, mid);
            Mesh.quad(mr, ix0, iy0, 0.5f, px[i], py[i], 0.5f, px[i + 1], py[i + 1], 0.5f, ix1, iy1, 0.5f, margin);
            if (i % 2 == 0) {
                float dx = ix0 + (px[i] - ix0) * 0.5f, dy = iy0 + (py[i] - iy0) * 0.5f;
                disc(mr, dx, dy, 0.42f, 0.45f, 0xFFF4F0FF, 0xFFF4F0FF);
                disc(mr, dx, dy, 0.58f, 0.45f, 0xFFF4F0FF, 0xFFF4F0FF);
            }
            if (i % 3 == 0) Mesh.beam(mr, cx, cy, 0.55f, ix0, iy0, 0.55f, 0.08f, margin, Mesh.alpha(margin, 0.6f));
        }
    }

    private static void disc(ModelRenderer mr, float x, float y, float z, float r, int centre, int edge) {
        int n = 10;
        for (int i = 0; i < n; i++) {
            double a0 = Math.PI * 2 * i / n, a1 = Math.PI * 2 * (i + 1) / n;
            Mesh.tri(mr, x, y, z, x + (float) Math.cos(a0) * r, y + (float) Math.sin(a0) * r, z,
                    x + (float) Math.cos(a1) * r, y + (float) Math.sin(a1) * r, z, centre, edge, edge);
        }
    }
}
