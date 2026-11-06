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
            // Tilted back a little so the ring reads as a ring from eye level, not a line.
            mr.rotate(-14f, 1, 0, 0);
            mr.rotate(t * 25f, 0, 1, 0);
            Mesh.torus(mr, 4.4f, 0.6f, 32, 8, color);
            Mesh.torus(mr, 4.4f, 1.1f, 32, 8, Mesh.alpha(color, 0.22f));
            mr.pop();
            mr.end();
        }
    }

    public static final class Crown extends Cosmetic {
        public Crown(String id, String name, Rarity rarity, int color) {
            super(id, name, "Rule the server.", CosmeticType.HAT, rarity, color, true, false);
        }

        @Override
        public float headroom() { return 3.8f; }

        @Override
        public void render(ModelRenderer mr, CosmeticContext ctx, int color) {
            float y = top(ctx);
            int dark = Mesh.shade(color, 0.7f);
            mr.begin(ctx.white, false);
            float r = 4.5f;
            int n = 12;
            for (int i = 0; i < n; i++) {
                double a0 = Math.PI * 2 * i / n, a1 = Math.PI * 2 * (i + 1) / n;
                float x0 = (float) Math.cos(a0) * r, z0 = (float) Math.sin(a0) * r;
                float x1 = (float) Math.cos(a1) * r, z1 = (float) Math.sin(a1) * r;
                Mesh.quad(mr, x0, y + 0.3f, z0, x0, y - 1.6f, z0, x1, y - 1.6f, z1, x1, y + 0.3f, z1, dark, color, color, dark);
                if (i % 2 == 0) {
                    float mx = (float) Math.cos((a0 + a1) / 2) * r, mz = (float) Math.sin((a0 + a1) / 2) * r;
                    Mesh.tri(mr, x0, y - 1.6f, z0, mx, y - 3.6f, mz, x1, y - 1.6f, z1, color, Colors.lerp(color, 0xFFFFFFFF, 0.4f), color);
                }
            }
            mr.end();
            mr.begin(ctx.white, true);
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4 + Math.PI / 4;
                float gx = (float) Math.cos(a) * (r + 0.05f), gz = (float) Math.sin(a) * (r + 0.05f);
                int gem = i % 2 == 0 ? 0xFFFF3A6E : 0xFF3AB6FF;
                Mesh.box(mr, gx - 0.45f, y - 1.1f, gz - 0.45f, gx + 0.45f, y - 0.2f, gz + 0.45f, gem);
            }
            mr.end();
        }
    }

    public static final class TopHat extends Cosmetic {
        public TopHat(String id, String name, Rarity rarity, int color) {
            super(id, name, "Classy.", CosmeticType.HAT, rarity, color, true, false);
        }

        @Override
        public float headroom() { return 7.8f; }

        @Override
        public void render(ModelRenderer mr, CosmeticContext ctx, int color) {
            float y = top(ctx);
            mr.begin(ctx.white, false);
            Mesh.box(mr, -5.5f, y - 0.6f, -5.5f, 5.5f, y, 5.5f, 0xFF15161C);
            Mesh.box(mr, -3.6f, y - 7.5f, -3.6f, 3.6f, y - 0.6f, 3.6f, 0xFF1C1E26);
            Mesh.box(mr, -3.7f, y - 2.2f, -3.7f, 3.7f, y - 0.6f, 3.7f, color);
            mr.end();
        }
    }

    public static final class WitchHat extends Cosmetic {
        public WitchHat(String id, String name, Rarity rarity, int color) {
            super(id, name, "Bubble, bubble.", CosmeticType.HAT, rarity, color, true, false);
        }

        @Override
        public float headroom() { return 9.4f; }

        @Override
        public void render(ModelRenderer mr, CosmeticContext ctx, int color) {
            float y = top(ctx);
            int dark = Mesh.shade(color, 0.6f);
            mr.begin(ctx.white, false);
            int n = 16;
            for (int i = 0; i < n; i++) {
                double a0 = Math.PI * 2 * i / n, a1 = Math.PI * 2 * (i + 1) / n;
                float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
                Mesh.quad(mr, c0 * 7.5f, y + 0.1f, s0 * 7.5f, c0 * 3.6f, y - 0.2f, s0 * 3.6f,
                        c1 * 3.6f, y - 0.2f, s1 * 3.6f, c1 * 7.5f, y + 0.1f, s1 * 7.5f, dark);
                // Cone in 3 bent segments.
                float[] rad = {3.8f, 2.6f, 1.4f, 0.1f};
                float[] hy = {0f, -3.5f, -6.5f, -9f};
                float[] lean = {0f, 0.4f, 1.4f, 3.2f};
                for (int k = 0; k < 3; k++) {
                    Mesh.quad(mr,
                            c0 * rad[k], y + hy[k], s0 * rad[k] + lean[k], c0 * rad[k + 1], y + hy[k + 1], s0 * rad[k + 1] + lean[k + 1],
                            c1 * rad[k + 1], y + hy[k + 1], s1 * rad[k + 1] + lean[k + 1], c1 * rad[k], y + hy[k], s1 * rad[k] + lean[k],
                            Mesh.shade(dark, 1f + k * 0.12f));
                }
            }
            mr.end();
            mr.begin(ctx.white, true);
            for (int i = 0; i < n; i++) {
                double a0 = Math.PI * 2 * i / n, a1 = Math.PI * 2 * (i + 1) / n;
                float c0 = (float) Math.cos(a0) * 3.85f, s0 = (float) Math.sin(a0) * 3.85f;
                float c1 = (float) Math.cos(a1) * 3.85f, s1 = (float) Math.sin(a1) * 3.85f;
                Mesh.quad(mr, c0, y - 0.3f, s0, c0, y - 1.4f, s0, c1, y - 1.4f, s1, c1, y - 0.3f, s1, color);
            }
            mr.end();
        }
    }

    public static final class CatEars extends Cosmetic {
        public CatEars(String id, String name, Rarity rarity, int color) {
            super(id, name, "Nya.", CosmeticType.HAT, rarity, color, true, false);
        }

        @Override
        public float headroom() { return 4f; }

        @Override
        public void render(ModelRenderer mr, CosmeticContext ctx, int color) {
            float y = top(ctx);
            int inner = 0xFFFFB3C7;
            mr.begin(ctx.white, false);
            for (int s = -1; s <= 1; s += 2) {
                float cx = s * 2.6f;
                Mesh.tri(mr, cx - 1.8f * s, y, -1.2f, cx + 1.2f * s, y, -1.2f, cx + 0.3f * s, y - 3.8f, -0.6f, color, color, Mesh.shade(color, 1.15f));
                Mesh.tri(mr, cx - 1.8f * s, y, 0.4f, cx + 1.2f * s, y, 0.4f, cx + 0.3f * s, y - 3.8f, -0.6f, Mesh.shade(color, 0.8f), Mesh.shade(color, 0.8f), color);
                Mesh.tri(mr, cx - 1.1f * s, y - 0.2f, -1.3f, cx + 0.7f * s, y - 0.2f, -1.3f, cx + 0.25f * s, y - 2.8f, -0.9f, inner, inner, inner);
            }
            mr.end();
        }
    }

    public static final class Horns extends Cosmetic {
        public Horns(String id, String name, Rarity rarity, int color) {
            super(id, name, "Curved demon horns.", CosmeticType.HAT, rarity, color, true, false);
        }

        @Override
        public float headroom() { return 4f; }

        @Override
        public void render(ModelRenderer mr, CosmeticContext ctx, int color) {
            float y = top(ctx) + 0.6f;
            mr.begin(ctx.white, false);
            for (int s = -1; s <= 1; s += 2) {
                float px = s * 3f, py = y, pz = -1.5f;
                float dx = s * 0.9f, dy = -1.2f, dz = 0.3f;
                float th = 0.95f;
                for (int i = 0; i < 7; i++) {
                    float nx = px + dx, ny = py + dy, nz = pz + dz;
                    int c1 = Colors.lerp(color, 0xFFFFF0DC, i / 7f);
                    int c2 = Colors.lerp(color, 0xFFFFF0DC, (i + 1) / 7f);
                    Mesh.beam(mr, px, py, pz, nx, ny, nz, th, c1, c2);
                    px = nx; py = ny; pz = nz;
                    dy += 0.28f; dz += 0.35f; dx *= 0.85f;
                    th *= 0.82f;
                }
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

    public static final class Bandana extends Cosmetic {
        public Bandana(String id, String name, Rarity rarity, int color) {
            super(id, name, "Tied tight around your forehead.", CosmeticType.BANDANA, rarity, color, true, true);
        }

        @Override
        public void render(ModelRenderer mr, CosmeticContext ctx, int color) {
            float t = ctx.seconds();
            float o = 4.28f;
            float y1 = -7.2f, y2 = -5.6f;
            int dark = Mesh.shade(color, 0.75f);
            mr.begin(ctx.white, false);
            Mesh.quad(mr, -o, y1, -o, -o, y2, -o, o, y2, -o, o, y1, -o, color);
            Mesh.quad(mr, o, y1, o, o, y2, o, -o, y2, o, -o, y1, o, dark);
            Mesh.quad(mr, -o, y1, o, -o, y2, o, -o, y2, -o, -o, y1, -o, dark);
            Mesh.quad(mr, o, y1, -o, o, y2, -o, o, y2, o, o, y1, o, dark);
            // Knot and two tails fluttering behind.
            Mesh.box(mr, -0.8f, -7.1f, o, 0.8f, -5.7f, o + 0.8f, color);
            for (int s = -1; s <= 1; s += 2) {
                float sway = (float) Math.sin(t * 4 + s) * 0.8f;
                Mesh.quad(mr, s * 0.2f, -6.8f, o + 0.6f, s * (1.2f + sway * 0.3f), -3.4f, o + 2.6f + sway,
                        s * (2.4f + sway * 0.3f), -3.9f, o + 2.4f + sway, s * 0.9f, -6.2f, o + 0.6f, color, dark, dark, color);
            }
            mr.end();
        }
    }
}
