package dev.coolclient.core.cosmetic.model;

import dev.coolclient.core.api.ModelRenderer;

/** Geometry helpers for cosmetic meshes (model pixel units, untextured). */
public final class Mesh {
    private Mesh() {}

    public static void quad(ModelRenderer mr,
                            float ax, float ay, float az, float bx, float by, float bz,
                            float cx, float cy, float cz, float dx, float dy, float dz,
                            int ca, int cb, int cc, int cd) {
        float ux = bx - ax, uy = by - ay, uz = bz - az;
        float vx = dx - ax, vy = dy - ay, vz = dz - az;
        float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
        float l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (l < 1e-6f) { nx = 0; ny = -1; nz = 0; } else { nx /= l; ny /= l; nz /= l; }
        mr.vertex(ax, ay, az, 0, 0, ca, nx, ny, nz);
        mr.vertex(bx, by, bz, 0, 1, cb, nx, ny, nz);
        mr.vertex(cx, cy, cz, 1, 1, cc, nx, ny, nz);
        mr.vertex(dx, dy, dz, 1, 0, cd, nx, ny, nz);
    }

    public static void quad(ModelRenderer mr,
                            float ax, float ay, float az, float bx, float by, float bz,
                            float cx, float cy, float cz, float dx, float dy, float dz, int c) {
        quad(mr, ax, ay, az, bx, by, bz, cx, cy, cz, dx, dy, dz, c, c, c, c);
    }

    /** Triangle drawn as a degenerate quad. */
    public static void tri(ModelRenderer mr, float ax, float ay, float az, float bx, float by, float bz,
                           float cx, float cy, float cz, int ca, int cb, int cc) {
        quad(mr, ax, ay, az, bx, by, bz, cx, cy, cz, cx, cy, cz, ca, cb, cc, cc);
    }

    /** Axis-aligned box with faux shading per face (works with emissive rendering too). */
    public static void box(ModelRenderer mr, float x1, float y1, float z1, float x2, float y2, float z2, int c) {
        int top = c, side = shade(c, 0.85f), side2 = shade(c, 0.72f), bottom = shade(c, 0.6f);
        quad(mr, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, top);       // top (-Y)
        quad(mr, x1, y2, z2, x2, y2, z2, x2, y2, z1, x1, y2, z1, bottom);    // bottom
        quad(mr, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, side);      // north
        quad(mr, x2, y1, z2, x2, y2, z2, x1, y2, z2, x1, y1, z2, side);      // south
        quad(mr, x1, y1, z2, x1, y2, z2, x1, y2, z1, x1, y1, z1, side2);     // west
        quad(mr, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, side2);     // east
    }

    /** A thin beam between two points with square cross-section. */
    public static void beam(ModelRenderer mr, float ax, float ay, float az, float bx, float by, float bz, float t, int c1, int c2) {
        float dx = bx - ax, dy = by - ay, dz = bz - az;
        float l = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (l < 1e-5f) return;
        dx /= l; dy /= l; dz /= l;
        // Two perpendicular vectors.
        float px = -dy, py = dx, pz = 0;
        float pl = (float) Math.sqrt(px * px + py * py);
        if (pl < 1e-4f) { px = 1; py = 0; pz = 0; pl = 1; }
        px = px / pl * t; py = py / pl * t; pz = pz / pl * t;
        float qx = (dy * pz - dz * py), qy = (dz * px - dx * pz), qz = (dx * py - dy * px);
        float[][] off = {{px, py, pz}, {qx, qy, qz}, {-px, -py, -pz}, {-qx, -qy, -qz}};
        for (int i = 0; i < 4; i++) {
            float[] o1 = off[i], o2 = off[(i + 1) % 4];
            quad(mr, ax + o1[0], ay + o1[1], az + o1[2], bx + o1[0], by + o1[1], bz + o1[2],
                    bx + o2[0], by + o2[1], bz + o2[2], ax + o2[0], ay + o2[1], az + o2[2], c1, c2, c2, c1);
        }
    }

