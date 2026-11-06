package dev.coolclient.core.cosmetic.model;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.api.ModelRenderer;
import dev.coolclient.core.cosmetic.Cosmetic;
import dev.coolclient.core.cosmetic.CosmeticContext;
import dev.coolclient.core.cosmetic.CosmeticType;
import dev.coolclient.core.cosmetic.Rarity;
import dev.coolclient.core.cosmetic.Unlock;
import dev.coolclient.core.json.Json;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A 3D cosmetic from a Blockbench model the server sends ("Java Block/Item" export: cubes with
 * per-face UVs on one texture, optionally rotated in 22.5 degree steps).
 *
 * <p>Where it sits: for head slots (hat, halo, bandana) the player's head is the cube from
 * [4, 0, 4] to [12, 8, 12] in Blockbench; for body slots (wings, aura) the body is the cube from
 * [4, 0, 6] to [12, 12, 10]. The front of the player is Blockbench's north side.
 */
public final class ModelCosmetic extends Cosmetic {
    /** Floats per quad: 4 vertices of (x, y, z, u, v), then the normal. */
    static final int STRIDE = 23;
    private static final int MAX_ELEMENTS = 1024;
    private static final String[] DIRS = {"north", "south", "east", "west", "up", "down"};

    private final boolean head;
    private final boolean glow;
    private final float spin, bob, sway;
    private volatile float[] quads;
    private volatile float headroom;
    private volatile int[] texture;
    private volatile int texW, texH;
    private int handle;

    public ModelCosmetic(String id, String name, String description, CosmeticType type, Rarity rarity, int color,
                         Unlock unlock, boolean glow, float spin, float bob, float sway) {
        super(id, name, description, type, rarity, color, false, spin != 0 || bob != 0 || sway != 0, unlock);
        this.head = type == CosmeticType.HAT || type == CosmeticType.HALO || type == CosmeticType.BANDANA;
        this.glow = glow;
        this.spin = spin;
        this.bob = bob;
        this.sway = sway;
    }

    /** Sets the parsed model (any thread). */
    public void setModel(Map<String, Object> blockbench) {
        float[] q = build(blockbench, head);
        float top = 0;
        for (int i = 0; i < q.length; i += STRIDE) for (int k = 0; k < 4; k++) top = Math.min(top, q[i + k * 5 + 1]);
        headroom = head ? Math.max(0, -top - 8 + Math.abs(bob)) : 0;
        quads = q;
    }

    /** Sets the downloaded texture (any thread); it's uploaded on the next render. */
    public void setTexture(int[] argb, int width, int height) {
        texW = width;
        texH = height;
        texture = argb;
    }

    public boolean ready() { return quads != null && texture != null; }

    public int quadCount() { return quads == null ? 0 : quads.length / STRIDE; }

    @Override
    public float headroom() { return headroom; }

    @Override
    public void render(ModelRenderer mr, CosmeticContext ctx, int color) {
        float[] q = quads;
        int[] tex = texture;
        if (q == null || tex == null) return;
        if (handle == 0) handle = CoolClient.get().platform().textures().create("cosmetic/" + id, texW, texH, tex, false);
        float t = ctx.seconds();
        mr.begin(handle, glow);
        mr.push();
        if (head && ctx.wearingHelmet) mr.translate(0, -1.2f, 0);
        if (bob != 0) mr.translate(0, (float) Math.sin(t * 2.0) * bob, 0);
        if (spin != 0 || sway != 0) {
            float py = head ? -8f : 6f;
            mr.translate(0, py, 0);
            if (spin != 0) mr.rotate(t * spin, 0, 1, 0);
            if (sway != 0) mr.rotate((float) Math.sin(t * 1.6) * sway, 0, 0, 1);
            mr.translate(0, -py, 0);
        }
        for (int i = 0; i < q.length; i += STRIDE) {
            for (int k = 0; k < 4; k++) {
                int o = i + k * 5;
                mr.vertex(q[o], q[o + 1], q[o + 2], q[o + 3], q[o + 4], 0xFFFFFFFF, q[i + 20], q[i + 21], q[i + 22]);
            }
        }
        mr.pop();
        mr.end();
    }

    // ------------------------------------------------------------------ Blockbench model

