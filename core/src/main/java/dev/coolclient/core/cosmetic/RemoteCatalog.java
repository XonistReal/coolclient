package dev.coolclient.core.cosmetic;

import dev.coolclient.core.api.ModelRenderer;
import dev.coolclient.core.cosmetic.cape.CapeCosmetic;
import dev.coolclient.core.cosmetic.cape.ImageCapeStyle;
import dev.coolclient.core.cosmetic.model.AuraCosmetic;
import dev.coolclient.core.cosmetic.model.HeadCosmetics;
import dev.coolclient.core.cosmetic.model.ModelCosmetic;
import dev.coolclient.core.cosmetic.model.WingsCosmetic;
import dev.coolclient.core.json.Json;
import dev.coolclient.core.net.ApiClient;
import dev.coolclient.core.util.Log;
import dev.coolclient.core.util.Scheduler;
import dev.coolclient.core.util.Time;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Cosmetics and seasons from the API server's /api/catalog, so new ones need no mod update.
 * Images and models come from the server's /cosmetics/ folder and are cached on disk; the last
 * catalog is cached too, so server cosmetics also show while offline.
 *
 * <p>A definition is one of: a cape from an image ({@code image}, optional {@code frames} and
 * {@code fps}); a Blockbench model for any other slot ({@code model} + {@code texture}, optional
 * {@code glow}, {@code spin}, {@code bob}, {@code sway}); or a recolour of a built-in design
 * ({@code kind}, e.g. "halo", "angel" or "leaves", with {@code color} and, for auras,
 * {@code colors}). See COSMETICS.md in the repository.
 */
public final class RemoteCatalog {
    private static final long REFRESH_MS = 30 * 60_000L;
    private static final int MAX_IMAGE_BYTES = 4 << 20;
    private static final int MAX_MODEL_BYTES = 1 << 20;
    private static final Pattern ID = Pattern.compile("[a-z0-9_]{1,40}");
    private static final Pattern PATH = Pattern.compile("[a-z0-9_\\-]+(/[a-z0-9_\\-]+)*\\.(png|json)");

    private final ApiClient api;
    private final File cacheDir;
    private final File catalogFile;
    /** Definition (as JSON text) of each server cosmetic we registered, to skip unchanged ones. */
    private final Map<String, String> defs = new HashMap<String, String>();
    private volatile Map<String, Object> featured;
    private long lastFetch = -REFRESH_MS;

    public RemoteCatalog(File dir, ApiClient api) {
        this.api = api;
        this.cacheDir = dir == null ? null : new File(dir, "cache/cosmetics");
        this.catalogFile = dir == null ? null : new File(dir, "cache/catalog.json");
    }

    /** Applies the catalog saved last time (call once at startup). */
    public void loadCached() {
        if (catalogFile == null || !catalogFile.isFile()) return;
        try {
            apply(Json.parseObject(new String(Files.readAllBytes(catalogFile.toPath()), StandardCharsets.UTF_8)));
        } catch (Exception e) {
            Log.warn("Cached cosmetics catalog unreadable: " + e.getMessage());
        }
    }

    /** Re-reads the catalog from the server every half hour while Online Features is on. */
    public void tick() {
        if (api == null || !api.enabled()) return;
        long now = Time.millis();
        if (now - lastFetch < REFRESH_MS) return;
        lastFetch = now;
        api.async("GET", "/api/catalog", null, false, res -> {
            if (res.containsKey("error")) return; // servers older than 0.1.5 don't have it
            apply(res);
            save(res);
        });
    }

    /** The server's featured drop ({title, subtitle, cosmetic}), or null. */
    public Map<String, Object> featured() { return featured; }

