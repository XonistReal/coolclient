package dev.coolclient.core.render.gpu;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.api.Gpu;
import dev.coolclient.core.modules.visual.Skybox;
import dev.coolclient.core.render.Colors;
import dev.coolclient.core.util.Log;
import dev.coolclient.core.util.Mat4;
import dev.coolclient.core.util.Scheduler;
import dev.coolclient.core.util.Time;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Draws the selected {@link Skybox} in place of the vanilla overworld sky. Platforms call
 * {@link #render} from their sky hook with the current projection and model-view matrices and
 * skip the vanilla sky when it returns true.
 */
public final class SkyRenderer {
    private static final float[] TRIANGLE = {-1f, -1f, 3f, -1f, -1f, 3f};
    private static final float[] VIEW_PROJ = new float[16];
    private static final float[] INVERSE = new float[16];
    private static final int MAX_IMAGE_WIDTH = 4096;

    private static String imageName;
    private static int imageTexture;
    private static int imageFog;
    private static boolean imageLoading;
    private static long imageCheckedAt = -1;
    private static boolean drew;

    private SkyRenderer() {}

    /** The enabled skybox module, or null. */
    public static Skybox active() {
        CoolClient c = CoolClient.get();
        if (c == null) return null;
        Skybox s = c.modules().get(Skybox.class);
        return s != null && s.isEnabled() ? s : null;
    }

    /**
     * Draws the sky. Matrices are column-major. Returns true when it drew, so the caller
     * should skip the vanilla sky (sun, moon, stars and sunrise fan included).
     */
    public static boolean render(Gpu gpu, float[] projection, float[] modelView, long worldTime) {
        drew = draw(gpu, projection, modelView, worldTime);
        return drew;
    }

    /** True when the last {@link #render} call replaced the sky (so sun, moon and stars should be skipped too). */
    public static boolean replacing() {
        return drew && active() != null;
    }

    private static boolean draw(Gpu gpu, float[] projection, float[] modelView, long worldTime) {
        Skybox sky = active();
        if (sky == null || gpu == null || !GpuDraw.enabled()) return false;
        SkyStyle style = sky.sky();
        int image = 0;
        if (style == SkyStyle.IMAGE) {
            image = imageTexture(sky.image.get());
            if (image == 0) return false;
        }
        Shaders.Program p = Shaders.sky(gpu, style, false);
        if (p == null) return false;
        Mat4.mul(projection, modelView, VIEW_PROJ);
        if (!Mat4.invert(VIEW_PROJ, INVERSE)) return false;
        float[] sun = sunDirection(timeOfDay(worldTime));
        double seconds = Time.seconds();
        gpu.begin(p.id);
        try {
            gpu.uniformMatrix(p.u("uInvViewProj"), INVERSE);
            gpu.uniform(p.u("uSun"), sun[0], sun[1], sun[2], daylight(sun[1]));
            float rot = (float) ((seconds * sky.rotation.d() * 0.004) % (Math.PI * 2));
            gpu.uniform(p.u("uSky"), (float) (seconds % 3600.0), rot, sky.brightness.f() / 100f, sky.sunMoon.on() ? 1f : 0f);
            if (image != 0) {
                gpu.bindTexture(0, gpu.glTexture(image));
                gpu.uniformInt(p.u("uImage"), 0);
            }
            gpu.draw(TRIANGLE, 3);
        } finally {
            gpu.end();
        }
        return true;
    }

    /** Fog/clear colour matching the active sky's horizon, or 0 to keep vanilla's. */
    public static int fogColor() {
        Skybox sky = active();
        if (sky == null || !sky.tintFog.on() || !GpuDraw.enabled()) return 0;
        SkyStyle style = sky.sky();
        int c = style == SkyStyle.IMAGE ? (imageTexture != 0 ? imageFog : 0) : style.fog;
        if (c == 0) return 0;
        float b = sky.brightness.f() / 100f;
        return Colors.argb(255, Math.round(Colors.r(c) * b), Math.round(Colors.g(c) * b), Math.round(Colors.b(c) * b));
    }

    // ================================================================== sun

    /** Vanilla's celestial angle (0 = noon, 0.5 = midnight) for a world time in ticks. */
    public static float timeOfDay(long worldTime) {
        double d = (worldTime % 24000L) / 24000.0 - 0.25;
        d -= Math.floor(d);
        double e = 0.5 - Math.cos(d * Math.PI) / 2.0;
        return (float) ((d * 2.0 + e) / 3.0);
    }

    /** World-space sun direction for a celestial angle (rises in +X, like vanilla). */
    public static float[] sunDirection(float timeOfDay) {
        double a = timeOfDay * Math.PI * 2;
        return new float[]{(float) -Math.sin(a), (float) Math.cos(a), 0f};
    }

    public static float daylight(float sunY) {
        float t = Math.max(0f, Math.min(1f, (sunY + 0.15f) / 0.35f));
        return t * t * (3f - 2f * t);
    }

    /** A 40-second day for the previews in the theme page. */
    public static float previewTimeOfDay(double seconds) {
        return (float) ((seconds / 40.0 + 0.8) % 1.0);
    }

    // ================================================================== custom images

    public static File folder() {
        return new File(CoolClient.get().config().dir(), "skyboxes");
    }

    /**
     * Texture handle for a panorama in the skyboxes folder; 0 while it's missing or loading.
     * A missing file is re-checked every couple of seconds, so dropping one in just works.
     */
    public static int imageTexture(String name) {
        name = name == null ? "" : name.trim();
        if (!name.equals(imageName)) {
            if (imageTexture != 0) CoolClient.get().platform().textures().delete(imageTexture);
            imageTexture = 0;
            imageName = name;
            imageCheckedAt = -1;
        }
        if (imageTexture == 0 && !imageLoading && !name.isEmpty() && Time.millis() - imageCheckedAt > 2000) {
            imageCheckedAt = Time.millis();
            if (new File(folder(), name).isFile()) load(name);
        }
        return imageTexture;
    }

    private static void load(final String name) {
        imageLoading = true;
        final File file = new File(folder(), name);
        Scheduler.async(() -> {
            try {
                BufferedImage img = ImageIO.read(file);
                if (img == null) throw new IllegalStateException("unsupported image format");
                if (img.getWidth() > MAX_IMAGE_WIDTH) {
                    int h = Math.max(1, img.getHeight() * MAX_IMAGE_WIDTH / img.getWidth());
                    BufferedImage scaled = new BufferedImage(MAX_IMAGE_WIDTH, h, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g = scaled.createGraphics();
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g.drawImage(img, 0, 0, MAX_IMAGE_WIDTH, h, null);
                    g.dispose();
                    img = scaled;
                }
                final int w = img.getWidth(), h = img.getHeight();
                final int[] argb = img.getRGB(0, 0, w, h, null, 0, w);
                final int fog = horizonColor(argb, w, h);
                Scheduler.main(() -> {
                    imageLoading = false;
                    if (!name.equals(imageName)) {
                        // The setting changed while this one was decoding; load the new one.
                        if (imageName != null && !imageName.isEmpty()) load(imageName);
                        return;
                    }
                    imageTexture = CoolClient.get().platform().textures().create("skybox", w, h, argb, true);
                    imageFog = fog;
                });
            } catch (Throwable t) {
                Log.warn("Skybox image " + name + ": " + t.getMessage());
                Scheduler.main(() -> {
                    imageLoading = false;
                    CoolClient.get().notifications().post("Skybox image", t.getMessage(),
                            dev.coolclient.core.gui.Notifications.Level.WARNING);
                });
            }
        });
    }

    /** Average colour of the band just above the horizon (the middle rows). */
    private static int horizonColor(int[] argb, int w, int h) {
        long r = 0, g = 0, b = 0, n = 0;
        int y0 = Math.max(0, h / 2 - h / 16), y1 = Math.min(h, h / 2);
        for (int y = y0; y < y1; y++) {
            for (int x = 0; x < w; x += 4) {
                int c = argb[y * w + x];
                r += (c >> 16) & 0xFF;
                g += (c >> 8) & 0xFF;
                b += c & 0xFF;
                n++;
            }
        }
        if (n == 0) return 0;
        return Colors.rgb((int) (r / n), (int) (g / n), (int) (b / n));
    }
}
