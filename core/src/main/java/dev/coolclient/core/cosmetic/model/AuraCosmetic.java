package dev.coolclient.core.cosmetic.model;

import dev.coolclient.core.api.ModelRenderer;
import dev.coolclient.core.cosmetic.Cosmetic;
import dev.coolclient.core.cosmetic.CosmeticContext;
import dev.coolclient.core.cosmetic.CosmeticType;
import dev.coolclient.core.cosmetic.Rarity;
import dev.coolclient.core.cosmetic.Unlock;
import dev.coolclient.core.render.Colors;
import dev.coolclient.core.util.Noise;

/** Glowing particles that orbit or drift around the body. Body space: y 0 (neck) .. 24 (feet). */
public final class AuraCosmetic extends Cosmetic {
    public enum Style { SPARKLE, FLAME, SNOW, HEARTS, ORBIT, LEAVES }

    private static final int[] LEAF_COLORS = {0xFFE8471C, 0xFFF29A1F, 0xFFF7C843, 0xFFB5301A, 0xFFD9661C};

    private final Style style;
    /** Colours the particles are picked from (server recolours), or null for the style's own. */
    private int[] palette;

    public AuraCosmetic(String id, String name, String description, Rarity rarity, Style style, int color) {
        this(id, name, description, rarity, style, color, Unlock.forRarity(rarity));
    }

    public AuraCosmetic(String id, String name, String description, Rarity rarity, Style style, int color, Unlock unlock) {
        super(id, name, description, CosmeticType.AURA, rarity, color,
                style != Style.FLAME && style != Style.SNOW && style != Style.LEAVES, true, unlock);
        this.style = style;
    }

    /** Gives the particles their own colours (null keeps the style's). */
    public AuraCosmetic palette(int[] colors) {
        this.palette = colors == null || colors.length == 0 ? null : colors.clone();
        return this;
    }

    @Override
    public void render(ModelRenderer mr, CosmeticContext ctx, int color) {
        float t = ctx.seconds();
        mr.begin(ctx.white, true);
        switch (style) {
            case FLAME: flames(mr, t); break;
            case SNOW: snow(mr, t); break;
            case HEARTS: hearts(mr, t); break;
            case ORBIT: orbit(mr, t, color); break;
            case LEAVES: leaves(mr, t); break;
            default: sparkles(mr, t, color); break;
        }
        mr.end();
    }

    private int pick(int i, int fallback) {
        return palette == null ? fallback : palette[(int) (Noise.hash(i, 47) * 97) % palette.length];
    }

    /** Four-point stars that drift around you, twinkle and slowly turn, each with a soft glow. */
    private void sparkles(ModelRenderer mr, float t, int color) {
        for (int i = 0; i < 16; i++) {
            float h1 = Noise.hash(i, 17), h2 = Noise.hash(i, 31), h3 = Noise.hash(i, 47);
            float a = (float) (h1 * Math.PI * 2 + t * (0.3f + h2 * 0.5f));
            float r = 8f + h3 * 6f;
            float x = (float) Math.cos(a) * r, z = (float) Math.sin(a) * r;
            float y = 1f + h2 * 22f + (float) Math.sin(t * 1.3f + i) * 2f;
            float tw = (float) Math.pow(0.5 + 0.5 * Math.sin(t * (2.5f + h1 * 3f) + i * 5), 2);
            int c = pick(i, Colors.lerp(color, 0xFFFFFFFF, 0.3f + h1 * 0.4f));
            float size = 0.7f + tw * 1.1f;
            float spin = t * (40f + h2 * 60f) + i * 37f;
            star(mr, x, y, z, size, spin, Mesh.alpha(c, 0.45f + tw * 0.55f), Mesh.alpha(0xFFFFFFFF, 0.5f + tw * 0.5f));
            glow(mr, x, y, z, size * 1.6f, Mesh.alpha(c, 0.12f + tw * 0.15f));
        }
    }

