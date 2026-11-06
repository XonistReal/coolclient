package dev.coolclient.core.cosmetic.model;

import dev.coolclient.core.api.ModelRenderer;
import dev.coolclient.core.cosmetic.Cosmetic;
import dev.coolclient.core.cosmetic.CosmeticContext;
import dev.coolclient.core.cosmetic.CosmeticType;
import dev.coolclient.core.cosmetic.Rarity;
import dev.coolclient.core.cosmetic.Unlock;
import dev.coolclient.core.render.Colors;

/** Head-attached cosmetics. Head space: x -4..4, y -8..0 (top at -8), z -4..4, face at -z. */
public final class HeadCosmetics {
    private HeadCosmetics() {}

    private static float top(CosmeticContext ctx) {
        return ctx.wearingHelmet ? -9.2f : -8f;
    }

    private static float frac(float v) { return v - (float) Math.floor(v); }

    public static final class Halo extends Cosmetic {
        public Halo(String id, String name, Rarity rarity, int color) {
            super(id, name, "A glowing ring floating above your head.", CosmeticType.HALO, rarity, color, true, true);
        }

        @Override
        public float headroom() { return 5.5f; }

        @Override
        public void render(ModelRenderer mr, CosmeticContext ctx, int color) {
            float t = ctx.seconds();
            mr.begin(ctx.white, true);
            mr.push();
            mr.translate(0, top(ctx) - 3.4f + (float) Math.sin(t * 2.1) * 0.35f, 0);
            // Tilted back a little so the ring reads as a ring from eye level, not a line; it wobbles gently.
            mr.rotate(-14f + (float) Math.sin(t * 1.3) * 3f, 1, 0, 0);
            mr.rotate((float) Math.sin(t * 0.9) * 3f, 0, 0, 1);
            mr.rotate(t * 25f, 0, 1, 0);
            int bright = Colors.lerp(color, 0xFFFFFFFF, 0.55f);
            Mesh.torus(mr, 4.4f, 0.55f, 40, 8, color);
            Mesh.torus(mr, 3.75f, 0.16f, 40, 5, bright);
            Mesh.torus(mr, 4.4f, 1.15f, 36, 8, Mesh.alpha(color, 0.16f));
            Mesh.torus(mr, 4.4f, 1.9f, 36, 8, Mesh.alpha(color, 0.06f));
            // Little stars riding the ring, each twinkling in turn.
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                float tw = (float) Math.pow(0.5 + 0.5 * Math.sin(t * 3 + i * 1.7), 3);
                float x = (float) Math.cos(a) * 4.4f, z = (float) Math.sin(a) * 4.4f;
                AuraCosmetic.star(mr, x, -0.2f, z, 0.5f + tw * 0.9f, t * 90f + i * 30f, Mesh.alpha(bright, 0.5f + tw * 0.5f), 0xFFFFFFFF);
                if (tw > 0.2f) AuraCosmetic.glow(mr, x, -0.2f, z, 1.6f * tw, Mesh.alpha(color, 0.25f * tw));
            }
            mr.pop();
            mr.end();
        }
    }

    /** A gold crown: a jewelled band with rims, eight points tipped with pearls, two arches and an orb over a velvet cap. */
    public static final class Crown extends Cosmetic {
        public Crown(String id, String name, Rarity rarity, int color) {
            super(id, name, "Rule the server.", CosmeticType.HAT, rarity, color, true, true);
        }

        @Override
        public float headroom() { return 8.8f; }

        @Override
        public void render(ModelRenderer mr, CosmeticContext ctx, int color) {
            float y = top(ctx), t = ctx.seconds();
            int dark = Mesh.shade(color, 0.62f), light = Colors.lerp(color, 0xFFFFFFFF, 0.35f);
            float r = 4.6f;
            int n = 28;
            mr.begin(ctx.white, false);
            // Velvet cap inside the band.
            Mesh.ellipsoid(mr, 0, y - 1.4f, 0, 4.2f, 3.1f, 4.2f, 16, 7, 0xFF8E1630);
            // Band and its rims.
            Mesh.tube(mr, y + 0.3f, r, y - 2.1f, r, n, dark, color);
            mr.push();
            mr.translate(0, y + 0.2f, 0);
            Mesh.torus(mr, r, 0.36f, n, 6, color);
            mr.translate(0, -2.3f, 0);
            Mesh.torus(mr, r, 0.3f, n, 6, light);
            mr.pop();
            // Points with pearls on top.
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8, w = Math.PI / 8 * 0.8;
                float x0 = (float) Math.cos(a - w) * r, z0 = (float) Math.sin(a - w) * r;
                float x1 = (float) Math.cos(a + w) * r, z1 = (float) Math.sin(a + w) * r;
                float xm = (float) Math.cos(a) * r, zm = (float) Math.sin(a) * r;
                Mesh.tri(mr, x0, y - 2.1f, z0, xm, y - 4.3f, zm, x1, y - 2.1f, z1, color, light, color);
                Mesh.ellipsoid(mr, xm, y - 4.5f, zm, 0.42f, 0.42f, 0.42f, 8, 5, 0xFFF4F0E6);
            }
            // Two arches over the top, meeting under an orb.
            for (int k = 0; k < 4; k++) {
                double a = Math.PI / 4 + Math.PI / 2 * k;
                float bx = (float) Math.cos(a) * r, bz = (float) Math.sin(a) * r;
                float px = bx, py = y - 2.1f, pz = bz;
                for (int seg = 1; seg <= 6; seg++) {
                    float f = seg / 6f;
                    float nx = bx * (1 - f), nz = bz * (1 - f);
                    float ny = y - 2.1f - (float) Math.sin(f * Math.PI / 2) * 4.2f;
                    Mesh.cone(mr, px, py, pz, 0.3f, nx, ny, nz, 0.3f, 5, color, light);
                    px = nx;
                    py = ny;
                    pz = nz;
                }
            }
            Mesh.ellipsoid(mr, 0, y - 6.9f, 0, 0.85f, 0.85f, 0.85f, 10, 6, color);
            Mesh.cone(mr, 0, y - 7.6f, 0, 0.2f, 0, y - 8.6f, 0, 0.2f, 4, light, light);
            Mesh.cone(mr, -0.5f, y - 8.2f, 0, 0.18f, 0.5f, y - 8.2f, 0, 0.18f, 4, light, light);
            // Jewels around the band.
            int[] gems = {0xFFE0213C, 0xFF2F7BFF, 0xFF2BC46A, 0xFF2F7BFF};
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8;
                Mesh.ellipsoid(mr, (float) Math.cos(a) * (r + 0.3f), y - 0.95f, (float) Math.sin(a) * (r + 0.3f), 0.5f, 0.6f, 0.5f, 8, 5, gems[i % 4]);
            }
            mr.end();
            // Now and then a jewel catches the light.
            mr.begin(ctx.white, true);
            int g = (int) (t * 0.7f) % 8;
            float tw = (float) Math.pow(Math.max(0, Math.sin((t * 0.7f % 1f) * Math.PI)), 3);
            double a = Math.PI * 2 * g / 8;
            AuraCosmetic.star(mr, (float) Math.cos(a) * (r + 0.7f), y - 1.1f, (float) Math.sin(a) * (r + 0.7f), 1.3f * tw, t * 120f, Mesh.alpha(0xFFFFFFFF, tw), 0xFFFFFFFF);
            mr.end();
        }
    }

    /** A tall felt top hat, slightly flared, its brim curled up at the sides, with a satin band and a bow. */
    public static final class TopHat extends Cosmetic {
        public TopHat(String id, String name, Rarity rarity, int color) {
            super(id, name, "Classy.", CosmeticType.HAT, rarity, color, true, false);
        }

        @Override
        public float headroom() { return 8.2f; }

        @Override
        public void render(ModelRenderer mr, CosmeticContext ctx, int color) {
            float y = top(ctx);
            int felt = 0xFF17181F, feltTop = 0xFF2A2C36, under = 0xFF0E0F14;
            int n = 28;
            mr.begin(ctx.white, false);
            // Brim: curls up toward the sides (x), dips a little front and back.
            float rin = 3.9f, rout = 6.6f;
            for (int i = 0; i < n; i++) {
                double a0 = Math.PI * 2 * i / n, a1 = Math.PI * 2 * (i + 1) / n;
                float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
                float l0 = 1.1f * c0 * c0, l1 = 1.1f * c1 * c1;
                Mesh.quad(mr, c0 * rin, y - 0.45f, s0 * rin, c0 * rout, y - 0.45f - l0, s0 * rout,
                        c1 * rout, y - 0.45f - l1, s1 * rout, c1 * rin, y - 0.45f, s1 * rin, felt, Mesh.shade(felt, 1.3f), Mesh.shade(felt, 1.3f), felt);
                Mesh.quad(mr, c0 * rin, y, s0 * rin, c0 * rout, y - l0, s0 * rout,
                        c1 * rout, y - l1, s1 * rout, c1 * rin, y, s1 * rin, under);
                Mesh.quad(mr, c0 * rout, y - l0, s0 * rout, c0 * rout, y - 0.45f - l0, s0 * rout,
                        c1 * rout, y - 0.45f - l1, s1 * rout, c1 * rout, y - l1, s1 * rout, Mesh.shade(felt, 1.4f));
            }
            // Crown, flaring out a touch toward the top, with a flat lid.
            Mesh.tube(mr, y - 0.45f, 3.75f, y - 8f, 4.1f, n, felt, feltTop);
            Mesh.ring(mr, y - 8f, 0, 4.1f, n, feltTop);
            mr.push();
            mr.translate(0, y - 8f, 0);
            Mesh.torus(mr, 4.05f, 0.18f, n, 5, Mesh.shade(feltTop, 1.3f));
            mr.pop();
            // Satin band with a sheen, and a bow on the left side (+x).
            int sheen = Colors.lerp(color, 0xFFFFFFFF, 0.3f);
            Mesh.tube(mr, y - 0.5f, 3.83f, y - 2.4f, 3.88f, n, color, sheen);
            float bx = 3.95f, by = y - 1.45f;
            Mesh.box(mr, bx - 0.1f, by - 0.5f, -0.5f, bx + 0.5f, by + 0.5f, 0.5f, Mesh.shade(color, 0.85f));
            for (int side = -1; side <= 1; side += 2) {
                Mesh.quad(mr, bx + 0.3f, by - 0.3f, side * 0.4f, bx + 0.5f, by - 1.2f, side * 2.0f,
                        bx + 0.5f, by + 1.2f, side * 2.0f, bx + 0.3f, by + 0.3f, side * 0.4f, Mesh.shade(color, 0.8f), sheen, color, Mesh.shade(color, 0.8f));
            }
            mr.end();
        }
    }

    /** A crooked witch hat: a wavy, drooping brim, a bent cone, a band with a buckle and a little star charm on the tip. */
    public static final class WitchHat extends Cosmetic {
        public WitchHat(String id, String name, Rarity rarity, int color) {
            super(id, name, "Bubble, bubble.", CosmeticType.HAT, rarity, color, true, true);
        }

        @Override
        public float headroom() { return 10.5f; }

        @Override
        public void render(ModelRenderer mr, CosmeticContext ctx, int color) {
            float y = top(ctx), t = ctx.seconds();
            int body = Mesh.shade(color, 0.48f), bodyLight = Mesh.shade(color, 0.7f);
            int n = 24;
            mr.begin(ctx.white, false);
            // Brim: wavy and drooping a bit at the edge.
            for (int i = 0; i < n; i++) {
                double a0 = Math.PI * 2 * i / n, a1 = Math.PI * 2 * (i + 1) / n;
                float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
                float r0 = 7.6f + 0.35f * (float) Math.sin(a0 * 5), r1 = 7.6f + 0.35f * (float) Math.sin(a1 * 5);
                float d0 = 0.7f + 0.45f * (float) Math.sin(a0 * 3 + 1), d1 = 0.7f + 0.45f * (float) Math.sin(a1 * 3 + 1);
                Mesh.quad(mr, c0 * 3.7f, y + 0.1f, s0 * 3.7f, c0 * r0, y + 0.1f + d0, s0 * r0,
                        c1 * r1, y + 0.1f + d1, s1 * r1, c1 * 3.7f, y + 0.1f, s1 * 3.7f, body, bodyLight, bodyLight, body);
            }
            // Cone in seven rings, leaning back more and more toward a floppy tip.
            float[] rad = {3.9f, 3.3f, 2.7f, 2.05f, 1.45f, 0.9f, 0.45f, 0.05f};
            float[] hy = {0f, -2f, -3.9f, -5.6f, -7f, -8.1f, -8.9f, -9.4f};
            float[] lean = {0f, 0.2f, 0.6f, 1.2f, 2.0f, 3.0f, 4.1f, 5.2f};
            for (int k = 0; k < 7; k++) {
                for (int i = 0; i < n; i++) {
                    double a0 = Math.PI * 2 * i / n, a1 = Math.PI * 2 * (i + 1) / n;
                    float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
                    int col = Mesh.shade(body, 0.95f + k * 0.07f + 0.1f * c0);
                    Mesh.quad(mr, c0 * rad[k], y + hy[k], s0 * rad[k] + lean[k], c0 * rad[k + 1], y + hy[k + 1], s0 * rad[k + 1] + lean[k + 1],
                            c1 * rad[k + 1], y + hy[k + 1], s1 * rad[k + 1] + lean[k + 1], c1 * rad[k], y + hy[k], s1 * rad[k] + lean[k], col);
                }
            }
            // Band with a gold buckle at the front (-z).
            Mesh.tube(mr, y - 0.2f, 3.95f, y - 1.5f, 3.65f, n, color, Colors.lerp(color, 0xFFFFFFFF, 0.2f));
            int gold = 0xFFE9B949;
            float bz1 = -4.15f, bz2 = -3.85f, cy = y - 0.85f;
            Mesh.box(mr, -1.0f, cy - 0.75f, bz1, 1.0f, cy - 0.45f, bz2, gold);
            Mesh.box(mr, -1.0f, cy + 0.45f, bz1, 1.0f, cy + 0.75f, bz2, gold);
            Mesh.box(mr, -1.0f, cy - 0.75f, bz1, -0.7f, cy + 0.75f, bz2, gold);
            Mesh.box(mr, 0.7f, cy - 0.75f, bz1, 1.0f, cy + 0.75f, bz2, gold);
            // A thread from the tip down to the charm.
            float tipX = 0, tipY = y - 9.4f, tipZ = 5.2f;
            float sway = (float) Math.sin(t * 1.8f) * 0.6f;
            float chX = tipX + sway, chY = tipY + 2.4f, chZ = tipZ + 0.4f;
            Mesh.beam(mr, tipX, tipY, tipZ, chX, chY, chZ, 0.05f, 0xFFDDD6F0, 0xFFDDD6F0);
            mr.end();
            mr.begin(ctx.white, true);
            float tw = 0.75f + 0.25f * (float) Math.sin(t * 3f);
            AuraCosmetic.star(mr, chX, chY + 0.5f, chZ, 0.75f, t * 40f, Mesh.alpha(0xFFFFE27A, 1f), 0xFFFFFFFF);
            AuraCosmetic.glow(mr, chX, chY + 0.5f, chZ, 1.6f, Mesh.alpha(0xFFFFE27A, 0.3f * tw));
            mr.end();
        }
    }

    /** Cat ears with some depth, fluffy insides, and a twitch every few seconds. */
    public static final class CatEars extends Cosmetic {
        public CatEars(String id, String name, Rarity rarity, int color) {
            super(id, name, "Nya.", CosmeticType.HAT, rarity, color, true, true);
        }

        @Override
        public float headroom() { return 4.4f; }

        @Override
        public void render(ModelRenderer mr, CosmeticContext ctx, int color) {
            float y = top(ctx), t = ctx.seconds();
            int inner = 0xFFFFB3C7, fur = 0xFFFFF4F7, back = Mesh.shade(color, 0.8f);
            mr.begin(ctx.white, false);
            for (int s = -1; s <= 1; s += 2) {
                // Each ear twitches on its own beat.
                float beat = (t + (s > 0 ? 1.7f : 0f)) % 4.3f;
                float twitch = beat < 0.3f ? (float) Math.sin(beat / 0.3f * Math.PI * 2) * 14f : 0f;
                mr.push();
                mr.translate(s * 2.6f, y + 0.1f, -0.4f);
                mr.rotate(s * 12f + s * twitch, 0, 0, 1);
                float bw = 1.8f, h = 4.1f, d = 0.45f;
                float tipX = s * 0.35f;
                // Front and back faces, then the two outer sides joining them.
                Mesh.tri(mr, -bw, 0, -d, tipX, -h, -d * 0.3f, bw, 0, -d, Mesh.shade(color, 1.1f), color, Mesh.shade(color, 1.1f));
                Mesh.tri(mr, -bw, 0, d, tipX, -h, d * 0.3f, bw, 0, d, back, back, back);
                Mesh.quad(mr, -bw, 0, -d, tipX, -h, -d * 0.3f, tipX, -h, d * 0.3f, -bw, 0, d, Mesh.shade(color, 0.9f));
                Mesh.quad(mr, bw, 0, -d, tipX, -h, -d * 0.3f, tipX, -h, d * 0.3f, bw, 0, d, Mesh.shade(color, 0.9f));
                // Inside: pink, with a few tufts of fur.
                Mesh.tri(mr, -bw * 0.62f, -0.15f, -d - 0.05f, tipX * 0.8f, -h * 0.74f, -d * 0.5f - 0.05f, bw * 0.62f, -0.15f, -d - 0.05f, inner, inner, inner);
                for (int k = -1; k <= 1; k++) {
                    float fx = k * 0.55f;
                    Mesh.tri(mr, fx - 0.3f, -0.2f, -d - 0.1f, fx + s * 0.25f, -1.7f - Math.abs(k) * 0.2f, -d - 0.1f, fx + 0.3f, -0.2f, -d - 0.1f, fur, fur, fur);
                }
                mr.pop();
            }
            mr.end();
        }
    }

    /** Curling horns: round, ridged, dark at the base and fading to bone at the tip. */
    public static final class Horns extends Cosmetic {
        public Horns(String id, String name, Rarity rarity, int color) {
            super(id, name, "Curved demon horns.", CosmeticType.HAT, rarity, color, true, false);
        }

        @Override
        public float headroom() { return 5.2f; }

        @Override
        public void render(ModelRenderer mr, CosmeticContext ctx, int color) {
            float y = top(ctx) + 0.6f;
            int boneCol = 0xFFF0E4CC;
            mr.begin(ctx.white, false);
            for (int s = -1; s <= 1; s += 2) {
                float px = s * 2.9f, py = y, pz = -1.4f;
                float dx = s * 0.55f, dy = -1.0f, dz = 0.12f;
                float th = 1.15f;
                int segs = 12;
                for (int i = 0; i < segs; i++) {
                    float nx = px + dx, ny = py + dy, nz = pz + dz;
                    float f0 = i / (float) segs, f1 = (i + 1) / (float) segs;
                    int c1 = Colors.lerp(color, boneCol, f0 * f0), c2 = Colors.lerp(color, boneCol, f1 * f1);
                    float th2 = th * 0.86f;
                    // Every other ring is a touch wider: the ridges.
                    float ridge = i % 2 == 0 ? 1.08f : 1f;
                    Mesh.cone(mr, px, py, pz, th * ridge, nx, ny, nz, th2, 8, c1, c2);
                    px = nx;
                    py = ny;
                    pz = nz;
                    // Curl up, then back, then out.
                    dy += 0.13f;
                    dz += 0.2f;
                    dx *= 0.92f;
                    th = th2;
                }
                Mesh.cone(mr, px, py, pz, th, px + dx * 0.9f, py + dy * 0.9f, pz + dz * 0.9f, 0f, 8, boneCol, boneCol);
            }
            mr.end();
        }
    }

    /** Thanksgiving: a tall buckled hat with a wide brim. */
    public static final class PilgrimHat extends Cosmetic {
        public PilgrimHat(String id, String name, Rarity rarity, int color) {
            super(id, name, "Buckle up, dinner's ready.", CosmeticType.HAT, rarity, color, true, false, Unlock.FREE);
        }

        @Override
        public float headroom() { return 8f; }

        @Override
        public void render(ModelRenderer mr, CosmeticContext ctx, int color) {
            float y = top(ctx);
            int n = 20;
            int band = Mesh.shade(color, 0.45f);
            mr.begin(ctx.white, false);
            // Brim
            Mesh.ring(mr, y - 0.5f, 0, 7.2f, n, color);
            Mesh.ring(mr, y, 0, 7.2f, n, Mesh.shade(color, 0.6f));
            Mesh.tube(mr, y, 7.2f, y - 0.5f, 7.2f, n, Mesh.shade(color, 0.8f), Mesh.shade(color, 0.8f));
            // Crown, a little narrower at the top, and its band
            Mesh.tube(mr, y - 0.5f, 4.3f, y - 7.4f, 3.7f, n, color, Mesh.shade(color, 1.15f));
            Mesh.ring(mr, y - 7.4f, 0, 3.7f, n, Mesh.shade(color, 1.2f));
            Mesh.tube(mr, y - 0.5f, 4.45f, y - 2.3f, 4.3f, n, band, band);
            // Gold buckle on the front (-z)
            int gold = 0xFFF2C94C;
            float cy = y - 1.4f, ow = 1.3f, oh = 1.05f, bar = 0.4f, z1 = -4.75f, z2 = -4.35f;
            Mesh.box(mr, -ow, cy - oh, z1, ow, cy - oh + bar, z2, gold);
            Mesh.box(mr, -ow, cy + oh - bar, z1, ow, cy + oh, z2, gold);
            Mesh.box(mr, -ow, cy - oh, z1, -ow + bar, cy + oh, z2, gold);
            Mesh.box(mr, ow - bar, cy - oh, z1, ow, cy + oh, z2, gold);
            mr.end();
        }
    }

    /** Thanksgiving: a little turkey sitting on your head. It gobbles every few seconds. */
    public static final class TurkeyHat extends Cosmetic {
        private static final int[] FEATHERS = {0xFFB8321F, 0xFFE0731F, 0xFFF2B33A, 0xFF9A5426};

        public TurkeyHat(String id, String name, Rarity rarity, int color) {
            super(id, name, "Gobble gobble.", CosmeticType.HAT, rarity, color, true, true, Unlock.FREE);
        }

        @Override
        public float headroom() { return 9.6f; }

        @Override
        public void render(ModelRenderer mr, CosmeticContext ctx, int color) {
            float t = ctx.seconds();
            float y = top(ctx);
            float ph = frac(t / 5f);
            float gobble = ph < 0.3f ? (float) Math.sin(ph / 0.3f * Math.PI * 4) * 0.35f : 0f;
            float idle = (float) Math.sin(t * 1.8f) * 0.12f;
            mr.begin(ctx.white, false);
            // Tail fan behind the body (+z), standing up.
            int n = 9;
            float sway = (float) Math.sin(t * 1.3f) * 4f;
            for (int i = 0; i < n; i++) {
                double a = Math.toRadians(-78 + 156.0 * i / (n - 1) + sway);
                float len = 6.9f - Math.abs(i - (n - 1) / 2f) * 0.35f;
                feather(mr, 0, y - 2.2f, 2.7f, (float) a, len, FEATHERS[Math.abs(i - (n - 1) / 2) % FEATHERS.length]);
            }
            // Body and wings
            Mesh.ellipsoid(mr, 0, y - 2.5f, 0.5f, 3.4f, 2.5f, 3.6f, 14, 8, color);
            for (int s = -1; s <= 1; s += 2) {
                Mesh.ellipsoid(mr, s * 3.1f, y - 2.6f, 0.9f, 0.9f, 1.7f, 2.6f, 10, 6, Mesh.shade(color, 0.8f));
            }
            // Neck and head, with beak, wattle and eyes; the head faces forward (-z).
            int skin = 0xFFC98B55;
            Mesh.ellipsoid(mr, 0, y - 4.6f + idle * 0.5f, -2.3f, 1.0f, 1.8f, 1.0f, 8, 6, skin);
            float hy = y - 6.6f + gobble + idle, hz = -2.8f;
            Mesh.ellipsoid(mr, 0, hy, hz, 1.25f, 1.2f, 1.3f, 10, 6, skin);
            Mesh.box(mr, -0.35f, hy - 0.15f, hz - 1.95f, 0.35f, hy + 0.4f, hz - 1.15f, 0xFFF5A623);
            float swing = (float) Math.sin(t * 3f) * 0.12f + gobble * 0.4f;
            Mesh.ellipsoid(mr, 0.3f + swing, hy + 1.05f, hz - 1.3f, 0.32f, 0.85f, 0.32f, 6, 5, 0xFFD7192A);
            for (int s = -1; s <= 1; s += 2) {
                Mesh.box(mr, s * 0.62f - 0.2f, hy - 0.6f, hz - 1.1f, s * 0.62f + 0.2f, hy - 0.2f, hz - 0.9f, 0xFF111111);
            }
            mr.end();
        }

        /** One flat tail feather from (x, y, z), fanned out by angle a (0 = straight up), leaning back. */
        private static void feather(ModelRenderer mr, float x, float y, float z, float a, float len, int col) {
            float dx = (float) Math.sin(a), dy = -(float) Math.cos(a), dz = 0.35f;
            float wx = (float) Math.cos(a), wy = (float) Math.sin(a);
            float[] at = {0f, 0.72f, 0.84f, 1f};
            float[] half = {0.3f, 1.05f, 1.12f, 1.0f};
            int[] cols = {Mesh.shade(col, 0.6f), col, 0xFF3A1E10, 0xFFF3E3C3};
            for (int k = 0; k < 3; k++) {
                float l0 = at[k] * len, l1 = at[k + 1] * len;
                float ax = x + dx * l0, ay = y + dy * l0, az = z + dz * l0;
                float bx = x + dx * l1, by = y + dy * l1, bz = z + dz * l1;
                // The base fades into the feather's colour, then a dark band and a cream tip.
                int c0 = k == 0 ? cols[0] : cols[k + 1], c1 = k == 0 ? cols[1] : cols[k + 1];
                Mesh.quad(mr, ax - wx * half[k], ay - wy * half[k], az, bx - wx * half[k + 1], by - wy * half[k + 1], bz,
                        bx + wx * half[k + 1], by + wy * half[k + 1], bz, ax + wx * half[k], ay + wy * half[k], az, c0, c1, c1, c0);
            }
            // Rounded tip
            float ex = x + dx * (len + 0.6f), ey = y + dy * (len + 0.6f), ez = z + dz * (len + 0.6f);
            Mesh.tri(mr, x + dx * len - wx, y + dy * len - wy, z + dz * len, ex, ey, ez, x + dx * len + wx, y + dy * len + wy, z + dz * len,
                    cols[3], cols[3], cols[3]);
        }
    }

    /** A bandana tied around the forehead: a dotted pattern, a fold, a knot at the back and two tails that flutter. */
    public static final class Bandana extends Cosmetic {
        public Bandana(String id, String name, Rarity rarity, int color) {
            super(id, name, "Tied tight around your forehead.", CosmeticType.BANDANA, rarity, color, true, true);
        }

        @Override
        public void render(ModelRenderer mr, CosmeticContext ctx, int color) {
            float t = ctx.seconds();
            float o = 4.28f;
            float y1 = -7.3f, y2 = -5.5f, ym = (y1 + y2) / 2f;
            int dark = Mesh.shade(color, 0.75f), fold = Mesh.shade(color, 0.6f);
            int dots = Colors.lerp(color, 0xFFFFFFFF, Colors.r(color) + Colors.g(color) + Colors.b(color) < 120 ? 0.55f : 0.7f);
            mr.begin(ctx.white, false);
            // The band on all four sides of the head, lighter at the front.
            Mesh.quad(mr, -o, y1, -o, -o, y2, -o, o, y2, -o, o, y1, -o, color);
            Mesh.quad(mr, o, y1, o, o, y2, o, -o, y2, o, -o, y1, o, dark);
            Mesh.quad(mr, -o, y1, o, -o, y2, o, -o, y2, -o, -o, y1, -o, Mesh.shade(color, 0.88f));
            Mesh.quad(mr, o, y1, -o, o, y2, -o, o, y2, o, o, y1, o, Mesh.shade(color, 0.88f));
            // A fold along the middle, and a row of dots on the front and sides.
            float oo = o + 0.02f;
            Mesh.quad(mr, -oo, ym - 0.08f, -oo, -oo, ym + 0.08f, -oo, oo, ym + 0.08f, -oo, oo, ym - 0.08f, -oo, fold);
            for (int i = 0; i < 5; i++) {
                float x = -3.2f + i * 1.6f, yy = ym + (i % 2 == 0 ? -0.45f : 0.45f);
                diamond(mr, x, yy, -oo - 0.01f, 0, 0.32f, dots);
                diamond(mr, -oo - 0.01f, yy, x, 1, 0.32f, dots);
                diamond(mr, oo + 0.01f, yy, x, 1, 0.32f, dots);
            }
            // Knot: two loops and the middle.
            Mesh.ellipsoid(mr, 0, ym, o + 0.55f, 0.7f, 0.75f, 0.55f, 8, 5, color);
            for (int s = -1; s <= 1; s += 2) {
                Mesh.ellipsoid(mr, s * 1.1f, ym - 0.1f, o + 0.4f, 0.85f, 0.6f, 0.35f, 8, 5, dark);
            }
            // Two tails that taper and flutter.
            for (int s = -1; s <= 1; s += 2) {
                float px = s * 0.3f, py = ym, pz = o + 0.6f, w = 0.75f;
                for (int k = 1; k <= 4; k++) {
                    float f = k / 4f;
                    float wave = (float) Math.sin(t * 5f - k * 0.9f + s) * 0.55f * f;
                    float nx = s * (0.3f + f * 1.8f) + wave * 0.4f, ny = ym + f * 3.4f, nz = o + 0.6f + f * 1.6f + wave;
                    float nw = 0.75f - f * 0.45f;
                    Mesh.quad(mr, px - w * 0.5f, py, pz, nx - nw * 0.5f, ny, nz, nx + nw * 0.5f, ny, nz, px + w * 0.5f, py, pz,
                            Colors.lerp(color, dark, f), Colors.lerp(color, dark, f + 0.25f), Colors.lerp(color, dark, f + 0.25f), Colors.lerp(color, dark, f));
                    px = nx;
                    py = ny;
                    pz = nz;
                    w = nw;
                }
            }
            mr.end();
        }

        /** A small flat diamond on the band: axis 0 lies on the front (z), 1 on a side (x). */
        private static void diamond(ModelRenderer mr, float x, float y, float z, int axis, float s, int c) {
            if (axis == 0) Mesh.quad(mr, x - s, y, z, x, y + s, z, x + s, y, z, x, y - s, z, c);
            else Mesh.quad(mr, x, y, z - s, x, y + s, z, x, y, z + s, x, y - s, z, c);
        }
    }
}
