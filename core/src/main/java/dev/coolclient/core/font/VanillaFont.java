package dev.coolclient.core.font;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.api.GuiRenderer;

/** The game's own bitmap font, optionally scaled. */
public final class VanillaFont implements FontRenderer {
    private final float scale;

    public VanillaFont(float scale) {
        this.scale = scale;
    }

    @Override
    public float draw(GuiRenderer r, String text, float x, float y, int color, boolean shadow) {
        if (scale == 1f) {
            r.vanillaText(text, x, y, color, shadow);
        } else {
            r.push();
            r.translate(x, y);
            r.scale(scale, scale);
            r.vanillaText(text, 0, 0, color, shadow);
            r.pop();
        }
        return width(text);
    }

    @Override
    public float width(String text) {
        GuiRenderer r = CoolClient.get().lastRenderer();
        return r == null ? text.length() * 6 * scale : r.vanillaTextWidth(text) * scale;
    }

    @Override
    public float height() {
        return 9f * scale;
    }
}
