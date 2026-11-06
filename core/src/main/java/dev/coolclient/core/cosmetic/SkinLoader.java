package dev.coolclient.core.cosmetic;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.json.Json;
import dev.coolclient.core.util.Log;
import dev.coolclient.core.util.Scheduler;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The local player's skin for menu figures (the title screen has no player entity to borrow it
 * from). Asked from Mojang's session server, the same place the game gets skins, then cached for
 * a day; Steve stands in while it loads or when there is none.
 */
public final class SkinLoader {
    private static final long CACHE_MS = 24 * 60 * 60_000L;
    private static final String[] DEFAULT_SKINS = {
        "/assets/minecraft/textures/entity/player/wide/steve.png", // 1.20.2+
        "/assets/minecraft/textures/entity/steve.png",             // older versions
    };

    private static volatile int[] pixels;
    private static volatile int width, height;
    private static volatile boolean slim;
    private static volatile boolean fresh;
    private static int handle;
    private static boolean requested;

    private SkinLoader() {}

    /** Texture handle of the skin, or 0 while nothing is loaded yet (draw a mannequin). Game thread. */
    public static int texture() {
        if (!requested) {
            requested = true;
            Scheduler.async(SkinLoader::load);
        }
        if (fresh && pixels != null) {
            if (handle != 0) CoolClient.get().platform().textures().delete(handle);
            handle = CoolClient.get().platform().textures().create("skin/self", width, height, pixels, false);
            fresh = false;
        }
        return handle;
    }

    public static boolean slim() { return slim; }

    /** An old 64 x 32 skin. */
    public static boolean legacy() { return height == width / 2; }

    private static void set(BufferedImage img, boolean isSlim) {
        if (img == null || img.getWidth() != 64 || (img.getHeight() != 64 && img.getHeight() != 32)) return;
        int w = img.getWidth(), h = img.getHeight();
        int[] px = img.getRGB(0, 0, w, h, null, 0, w);
        width = w;
        height = h;
        slim = isSlim;
        pixels = px;
        fresh = true;
    }

    private static void load() {
        // Steve first, so there's something to show straight away.
        for (String path : DEFAULT_SKINS) {
            try (InputStream in = SkinLoader.class.getResourceAsStream(path)) {
                if (in != null) {
                    set(ImageIO.read(in), false);
                    break;
                }
            } catch (IOException ignored) {
            }
        }
        UUID uuid = CoolClient.get().platform().game().selfUuid();
        if (uuid == null) return;
        File dir = new File(CoolClient.get().config().dir(), "cache");
        File png = new File(dir, "skin.png"), meta = new File(dir, "skin.json");
        try {
            if (png.isFile() && meta.isFile()) {
                Map<String, Object> m = Json.parseObject(new String(Files.readAllBytes(meta.toPath()), StandardCharsets.UTF_8));
                if (uuid.toString().equals(Json.str(m, "uuid", "")) && System.currentTimeMillis() - png.lastModified() < CACHE_MS) {
                    set(ImageIO.read(png), Json.bool(m, "slim", false));
                    return;
                }
            }
            String id = uuid.toString().replace("-", "");
            Map<String, Object> profile = Json.parseObject(new String(get("https://sessionserver.mojang.com/session/minecraft/profile/" + id), StandardCharsets.UTF_8));
            List<Object> props = Json.arr(profile, "properties");
            if (props == null) return;
            for (Object o : props) {
                if (!(o instanceof Map)) continue;
                @SuppressWarnings("unchecked")
                Map<String, Object> p = (Map<String, Object>) o;
                if (!"textures".equals(Json.str(p, "name", ""))) continue;
                Map<String, Object> tex = Json.parseObject(new String(Base64.getDecoder().decode(Json.str(p, "value", "")), StandardCharsets.UTF_8));
                Map<String, Object> skin = Json.obj(Json.obj(tex, "textures"), "SKIN");
                if (skin == null) return;
                String url = Json.str(skin, "url", "").replace("http://", "https://");
                if (!url.startsWith("https://textures.minecraft.net/")) return;
                boolean isSlim = "slim".equals(Json.str(Json.obj(skin, "metadata"), "model", ""));
                byte[] data = get(url);
                BufferedImage img = ImageIO.read(new java.io.ByteArrayInputStream(data));
                set(img, isSlim);
                dir.mkdirs();
                Files.write(png.toPath(), data);
                Files.write(meta.toPath(), ("{\"uuid\":\"" + uuid + "\",\"slim\":" + isSlim + "}").getBytes(StandardCharsets.UTF_8));
                return;
            }
        } catch (Exception e) {
            // Offline or an offline-mode account: Steve it is.
            Log.warn("Skin for the menu not loaded: " + e.getMessage());
        }
    }

    private static byte[] get(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(4000);
        c.setReadTimeout(6000);
        c.setRequestProperty("User-Agent", "CoolClient/" + CoolClient.VERSION);
        if (c.getResponseCode() != 200) throw new IOException("HTTP " + c.getResponseCode());
        try (InputStream in = c.getInputStream()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0 && out.size() < (1 << 20)) out.write(buf, 0, n);
            return out.toByteArray();
        }
    }
}