    /** Flame tongues licking up around your feet, flickering, with embers rising out of them. */
    private void flames(ModelRenderer mr, float t) {
        int outerA = palette != null && palette.length > 1 ? palette[1] : 0xFFFF5A14;
        int innerA = palette != null ? palette[0] : 0xFFFFE680;
        for (int i = 0; i < 16; i++) {
            float h1 = Noise.hash(i, 17), h2 = Noise.hash(i, 31), h3 = Noise.hash(i, 47);
            float a = (float) (h3 * Math.PI * 2 + i * 0.5);
            float r = 5f + h1 * 3.5f;
            float x = (float) Math.cos(a) * r, z = (float) Math.sin(a) * r;
            float flick = 0.75f + 0.25f * (float) Math.sin(t * (9f + h2 * 7f) + i * 3) + 0.1f * (float) Math.sin(t * 23f + i);
            float hgt = (5f + h2 * 4f) * flick;
            float rot = a * 57.3f + 90f;
            tongue(mr, x, 23.9f, z, 1.4f + h1 * 0.6f, hgt, rot, Mesh.alpha(outerA, 0.9f), Mesh.alpha(Colors.lerp(outerA, innerA, 0.5f), 0.2f));
            tongue(mr, x, 23.9f, z, 0.75f + h1 * 0.35f, hgt * 0.62f, rot, Mesh.alpha(innerA, 1f), Mesh.alpha(0xFFFFFFFF, 0.4f));
        }
        for (int i = 0; i < 12; i++) {
            float h1 = Noise.hash(i, 71), h2 = Noise.hash(i, 73), h3 = Noise.hash(i, 79);
            float life = frac(t * (0.45f + h1 * 0.4f) + h2);
            float a = (float) (h3 * Math.PI * 2);
            float r = 6f + h1 * 4f + life * 1.5f;
            float x = (float) Math.cos(a) * r + (float) Math.sin(t * 3 + i) * 0.6f, z = (float) Math.sin(a) * r;
            float y = 23f - life * 20f;
            diamond(mr, x, y, z, 0.35f * (1f - life) + 0.12f, Mesh.alpha(Colors.lerp(innerA, outerA, life), 1f - life));
        }
    }

    /** Six-armed snowflakes drifting down and turning. */
    private void snow(ModelRenderer mr, float t) {
        for (int i = 0; i < 14; i++) {
            float h1 = Noise.hash(i, 17), h2 = Noise.hash(i, 31), h3 = Noise.hash(i, 47);
            float life = frac(t * (0.12f + h1 * 0.08f) + h2);
            float a = (float) (h3 * Math.PI * 2 + t * 0.25);
            float r = 7f + h2 * 5f;
            float x = (float) Math.cos(a) * r + (float) Math.sin(t * 0.9f + i) * 0.8f, z = (float) Math.sin(a) * r;
            float y = -9f + life * 33f;
            float fade = Math.min(1f, Math.min(life * 5f, (1f - life) * 5f));
            int c = Mesh.alpha(pick(i, 0xFFEAF6FF), fade);
            snowflake(mr, x, y, z, 0.9f + h1 * 0.6f, t * (30f + h2 * 50f) + i * 40f, t * 20f + i * 25f, c);
        }
    }

    /** Hearts floating up around you, gently beating. */
    private void hearts(ModelRenderer mr, float t) {
        for (int i = 0; i < 10; i++) {
            float h1 = Noise.hash(i, 17), h2 = Noise.hash(i, 31), h3 = Noise.hash(i, 47);
            float life = frac(t * (0.12f + h1 * 0.08f) + h2);
            float a = (float) (h3 * Math.PI * 2 + t * 0.2);
            float r = 8f + h2 * 5f;
            float x = (float) Math.cos(a) * r + (float) Math.sin(t * 1.4f + i) * 0.9f, z = (float) Math.sin(a) * r;
            float y = 22f - life * 28f;
            float fade = Math.min(1f, Math.min(life * 5f, (1f - life) * 3f));
            float beat = 1f + 0.12f * (float) Math.max(0, Math.sin(t * 6f + i * 2));
            int c = pick(i, Colors.lerp(0xFFFF3D6E, 0xFFFF8AB0, h1));
            float rot = (float) Math.toDegrees(a) + 90f + (float) Math.sin(t * 1.2f + i) * 25f;
            heart(mr, x, y, z, (1.0f + h1 * 0.5f) * beat, rot, Mesh.alpha(c, fade), Mesh.alpha(Colors.lerp(c, 0xFFFFFFFF, 0.45f), fade));
            glow(mr, x, y, z, 2.2f * beat, Mesh.alpha(c, 0.12f * fade));
        }
    }