    /** Turns a Blockbench "Java Block/Item" model into quads in head or body space. */
    static float[] build(Map<String, Object> json, boolean head) {
        List<Object> elements = json == null ? null : Json.arr(json, "elements");
        if (elements == null) return new float[0];
        List<float[]> out = new ArrayList<float[]>();
        for (int n = 0; n < Math.min(MAX_ELEMENTS, elements.size()); n++) {
            if (!(elements.get(n) instanceof Map)) continue;
            @SuppressWarnings("unchecked")
            Map<String, Object> el = (Map<String, Object>) elements.get(n);
            float[] a = vec(el.get("from")), b = vec(el.get("to"));
            if (a == null || b == null) continue;
            Map<String, Object> rot = el.get("rotation") instanceof Map ? Json.obj(el, "rotation") : null;
            Map<String, Object> faces = el.get("faces") instanceof Map ? Json.obj(el, "faces") : null;
            if (faces == null) continue;
            for (String dir : DIRS) {
                if (!(faces.get(dir) instanceof Map)) continue;
                Map<String, Object> face = Json.obj(faces, dir);
                float[] uv = uv(face.get("uv"));
                float[][] corners = corners(dir, a, b);
                float[] q = new float[STRIDE];
                int shift = ((int) Json.num(face, "rotation", 0) / 90 % 4 + 4) % 4;
                float[][] uvs = {{uv[0], uv[1]}, {uv[0], uv[3]}, {uv[2], uv[3]}, {uv[2], uv[1]}};
                for (int k = 0; k < 4; k++) {
                    float[] p = rotate(corners[k], rot);
                    q[k * 5] = 8f - p[0];
                    q[k * 5 + 1] = (head ? 0f : 12f) - p[1];
                    q[k * 5 + 2] = p[2] - 8f;
                    float[] t = uvs[(k + shift) % 4];
                    q[k * 5 + 3] = t[0] / 16f;
                    q[k * 5 + 4] = t[1] / 16f;
                }
                normal(q);
                out.add(q);
            }
        }
        float[] all = new float[out.size() * STRIDE];
        for (int i = 0; i < out.size(); i++) System.arraycopy(out.get(i), 0, all, i * STRIDE, STRIDE);
        return all;
    }

    /** The face's corners in Minecraft's order: top-left, bottom-left, bottom-right, top-right (as textured). */
    private static float[][] corners(String dir, float[] a, float[] b) {
        float x1 = Math.min(a[0], b[0]), y1 = Math.min(a[1], b[1]), z1 = Math.min(a[2], b[2]);
        float x2 = Math.max(a[0], b[0]), y2 = Math.max(a[1], b[1]), z2 = Math.max(a[2], b[2]);
        switch (dir) {
            case "north": return new float[][]{{x2, y2, z1}, {x2, y1, z1}, {x1, y1, z1}, {x1, y2, z1}};
            case "south": return new float[][]{{x1, y2, z2}, {x1, y1, z2}, {x2, y1, z2}, {x2, y2, z2}};
            case "west": return new float[][]{{x1, y2, z1}, {x1, y1, z1}, {x1, y1, z2}, {x1, y2, z2}};
            case "east": return new float[][]{{x2, y2, z2}, {x2, y1, z2}, {x2, y1, z1}, {x2, y2, z1}};
            case "up": return new float[][]{{x1, y2, z1}, {x1, y2, z2}, {x2, y2, z2}, {x2, y2, z1}};
            default: return new float[][]{{x1, y1, z2}, {x1, y1, z1}, {x2, y1, z1}, {x2, y1, z2}};
        }
    }

    /** Applies an element's rotation ({angle, axis, origin}) to a point. */
    private static float[] rotate(float[] p, Map<String, Object> rot) {
        if (rot == null) return p;
        double ang = Math.toRadians(Json.num(rot, "angle", 0));
        if (ang == 0) return p;
        float[] o = vec(rot.get("origin"));
        if (o == null) o = new float[]{8, 8, 8};
        double c = Math.cos(ang), s = Math.sin(ang);
        double x = p[0] - o[0], y = p[1] - o[1], z = p[2] - o[2];
        double nx = x, ny = y, nz = z;
        String axis = Json.str(rot, "axis", "y");
        if ("x".equals(axis)) {
            ny = y * c - z * s;
            nz = y * s + z * c;
        } else if ("z".equals(axis)) {
            nx = x * c - y * s;
            ny = x * s + y * c;
        } else {
            nx = x * c + z * s;
            nz = -x * s + z * c;
        }
        return new float[]{(float) nx + o[0], (float) ny + o[1], (float) nz + o[2]};
    }

    private static void normal(float[] q) {
        float ux = q[5] - q[0], uy = q[6] - q[1], uz = q[7] - q[2];
        float vx = q[15] - q[0], vy = q[16] - q[1], vz = q[17] - q[2];
        float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
        float l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (l < 1e-6f) {
            q[21] = -1;
            return;
        }
        q[20] = nx / l;
        q[21] = ny / l;
        q[22] = nz / l;
    }

    private static float[] vec(Object o) {
        if (!(o instanceof List) || ((List<?>) o).size() < 3) return null;
        float[] v = new float[3];
        for (int i = 0; i < 3; i++) {
            Object e = ((List<?>) o).get(i);
            if (!(e instanceof Number)) return null;
            v[i] = ((Number) e).floatValue();
        }
        return v;
    }

    private static float[] uv(Object o) {
        float[] v = {0, 0, 16, 16};
        if (o instanceof List && ((List<?>) o).size() >= 4) {
            for (int i = 0; i < 4; i++) {
                Object e = ((List<?>) o).get(i);
                if (e instanceof Number) v[i] = ((Number) e).floatValue();
            }
        }
        return v;
    }
}