    /** Torus around the Y axis. */
    public static void torus(ModelRenderer mr, float radius, float tube, int segments, int sides, int c) {
        for (int i = 0; i < segments; i++) {
            double a0 = Math.PI * 2 * i / segments, a1 = Math.PI * 2 * (i + 1) / segments;
            for (int j = 0; j < sides; j++) {
                double b0 = Math.PI * 2 * j / sides, b1 = Math.PI * 2 * (j + 1) / sides;
                float[] p00 = torusPoint(radius, tube, a0, b0), p10 = torusPoint(radius, tube, a1, b0);
                float[] p11 = torusPoint(radius, tube, a1, b1), p01 = torusPoint(radius, tube, a0, b1);
                int sc = shade(c, 0.78f + 0.22f * (float) Math.cos(b0));
                quad(mr, p00[0], p00[1], p00[2], p10[0], p10[1], p10[2], p11[0], p11[1], p11[2], p01[0], p01[1], p01[2], sc);
            }
        }
    }

    /** Ellipsoid around (cx, cy, cz), lit from above (-Y is up). */
    public static void ellipsoid(ModelRenderer mr, float cx, float cy, float cz, float rx, float ry, float rz,
                                 int segments, int rings, int c) {
        for (int i = 0; i < rings; i++) {
            double p0 = Math.PI * i / rings, p1 = Math.PI * (i + 1) / rings;
            int sc = shade(c, 1.08f - 0.4f * (i + 0.5f) / rings);
            for (int j = 0; j < segments; j++) {
                double a0 = Math.PI * 2 * j / segments, a1 = Math.PI * 2 * (j + 1) / segments;
                quad(mr,
                        cx + rx * (float) (Math.sin(p0) * Math.cos(a0)), cy - ry * (float) Math.cos(p0), cz + rz * (float) (Math.sin(p0) * Math.sin(a0)),
                        cx + rx * (float) (Math.sin(p1) * Math.cos(a0)), cy - ry * (float) Math.cos(p1), cz + rz * (float) (Math.sin(p1) * Math.sin(a0)),
                        cx + rx * (float) (Math.sin(p1) * Math.cos(a1)), cy - ry * (float) Math.cos(p1), cz + rz * (float) (Math.sin(p1) * Math.sin(a1)),
                        cx + rx * (float) (Math.sin(p0) * Math.cos(a1)), cy - ry * (float) Math.cos(p0), cz + rz * (float) (Math.sin(p0) * Math.sin(a1)),
                        sc);
            }
        }
    }

    /** Round tube around the Y axis from radius r1 at y1 to r2 at y2 (a cone when they differ). */
    public static void tube(ModelRenderer mr, float y1, float r1, float y2, float r2, int segments, int c1, int c2) {
        for (int i = 0; i < segments; i++) {
            double a0 = Math.PI * 2 * i / segments, a1 = Math.PI * 2 * (i + 1) / segments;
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), cc = (float) Math.cos(a1), ss = (float) Math.sin(a1);
            // Faces turned away from the light (+x, +z) a little darker.
            float k = 0.86f + 0.14f * (float) Math.cos((a0 + a1) / 2 + Math.PI * 0.75);
            quad(mr, c0 * r1, y1, s0 * r1, c0 * r2, y2, s0 * r2, cc * r2, y2, ss * r2, cc * r1, y1, ss * r1,
                    shade(c1, k), shade(c2, k), shade(c2, k), shade(c1, k));
        }
    }

    /** Flat ring around the Y axis at height y (a disc when inner is 0). */
    public static void ring(ModelRenderer mr, float y, float inner, float outer, int segments, int c) {
        for (int i = 0; i < segments; i++) {
            double a0 = Math.PI * 2 * i / segments, a1 = Math.PI * 2 * (i + 1) / segments;
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), cc = (float) Math.cos(a1), ss = (float) Math.sin(a1);
            quad(mr, c0 * outer, y, s0 * outer, c0 * inner, y, s0 * inner, cc * inner, y, ss * inner, cc * outer, y, ss * outer, c);
        }
    }

    private static float[] torusPoint(float r, float t, double a, double b) {
        float rr = r + t * (float) Math.cos(b);
        return new float[]{rr * (float) Math.cos(a), t * (float) Math.sin(b), rr * (float) Math.sin(a)};
    }

    public static int shade(int c, float f) {
        int a = (c >>> 24) & 0xFF;
        int r = Math.min(255, (int) (((c >> 16) & 0xFF) * f));
        int g = Math.min(255, (int) (((c >> 8) & 0xFF) * f));
        int b = Math.min(255, (int) ((c & 0xFF) * f));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int alpha(int c, float a) {
        return (Math.max(0, Math.min(255, (int) (((c >>> 24) & 0xFF) * a))) << 24) | (c & 0xFFFFFF);
    }
}
