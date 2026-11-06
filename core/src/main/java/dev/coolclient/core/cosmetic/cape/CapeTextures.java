package dev.coolclient.core.cosmetic.cape;

import dev.coolclient.core.api.Textures;
import dev.coolclient.core.util.Time;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Generates and animates cape textures. Designs are painted onto the vanilla 64x32 cape UV
 * layout at 4x resolution (front, back, edges and the elytra region).
 */
public final class CapeTextures {
    public static final int SCALE = 4;
    public static final int WIDTH = 64 * SCALE;
    public static final int HEIGHT = 32 * SCALE;
    private static final long FRAME_MS = 66;
    private static final long EVICT_MS = 30_000;

    private static final class Entry {
        int handle;
        final int[] pixels = new int[WIDTH * HEIGHT];
        long lastUsed;
        long lastFrame;
        int revision;
    }

    private final Textures textures;
    private final Map<String, Entry> entries = new HashMap<String, Entry>();

    public CapeTextures(Textures textures) {
        this.textures = textures;
    }

    /** Texture handle for a cape with a given tint, creating it on first use. */
    public int get(CapeCosmetic cape, int tint) {
        String key = cape.id + "#" + (cape.tintable ? Integer.toHexString(tint) : "");
        Entry e = entries.get(key);
        long now = Time.millis();
        int revision = cape.style.revision();
        if (e == null) {
            e = new Entry();
            paint(e.pixels, cape.style, (float) Time.seconds(), tint);
            e.handle = textures.create("cape/" + cape.id + "_" + Integer.toHexString(tint), WIDTH, HEIGHT, e.pixels, true);
            e.lastFrame = now;
            e.revision = revision;
            entries.put(key, e);
        } else if ((cape.animated || revision != e.revision) && now - e.lastFrame >= FRAME_MS) {
            paint(e.pixels, cape.style, (float) Time.seconds(), tint);
            textures.update(e.handle, e.pixels);
            e.lastFrame = now;
            e.revision = revision;
        }
        e.lastUsed = now;
        return e.handle;
    }

    /** Frees textures that haven't been drawn for a while. */
    public void evictUnused() {
        long now = Time.millis();
        Iterator<Map.Entry<String, Entry>> it = entries.entrySet().iterator();
        while (it.hasNext()) {
            Entry e = it.next().getValue();
            if (now - e.lastUsed > EVICT_MS) {
                textures.delete(e.handle);
                it.remove();
            }
        }
    }

    /** Paints a full cape texture (also used by the launcher-style previews). */
    public static void paint(int[] px, CapeStyle style, float t, int tint) {
        final int s = SCALE;
        java.util.Arrays.fill(px, 0);
        // Outer face (seen from behind the player): x 1..11, y 1..17
        for (int y = s; y < 17 * s; y++) {
            float v = (y - s + 0.5f) / (16f * s);
            for (int x = s; x < 11 * s; x++) {
                float u = (x - s + 0.5f) / (10f * s);
                px[y * WIDTH + x] = style.sample(u, v, t, tint) | 0xFF000000;
            }
        }
        // Inner face: x 12..22, mirrored and darker
        for (int y = s; y < 17 * s; y++) {
            for (int x = 12 * s; x < 22 * s; x++) {
                int mirror = (11 * s - 1) - (x - 12 * s);
                px[y * WIDTH + x] = darken(px[y * WIDTH + mirror], 0.55f);
            }
        }
        // Edges sample the outer face border.
        for (int x = s; x < 11 * s; x++) {
            for (int y = 0; y < s; y++) {
                px[y * WIDTH + x] = darken(px[s * WIDTH + x], 0.7f);                     // top
                px[y * WIDTH + x + 10 * s] = darken(px[(17 * s - 1) * WIDTH + x], 0.7f);  // bottom
            }
        }
        for (int y = s; y < 17 * s; y++) {
            for (int x = 0; x < s; x++) {
                px[y * WIDTH + x] = darken(px[y * WIDTH + s], 0.7f);                      // left
                px[y * WIDTH + 11 * s + x] = darken(px[y * WIDTH + 11 * s - 1], 0.7f);    // right
            }
        }
        // Elytra region: x 22..46, y 0..22
        for (int y = 0; y < 22 * s; y++) {
            float v = (y + 0.5f) / (22f * s);
            for (int x = 22 * s; x < 46 * s; x++) {
                float u = (x - 22 * s + 0.5f) / (24f * s);
                px[y * WIDTH + x] = style.sample(u, v, t, tint) | 0xFF000000;
            }
        }
    }

    private static int darken(int c, float f) {
        return 0xFF000000 | ((int) (((c >> 16) & 0xFF) * f) << 16) | ((int) (((c >> 8) & 0xFF) * f) << 8) | (int) ((c & 0xFF) * f);
    }
}