    /** Three glowing comets circling you on tilted paths, with long fading tails. */
    private void orbit(ModelRenderer mr, float t, int color) {
        for (int i = 0; i < 3; i++) {
            int c = pick(i, Colors.lerp(color, 0xFFFFFFFF, 0.2f * i));
            for (int k = 22; k >= 0; k--) {
                float tt = t - k * 0.022f;
                float a = tt * 2.2f + i * (float) (Math.PI * 2 / 3);
                float tilt = 0.35f * (float) Math.sin(i * 2.1f);
                float x = (float) Math.cos(a) * 11f;
                float z = (float) Math.sin(a) * 11f;
                float y = 9f + (float) Math.sin(a + i) * 6f * tilt * 2f + (float) Math.sin(tt * 1.6f + i * 2) * 3f;
                float f = 1f - k / 23f;
                if (k == 0) {
                    glow(mr, x, y, z, 2.6f, Mesh.alpha(c, 0.3f));
                    star(mr, x, y, z, 1.6f, tt * 200f, Mesh.alpha(c, 1f), 0xFFFFFFFF);
                    diamond(mr, x, y, z, 0.9f, 0xFFFFFFFF);
                } else {
                    diamond(mr, x, y, z, 0.9f * f + 0.1f, Mesh.alpha(c, 0.6f * f * f));
                }
            }
        }
    }

    private void leaves(ModelRenderer mr, float t) {
        for (int i = 0; i < 14; i++) {
            float h1 = Noise.hash(i, 17), h2 = Noise.hash(i, 31), h3 = Noise.hash(i, 47);
            float life = frac(t * (0.11f + h1 * 0.08f) + h2);
            float a = (float) (h3 * Math.PI * 2 + t * 0.25);
            float r = 7f + h2 * 5f;
            float lx = (float) Math.cos(a) * r + (float) Math.sin(t * 1.7f + i) * 1.2f;
            float lz = (float) Math.sin(a) * r;
            float fade = Math.min(1f, Math.min(life * 6f, (1f - life) * 4f));
            int[] leaves = palette != null ? palette : LEAF_COLORS;
            int lc = leaves[(int) (h3 * 97) % leaves.length];
            leaf(mr, lx, -9f + life * 33f, lz, 1.0f + h1 * 0.5f, t * (1.5f + h1 * 2f) + i * 2f, Mesh.alpha(lc, fade));
        }
    }

    // ------------------------------------------------------------------ particle shapes

    /** A flat shape in a vertical plane turned {@code rot} degrees, as a fan from its centre. Shapes are drawn twice, crossed, so they read from every side. */
    private static void fan(ModelRenderer mr, float x, float y, float z, float[] outline, float s, float rot, int edge, int centre, boolean crossed) {
        for (int pass = 0; pass < (crossed ? 2 : 1); pass++) {
            double a = Math.toRadians(rot + pass * 90);
            float ca = (float) Math.cos(a), sa = (float) Math.sin(a);
            int n = outline.length / 2;
            for (int i = 0; i < n; i++) {
                float x0 = outline[i * 2] * s, y0 = outline[i * 2 + 1] * s;
                float x1 = outline[(i + 1) % n * 2] * s, y1 = outline[(i + 1) % n * 2 + 1] * s;
                Mesh.tri(mr, x, y, z, x + ca * x0, y + y0, z + sa * x0, x + ca * x1, y + y1, z + sa * x1, centre, edge, edge);
            }
        }
    }

    private static final float[] STAR = shape(8, (i) -> i % 2 == 0 ? 1f : 0.2f, 0);
    private static final float[] HEART = heartShape();

    private interface Radius { float at(int i); }

