package dev.coolclient.core.cosmetic.model;

import dev.coolclient.core.api.ModelRenderer;
import dev.coolclient.core.cosmetic.Cosmetic;
import dev.coolclient.core.cosmetic.CosmeticContext;
import dev.coolclient.core.cosmetic.CosmeticType;
import dev.coolclient.core.cosmetic.Rarity;
import dev.coolclient.core.render.Colors;

/** Head-attached cosmetics. Head space: x -4..4, y -8..0 (top at -8), z -4..4, face at -z. */
public final class HeadCosmetics {
    private HeadCosmetics() {}

    private static float top(CosmeticContext ctx) {
        return ctx.wearingHelmet ? -9.2f : -8f;
    }

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
