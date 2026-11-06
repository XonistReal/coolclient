package dev.coolclient.core.cosmetic;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.api.ModelRenderer;
import dev.coolclient.core.cosmetic.cape.CapeCosmetic;
import dev.coolclient.core.cosmetic.model.Mesh;
import dev.coolclient.core.util.Log;

import java.util.EnumMap;
import java.util.Map;

/**
 * A player figure for menus (no world needed): the player's skin, or a plain mannequin, wearing
 * cosmetics. Drawn in cosmetic model space: origin at the neck, y down, the front facing -z;
 * the head reaches y -8 and the feet y 24.
 */
public final class PlayerFigure {
    /** What the figure wears, one cosmetic per slot. */
    public final Map<CosmeticType, Cosmetic> wearing = new EnumMap<CosmeticType, Cosmetic>(CosmeticType.class);
    /** Colours to wear them in (null: each cosmetic's default). */
    public Loadout colors;
    /** Skin texture handle, or 0 for the mannequin. */
    public int skin;
    public boolean slim;
    /** An old 64 x 32 skin (no separate left limbs, no outer layer below the head). */
    public boolean legacySkin;
    public boolean showBody = true;
    public float headYaw, headPitch;
    /** Seconds; drives the idle animation and the cosmetics' own animations. */
    public float time;
    /** Breathing and arm sway. */
    public boolean idle = true;

    private final CosmeticContext ctx = new CosmeticContext();

    /** Wears everything from a loadout. */
    public PlayerFigure wear(Loadout l) {
        colors = l;
        wearing.clear();
        for (CosmeticType t : CosmeticType.values()) {
            Cosmetic c = l.cosmetic(t);
            if (c != null) wearing.put(t, c);
        }
        return this;
    }

    private int color(Cosmetic c) {
        return colors != null ? colors.color(c) : c.defaultColor;
    }

    public void draw(ModelRenderer mr) {
        int white = CoolClient.get().platform().textures().white();
        ctx.white = white;
        ctx.self = true;
        ctx.age = time * 20f;
        ctx.wearingHelmet = false;
        ctx.wearingChestplate = false;

        float arm = idle ? (float) Math.toDegrees(Math.cos(ctx.age * 0.09f) * 0.05f + 0.05f) : 0f;
        float swing = idle ? (float) Math.toDegrees(Math.sin(ctx.age * 0.067f) * 0.05f) : 0f;

        if (showBody) {
            body(mr, white);
            limb(mr, white, -5, 2, arm, swing, true, false);
            limb(mr, white, 5, 2, -arm, -swing, false, false);
            limb(mr, white, -1.9f, 12, 0, 0, true, true);
            limb(mr, white, 1.9f, 12, 0, 0, false, true);
            cape(mr);
        }
        mr.push();
        mr.rotate(headYaw, 0, 1, 0);
        mr.rotate(headPitch, 1, 0, 0);
        head(mr, white);
        render(mr, CosmeticType.HAT);
        render(mr, CosmeticType.HALO);
        render(mr, CosmeticType.BANDANA);
        mr.pop();
        if (showBody) {
            render(mr, CosmeticType.WINGS);
        }
        render(mr, CosmeticType.AURA);
    }

    private void render(ModelRenderer mr, CosmeticType type) {
        Cosmetic c = wearing.get(type);
        if (c == null) return;
        mr.push();
        try {
            c.render(mr, ctx, color(c));
        } catch (Throwable t) {
            Log.error("Cosmetic " + c.id + " failed to render in a menu", t);
        } finally {
            mr.pop();
        }
    }

    // ------------------------------------------------------------------ body parts

    private static final int MANNEQUIN = 0xFF6B7488;

    private void head(ModelRenderer mr, int white) {
        if (skin == 0) {
            mr.begin(white, false);
            Mesh.box(mr, -4, -8, -4, 4, 0, 4, Mesh.shade(MANNEQUIN, 1.1f));
            mr.end();
            return;
        }
        mr.begin(skin, false);
        skinBox(mr, -4, -8, -4, 4, 0, 4, 0, 0, 0);
        skinBox(mr, -4, -8, -4, 4, 0, 4, 32, 0, 0.5f);
        mr.end();
    }

    private void body(ModelRenderer mr, int white) {
        if (skin == 0) {
            mr.begin(white, false);
            Mesh.box(mr, -4, 0, -2, 4, 12, 2, MANNEQUIN);
            mr.end();
            return;
        }
        mr.begin(skin, false);
        skinBox(mr, -4, 0, -2, 4, 12, 2, 16, 16, 0);
        if (!legacySkin) skinBox(mr, -4, 0, -2, 4, 12, 2, 16, 32, 0.25f);
        mr.end();
    }

