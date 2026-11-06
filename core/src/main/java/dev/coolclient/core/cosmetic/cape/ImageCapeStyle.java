package dev.coolclient.core.cosmetic.cape;

/**
 * A cape drawn from an image the server sends. The image is either the cape's outer face (any
 * size with a 10:16 shape, e.g. 40 x 64), with animation frames stacked top to bottom, or a
 * whole cape in the vanilla 64 x 32 layout (any 2:1 size; not animated).
 */
public final class ImageCapeStyle implements CapeStyle {
    private static final class Image {
        final int[] argb;
        final int width, frameHeight, frames;
        final boolean vanilla;

        Image(int[] argb, int width, int frameHeight, int frames, boolean vanilla) {
            this.argb = argb;
            this.width = width;
            this.frameHeight = frameHeight;
            this.frames = frames;
            this.vanilla = vanilla;
        }
    }

    private final int frames;
    private final float fps;
    private volatile Image image;
    private volatile int revision;

    public ImageCapeStyle(int frames, float fps) {
        this.frames = Math.max(1, frames);
        this.fps = fps > 0 ? fps : 8f;
    }

    /** Sets the downloaded pixels (any thread). */
    public void setImage(int[] argb, int width, int height) {
        boolean vanilla = width == height * 2;
        int n = vanilla ? 1 : Math.max(1, Math.min(frames, height));
        image = new Image(argb, width, Math.max(1, height / n), n, vanilla);
        revision++;
    }

    public boolean ready() { return image != null; }

    @Override
    public int revision() { return revision; }

    @Override
    public int sample(float u, float v, float t, int tint) {
        Image img = image;
        if (img == null) {
            // Still downloading: a dark cape with a slow shimmer.
            float s = 0.5f + 0.5f * (float) Math.sin((u + v) * 6 - t * 2);
            int g = 0x22 + (int) (s * 0x10);
            return 0xFF000000 | (g << 16) | (g << 8) | (g + 8);
        }
        int x, y;
        if (img.vanilla) {
            x = (int) ((1 + u * 10) * img.width / 64f);
            y = (int) ((1 + v * 16) * img.frameHeight / 32f);
        } else {
            int frame = img.frames > 1 ? ((int) (t * fps) % img.frames + img.frames) % img.frames : 0;
            x = (int) (u * img.width);
            y = (int) (v * img.frameHeight);
            y = Math.min(img.frameHeight - 1, Math.max(0, y)) + frame * img.frameHeight;
        }
        x = Math.min(img.width - 1, Math.max(0, x));
        int i = Math.min(img.argb.length - 1, y * img.width + x);
        return img.argb[i];
    }
}