    private void save(Map<String, Object> res) {
        if (catalogFile == null) return;
        try {
            catalogFile.getParentFile().mkdirs();
            Files.write(catalogFile.toPath(), Json.stringify(res).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Log.warn("Could not cache the cosmetics catalog: " + e.getMessage());
        }
    }

    /** Registers seasons and cosmetics from a catalog response. Runs on the game thread. */
    void apply(Map<String, Object> m) {
        Map<String, Object> seasons = obj(m, "seasons");
        for (Map.Entry<String, Object> e : seasons.entrySet()) {
            if (!(e.getValue() instanceof Map) || !ID.matcher(e.getKey()).matches()) continue;
            Map<String, Object> d = Json.obj(seasons, e.getKey());
            Season.define(e.getKey(), Json.str(d, "name", e.getKey()), color(Json.str(d, "color", ""), 0xFFFF9A3C),
                    Json.str(d, "from", ""), Json.str(d, "to", ""));
        }
        for (Object o : arr(m, "cosmetics")) {
            if (!(o instanceof Map)) continue;
            @SuppressWarnings("unchecked")
            Map<String, Object> d = (Map<String, Object>) o;
            String id = Json.str(d, "id", "");
            if (!ID.matcher(id).matches()) continue;
            if (CosmeticCatalog.get(id) != null && !CosmeticCatalog.remote(id)) continue; // built in; the mod's version wins
            String sig = Json.stringify(d);
            if (sig.equals(defs.get(id))) continue;
            Cosmetic c = build(d, true);
            if (c == null) {
                Log.warn("Skipping server cosmetic " + id + ": definition not understood");
                continue;
            }
            CosmeticCatalog.register(c);
            defs.put(id, sig);
        }
        Map<String, Object> seasonal = obj(m, "seasonal");
        for (Map.Entry<String, Object> e : seasonal.entrySet()) {
            Cosmetic c = CosmeticCatalog.get(e.getKey());
            Season s = e.getValue() instanceof String ? Season.get((String) e.getValue()) : null;
            if (c != null && s != null) c.in(s);
        }
        featured = m.get("featured") instanceof Map ? Json.obj(m, "featured") : null;
    }

    /**
     * Builds a cosmetic from its definition, or null if it isn't valid. With {@code load}, its
     * image or model starts downloading (or loading from the cache).
     */
    public Cosmetic build(Map<String, Object> d, boolean load) {
        String id = Json.str(d, "id", "");
        CosmeticType type = parse(CosmeticType.class, Json.str(d, "type", ""), null);
        if (!ID.matcher(id).matches() || type == null) return null;
        Rarity rarity = parse(Rarity.class, Json.str(d, "rarity", ""), Rarity.COMMON);
        Unlock unlock = parse(Unlock.class, Json.str(d, "unlock", ""), Unlock.FREE);
        int color = color(Json.str(d, "color", ""), 0xFFFFFFFF);
        String name = Json.str(d, "name", id), description = Json.str(d, "description", "");
        String image = Json.str(d, "image", ""), model = Json.str(d, "model", ""), texture = Json.str(d, "texture", "");
        String kind = Json.str(d, "kind", "").toLowerCase(Locale.ROOT);
        int version = (int) Json.num(d, "v", 1);

        if (type == CosmeticType.CAPE && PATH.matcher(image).matches()) {
            int frames = (int) Math.max(1, Math.min(256, Json.num(d, "frames", 1)));
            final ImageCapeStyle style = new ImageCapeStyle(frames, (float) Json.num(d, "fps", 8));
            if (load) fetchImage(image, version, style::setImage);
            return new CapeCosmetic(id, name, description, rarity, style, color, false, frames > 1, unlock);
        }
        if (type != CosmeticType.CAPE && PATH.matcher(model).matches() && model.endsWith(".json") && PATH.matcher(texture).matches()) {
            final ModelCosmetic c = new ModelCosmetic(id, name, description, type, rarity, color, unlock, Json.bool(d, "glow", false),
                    (float) Json.num(d, "spin", 0), (float) Json.num(d, "bob", 0), (float) Json.num(d, "sway", 0));
            if (load) {
                fetch(model, version, MAX_MODEL_BYTES, bytes -> c.setModel(Json.parseObject(new String(bytes, StandardCharsets.UTF_8))));
                fetchImage(texture, version, c::setTexture);
            }
            return c;
        }
        Cosmetic base = kind.isEmpty() ? null : base(kind, color, palette(d));
        if (base == null || base.type != type) return null;
        return new Variant(id, name, description, rarity, color, unlock, base);
    }

    // ------------------------------------------------------------------ built-in designs, recoloured

    private static Cosmetic base(String kind, int color, int[] palette) {
        switch (kind) {
            case "halo": return new HeadCosmetics.Halo("_", "", Rarity.COMMON, color);
            case "crown": return new HeadCosmetics.Crown("_", "", Rarity.COMMON, color);
            case "tophat": return new HeadCosmetics.TopHat("_", "", Rarity.COMMON, color);
            case "witch": return new HeadCosmetics.WitchHat("_", "", Rarity.COMMON, color);
            case "catears": return new HeadCosmetics.CatEars("_", "", Rarity.COMMON, color);
            case "horns": return new HeadCosmetics.Horns("_", "", Rarity.COMMON, color);
            case "pilgrim": return new HeadCosmetics.PilgrimHat("_", "", Rarity.COMMON, color);
            case "turkey": return new HeadCosmetics.TurkeyHat("_", "", Rarity.COMMON, color);
            case "bandana": return new HeadCosmetics.Bandana("_", "", Rarity.COMMON, color);
            default:
                break;
        }
        WingsCosmetic.Style wings = parse(WingsCosmetic.Style.class, kind, null);
        if (wings != null) return new WingsCosmetic("_", "", "", Rarity.COMMON, wings, color);
        AuraCosmetic.Style aura = parse(AuraCosmetic.Style.class, kind, null);
        if (aura != null) return new AuraCosmetic("_", "", "", Rarity.COMMON, aura, color).palette(palette);
        return null;
    }

    /** A built-in design with its own id, name, colour and tier. */
    private static final class Variant extends Cosmetic {
        private final Cosmetic base;

        Variant(String id, String name, String description, Rarity rarity, int color, Unlock unlock, Cosmetic base) {
            super(id, name, description, base.type, rarity, color, base.tintable, base.animated, unlock);
            this.base = base;
        }

        @Override
        public void render(ModelRenderer mr, CosmeticContext ctx, int color) { base.render(mr, ctx, color); }

        @Override
        public float headroom() { return base.headroom(); }
    }

    // ------------------------------------------------------------------ files

    private void fetchImage(String path, int version, final ImageSink sink) {
        fetch(path, version, MAX_IMAGE_BYTES, bytes -> {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
            if (img == null || img.getWidth() > 4096 || img.getHeight() > 4096) throw new IOException("not a usable PNG");
            int w = img.getWidth(), h = img.getHeight();
            sink.accept(img.getRGB(0, 0, w, h, null, 0, w), w, h);
        });
    }

    private interface ImageSink {
        void accept(int[] argb, int width, int height);
    }

    private interface Bytes {
        void accept(byte[] data) throws IOException;
    }

    /** Loads a file from the disk cache, or downloads and caches it, off the game thread. */
    private void fetch(final String path, final int version, final int maxBytes, final Bytes onLoaded) {
        if (api == null || cacheDir == null) return;
        Scheduler.async(() -> {
            File f = new File(cacheDir, path.replace('/', '_') + "@" + version);
            try {
                byte[] data;
                if (f.isFile()) {
                    data = Files.readAllBytes(f.toPath());
                } else {
                    if (!api.enabled()) return;
                    data = api.download("/cosmetics/" + path, maxBytes);
                    cacheDir.mkdirs();
                    Files.write(f.toPath(), data);
                }
                onLoaded.accept(data);
            } catch (Exception e) {
                Log.warn("Cosmetic file " + path + " failed: " + e.getMessage());
                f.delete();
            }
        });
    }

    // ------------------------------------------------------------------ parsing helpers

    private static <E extends Enum<E>> E parse(Class<E> type, String name, E def) {
        try {
            return name.isEmpty() ? def : Enum.valueOf(type, name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return def;
        }
    }

    private static Map<String, Object> obj(Map<String, Object> m, String key) {
        Map<String, Object> o = Json.obj(m, key);
        return o == null ? java.util.Collections.<String, Object>emptyMap() : o;
    }

    private static List<Object> arr(Map<String, Object> m, String key) {
        List<Object> o = Json.arr(m, key);
        return o == null ? java.util.Collections.emptyList() : o;
    }

    /** "#RRGGBB" or "#AARRGGBB". */
    static int color(String s, int def) {
        if (s == null || !s.matches("#([0-9a-fA-F]{6}|[0-9a-fA-F]{8})")) return def;
        long v = Long.parseLong(s.substring(1), 16);
        return s.length() == 7 ? (int) (0xFF000000L | v) : (int) v;
    }

    private static int[] palette(Map<String, Object> d) {
        List<Object> list = arr(d, "colors");
        if (list.isEmpty()) return null;
        int[] out = new int[Math.min(16, list.size())];
        for (int i = 0; i < out.length; i++) out[i] = color(String.valueOf(list.get(i)), 0xFFFFFFFF);
        return out;
    }
}