    private static float[] shape(int n, Radius r, double offset) {
        float[] o = new float[n * 2];
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n + offset - Math.PI / 2;
            o[i * 2] = (float) Math.cos(a) * r.at(i);
            o[i * 2 + 1] = (float) Math.sin(a) * r.at(i);
        }
        return o;
    }

    private static float[] heartShape() {
        int n = 28;
        float[] o = new float[n * 2];
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n;
            double hx = 16 * Math.pow(Math.sin(a), 3);
            double hy = 13 * Math.cos(a) - 5 * Math.cos(2 * a) - 2 * Math.cos(3 * a) - Math.cos(4 * a);
            o[i * 2] = (float) (hx / 17);
            o[i * 2 + 1] = (float) (-hy / 17) + 0.15f;
        }
        return o;
    }

    static void star(ModelRenderer mr, float x, float y, float z, float s, float rot, int edge, int centre) {
        fan(mr, x, y, z, STAR, s, rot, edge, centre, true);
    }

    private static void heart(ModelRenderer mr, float x, float y, float z, float s, float rot, int edge, int centre) {
        fan(mr, x, y, z, HEART, s, rot, edge, centre, false);
    }

    /** A soft halo behind a particle. */
    static void glow(ModelRenderer mr, float x, float y, float z, float s, int c) {
        float[] disc = shape(10, (i) -> 1f, 0);
        fan(mr, x, y, z, disc, s, 0, c & 0x00FFFFFF, c, true);
    }

    /** A flame tongue standing on (x, y, z): wide at the bottom, curling to a point. */
    private static void tongue(ModelRenderer mr, float x, float y, float z, float w, float h, float rot, int edge, int tip) {
        int n = 7;
        float[] o = new float[(n * 2 + 1) * 2];
        int k = 0;
        for (int i = 0; i <= n; i++) {
            float f = i / (float) n;
            float half = (float) Math.pow(Math.sin(Math.PI * (0.5f + f * 0.5f)), 0.7) * w;
            o[k++] = half;
            o[k++] = -f * h;
        }
        for (int i = n - 1; i >= 0; i--) {
            float f = i / (float) n;
            float half = (float) Math.pow(Math.sin(Math.PI * (0.5f + f * 0.5f)), 0.7) * w;
            o[k++] = -half;
            o[k++] = -f * h;
        }
        // Fan from a point low in the flame so the bottom is bright and the tip fades.
        for (int pass = 0; pass < 2; pass++) {
            double a = Math.toRadians(rot + pass * 90);
            float ca = (float) Math.cos(a), sa = (float) Math.sin(a);
            int m = o.length / 2;
            float cy = y - h * 0.25f;
            for (int i = 0; i < m; i++) {
                float x0 = o[i * 2], y0 = o[i * 2 + 1], x1 = o[(i + 1) % m * 2], y1 = o[(i + 1) % m * 2 + 1];
                int c0 = Colors.lerp(edge, tip, -y0 / h), c1 = Colors.lerp(edge, tip, -y1 / h);
                Mesh.tri(mr, x, cy, z, x + ca * x0, y + y0, z + sa * x0, x + ca * x1, y + y1, z + sa * x1, edge, c0, c1);
            }
        }
    }

    /** Six arms with little side branches, built from thin beams so it reads from any angle. */
    private static void snowflake(ModelRenderer mr, float x, float y, float z, float s, float spin, float tilt, int c) {
        double sp = Math.toRadians(spin), tl = Math.toRadians(tilt);
        float ux = (float) Math.cos(sp), uz = (float) Math.sin(sp);
        float vx = -uz * (float) Math.sin(tl), vy = (float) Math.cos(tl), vz = ux * (float) Math.sin(tl);
        float th = 0.07f * s;
        int core = Colors.lerp(c, 0xFFFFFFFF, 0.5f);
        for (int arm = 0; arm < 6; arm++) {
            double a = Math.PI * arm / 3;
            float dx = (float) Math.cos(a), dy = (float) Math.sin(a);
            float ax = (ux * dx + vx * dy) * s, ay = (vy * dy) * s, az = (uz * dx + vz * dy) * s;
            Mesh.beam(mr, x, y, z, x + ax, y + ay, z + az, th, core, c);
            for (int side = -1; side <= 1; side += 2) {
                double b = a + side * Math.PI / 4;
                float bx = (float) Math.cos(b), by = (float) Math.sin(b);
                float px = x + ax * 0.55f, py = y + ay * 0.55f, pz = z + az * 0.55f;
                float l = 0.35f * s;
                Mesh.beam(mr, px, py, pz, px + (ux * bx + vx * by) * l, py + vy * by * l, pz + (uz * bx + vz * by) * l, th * 0.8f, c, c);
            }
        }
    }

    private static float frac(float v) { return v - (float) Math.floor(v); }

    /** A falling leaf: a pointed oval tumbling as it goes, lighter at the tip. */
    private static void leaf(ModelRenderer mr, float x, float y, float z, float s, float spin, int c) {
        float e1x = (float) Math.cos(spin) * 0.8f * s, e1z = (float) Math.sin(spin) * 0.8f * s;
        float tilt = spin * 0.7f;
        float e2x = (float) Math.sin(tilt) * 0.5f * 1.4f * s, e2y = (float) Math.cos(tilt) * 1.4f * s, e2z = 0.3f * 1.4f * s;
        int tip = Mesh.alpha(Colors.lerp(c, 0xFFFFFFFF, 0.25f), ((c >>> 24) & 0xFF) / 255f);
        Mesh.quad(mr, x - e2x, y - e2y, z - e2z, x - e1x, y, z - e1z, x + e2x * 0.8f, y + e2y * 0.8f, z + e2z * 0.8f, x + e1x, y, z + e1z,
                tip, c, Mesh.shade(c, 0.8f), c);
    }

    /** Two crossed diamonds so the particle reads from any angle. */
    private static void diamond(ModelRenderer mr, float x, float y, float z, float s, int c) {
        Mesh.quad(mr, x, y - s, z, x - s, y, z, x, y + s, z, x + s, y, z, c);
        Mesh.quad(mr, x, y - s, z, x, y, z - s, x, y + s, z, x, y, z + s, c);
    }
}
