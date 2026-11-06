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

    public AuraCosmetic(String id, String name, String description, Rarity rarity, Style style, int color) {
        this(id, name, description, rarity, style, color, Unlock.forRarity(rarity));
    }

    public AuraCosmetic(String id, String name, String description, Rarity rarity, Style style, int color, Unlock unlock) {
        super(id, name, description, CosmeticType.AURA, rarity, color,
                style != Style.FLAME && style != Style.SNOW && style != Style.LEAVES, true, unlock);
        this.style = style;
    }

    @Override
    public void render(ModelRenderer mr, CosmeticContext ctx, int color) {
        float t = ctx.seconds();
        mr.begin(ctx.white, true);
        int count = style == Style.ORBIT ? 3 : 14;
        for (int i = 0; i < count; i++) {
            float h1 = Noise.hash(i, 17), h2 = Noise.hash(i, 31), h3 = Noise.hash(i, 47);
            float x, y, z, size, alpha;
            int c;
            switch (style) {
                case FLAME: {
                    float life = frac(t * (0.6f + h1 * 0.5f) + h2);
                    float a = (float) (h3 * Math.PI * 2);
                    float r = 6f + h1 * 4f;
                    x = (float) Math.cos(a) * r;
                    z = (float) Math.sin(a) * r;
                    y = 24f - life * 26f;
                    size = 1.4f * (1f - life) + 0.3f;
                    alpha = 1f - life;
                    c = Colors.lerp(0xFFFFE27A, 0xFFFF3A12, life);
                    break;
                }
                case SNOW: {
                    float life = frac(t * (0.18f + h1 * 0.12f) + h2);
                    float a = (float) (h3 * Math.PI * 2 + t * 0.3);
                    float r = 7f + h2 * 5f;
                    x = (float) Math.cos(a) * r;
                    z = (float) Math.sin(a) * r;
                    y = -6f + life * 30f;
                    size = 0.7f + h1 * 0.5f;
                    alpha = Math.min(1f, Math.min(life * 5f, (1f - life) * 5f));
                    c = 0xFFF2F8FF;
                    break;
                }
                case LEAVES: {
                    float life = frac(t * (0.11f + h1 * 0.08f) + h2);
                    float a = (float) (h3 * Math.PI * 2 + t * 0.25);
                    float r = 7f + h2 * 5f;
                    float lx = (float) Math.cos(a) * r + (float) Math.sin(t * 1.7f + i) * 1.2f;
                    float lz = (float) Math.sin(a) * r;
                    float fade = Math.min(1f, Math.min(life * 6f, (1f - life) * 4f));
                    int lc = LEAF_COLORS[(int) (h3 * 97) % LEAF_COLORS.length];
                    leaf(mr, lx, -9f + life * 33f, lz, 1.0f + h1 * 0.5f, t * (1.5f + h1 * 2f) + i * 2f, Mesh.alpha(lc, fade));
                    continue;
                }
                case ORBIT: {
                    float a = t * 2.2f + i * (float) (Math.PI * 2 / 3);
                    x = (float) Math.cos(a) * 11f;
                    z = (float) Math.sin(a) * 11f;
                    y = 8f + (float) Math.sin(t * 1.6f + i * 2) * 6f;
                    size = 1.3f;
                    alpha = 1f;
                    c = Colors.lerp(color, 0xFFFFFFFF, 0.25f * i);
                    // Trail
                    for (int k = 1; k <= 6; k++) {
                        float ta = a - k * 0.14f;
                        diamond(mr, (float) Math.cos(ta) * 11f, 8f + (float) Math.sin(t * 1.6f - k * 0.1f + i * 2) * 6f,
                                (float) Math.sin(ta) * 11f, size * (1f - k / 8f), Mesh.alpha(c, 0.5f * (1f - k / 7f)));
                    }
                    break;
                }
                default: {
                    float a = (float) (h1 * Math.PI * 2 + t * (0.4f + h2 * 0.6f));
                    float r = 8f + h3 * 6f;
                    x = (float) Math.cos(a) * r;
                    z = (float) Math.sin(a) * r;
                    y = 2f + h2 * 22f + (float) Math.sin(t * 1.5f + i) * 2f;
                    float tw = 0.5f + 0.5f * (float) Math.sin(t * (3f + h1 * 3f) + i * 5);
                    size = (style == Style.HEARTS ? 1.3f : 0.9f) * (0.6f + tw * 0.6f);
                    alpha = 0.35f + tw * 0.65f;
                    c = style == Style.HEARTS ? 0xFFFF4F7A : Colors.lerp(color, 0xFFFFFFFF, h1 * 0.5f);
                    break;
                }
            }
            diamond(mr, x, y, z, size, Mesh.alpha(c, alpha));
        }
        mr.end();
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