    /** An arm or a leg hanging from (px, py); right means the player's right (-x). */
    private void limb(ModelRenderer mr, int white, float px, float py, float roll, float swing, boolean right, boolean leg) {
        float w = !leg && slim ? 3 : 4;
        float x1, x2;
        if (leg) {
            x1 = right ? -2 : -2;
            x2 = x1 + 4;
        } else if (right) {
            x2 = 1;
            x1 = x2 - w;
        } else {
            x1 = -1;
            x2 = x1 + w;
        }
        float y1 = leg ? 0 : -2, y2 = y1 + 12;
        mr.push();
        mr.translate(px, py, 0);
        if (roll != 0) mr.rotate(roll, 0, 0, 1);
        if (swing != 0) mr.rotate(swing, 1, 0, 0);
        if (skin == 0) {
            mr.begin(white, false);
            Mesh.box(mr, x1, y1, -2, x2, y2, 2, leg ? Mesh.shade(MANNEQUIN, 0.85f) : Mesh.shade(MANNEQUIN, 0.95f));
            mr.end();
        } else {
            int u, v, ou, ov;
            if (leg) {
                u = right || legacySkin ? 0 : 16;
                v = right || legacySkin ? 16 : 48;
                ou = right ? 0 : 0;
                ov = right ? 32 : 48;
            } else {
                u = right || legacySkin ? 40 : 32;
                v = right || legacySkin ? 16 : 48;
                ou = right ? 40 : 48;
                ov = right ? 32 : 48;
            }
            mr.begin(skin, false);
            skinBox(mr, x1, y1, -2, x2, y2, 2, u, v, 0);
            if (!legacySkin) skinBox(mr, x1, y1, -2, x2, y2, 2, ou, ov, 0.25f);
            mr.end();
        }
        mr.pop();
    }

    private void cape(ModelRenderer mr) {
        Cosmetic c = wearing.get(CosmeticType.CAPE);
        if (!(c instanceof CapeCosmetic)) return;
        int tex = CoolClient.get().cosmetics().capeTexture((CapeCosmetic) c, color(c));
        float sway = 6f + (float) Math.sin(time * 1.3f) * 2.5f;
        mr.push();
        mr.translate(0, 0, 2);
        mr.rotate(sway, 1, 0, 0);
        mr.begin(tex, false);
        // The vanilla cape layout (64 x 32): outer face at (1, 1), inner face at (12, 1), edges around them.
        float x1 = -5, x2 = 5, y1 = 0, y2 = 16, z1 = 0, z2 = 1;
        float s = 1 / 64f, t = 1 / 32f;
        quad(mr, x2, y1, z2, x2, y2, z2, x1, y2, z2, x1, y1, z2, 1 * s, 1 * t, 11 * s, 17 * t, 0, 0, 1);     // outer (back)
        quad(mr, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, 12 * s, 1 * t, 22 * s, 17 * t, 0, 0, -1);  // inner
        quad(mr, x1, y1, z2, x1, y1, z1, x2, y1, z1, x2, y1, z2, 1 * s, 0, 11 * s, 1 * t, 0, -1, 0);        // top
        quad(mr, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, 11 * s, 0, 21 * s, 1 * t, 0, 1, 0);        // bottom
        quad(mr, x1, y1, z2, x1, y2, z2, x1, y2, z1, x1, y1, z1, 0, 1 * t, 1 * s, 17 * t, -1, 0, 0);        // right side
        quad(mr, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, 11 * s, 1 * t, 12 * s, 17 * t, 1, 0, 0);   // left side
        mr.end();
        mr.pop();
    }

    /**
     * A skin cube in the usual unwrap: top and bottom above, then right side, front, left side and
     * back in a row. (u, v) is the unwrap's corner in skin pixels; inflate grows the outer layer.
     */
    private void skinBox(ModelRenderer mr, float x1, float y1, float z1, float x2, float y2, float z2, int u, int v, float inflate) {
        float w = x2 - x1, h = y2 - y1, d = z2 - z1;
        x1 -= inflate;
        y1 -= inflate;
        z1 -= inflate;
        x2 += inflate;
        y2 += inflate;
        z2 += inflate;
        float su = 1 / 64f, sv = 1 / (legacySkin ? 32f : 64f);
        float a = u * su, b = (u + d) * su, c = (u + d + w) * su, e = (u + d + w + d) * su, f = (u + 2 * d + 2 * w) * su;
        float v0 = v * sv, v1 = (v + d) * sv, v2 = (v + d + h) * sv;
        quad(mr, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, b, v1, c, v2, 0, 0, -1);  // front
        quad(mr, x2, y1, z2, x2, y2, z2, x1, y2, z2, x1, y1, z2, e, v1, f, v2, 0, 0, 1);   // back
        quad(mr, x1, y1, z2, x1, y2, z2, x1, y2, z1, x1, y1, z1, a, v1, b, v2, -1, 0, 0); // right side
        quad(mr, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, c, v1, e, v2, 1, 0, 0);  // left side
        quad(mr, x1, y1, z2, x1, y1, z1, x2, y1, z1, x2, y1, z2, b, v0, c, v1, 0, -1, 0); // top
        quad(mr, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, c, v0, e, v1, 0, 1, 0);  // bottom
    }

    /** A textured quad: corners top-left, bottom-left, bottom-right, top-right of the texture rect (u0, v0)-(u1, v1). */
    private static void quad(ModelRenderer mr, float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float u0, float v0, float u1, float v1, float nx, float ny, float nz) {
        mr.vertex(ax, ay, az, u0, v0, 0xFFFFFFFF, nx, ny, nz);
        mr.vertex(bx, by, bz, u0, v1, 0xFFFFFFFF, nx, ny, nz);
        mr.vertex(cx, cy, cz, u1, v1, 0xFFFFFFFF, nx, ny, nz);
        mr.vertex(dx, dy, dz, u1, v0, 0xFFFFFFFF, nx, ny, nz);
    }
}
