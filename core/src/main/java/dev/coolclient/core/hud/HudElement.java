package dev.coolclient.core.hud;

import dev.coolclient.core.api.Feature;
import dev.coolclient.core.api.GuiRenderer;
import dev.coolclient.core.font.Fonts;
import dev.coolclient.core.json.Json;
import dev.coolclient.core.module.Category;
import dev.coolclient.core.module.Module;
import dev.coolclient.core.util.MathUtil;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A draggable HUD element. Position is stored as an anchor (one of nine screen points) plus
 * an offset, so layouts survive resolution and GUI-scale changes.
 */
public abstract class HudElement extends Module {
    public enum Anchor {
        TOP_LEFT(0, 0), TOP_CENTER(0.5f, 0), TOP_RIGHT(1, 0),
        MIDDLE_LEFT(0, 0.5f), CENTER(0.5f, 0.5f), MIDDLE_RIGHT(1, 0.5f),
        BOTTOM_LEFT(0, 1), BOTTOM_CENTER(0.5f, 1), BOTTOM_RIGHT(1, 1);

        public final float fx, fy;

        Anchor(float fx, float fy) {
            this.fx = fx;
            this.fy = fy;
        }
    }

    private Anchor anchor = Anchor.TOP_LEFT;
    private float offsetX = 4, offsetY = 4;
    private float scale = 1f;
    private Anchor defAnchor = Anchor.TOP_LEFT;
    private float defX = 4, defY = 4;
    protected float width = 40, height = 12;

    protected HudElement(String name, String description, Feature... requires) {
        super(name, description, Category.HUD, requires);
    }

    protected HudElement(String name, String description, Category category, Feature... requires) {
        super(name, description, category, requires);
    }

    protected void defaultPosition(Anchor a, float x, float y) {
        defAnchor = anchor = a;
        defX = offsetX = x;
        defY = offsetY = y;
    }

    /** Draws at local origin (0,0). Must call {@link #size} with the content size. */
    protected abstract void draw(GuiRenderer r, boolean editing);

    /** Whether to render right now outside the editor (e.g. hide combo counter at 0). */
    public boolean shouldRender() { return true; }

    protected void size(float w, float h) {
        this.width = Math.max(1, w);
        this.height = Math.max(1, h);
    }

    public final void render(GuiRenderer r, boolean editing) {
        float x = screenX(r.width()), y = screenY(r.height());
        r.push();
        r.translate(x, y);
        r.scale(scale, scale);
        Fonts.setExtraScale(scale);
        try {
            draw(r, editing);
        } finally {
            Fonts.setExtraScale(1f);
            r.pop();
        }
    }

    // ------------------------------------------------------------------ geometry

    public float scaledWidth() { return width * scale; }

    public float scaledHeight() { return height * scale; }

    public float screenX(float screenW) {
        return screenW * anchor.fx + offsetX - scaledWidth() * anchor.fx;
    }

    public float screenY(float screenH) {
        return screenH * anchor.fy + offsetY - scaledHeight() * anchor.fy;
    }

    /** Moves the element so its top-left lands at (x, y), re-picking the nearest anchor. */
    public void setScreenPosition(float x, float y, float screenW, float screenH) {
        x = MathUtil.clamp(x, 0, Math.max(0, screenW - scaledWidth()));
        y = MathUtil.clamp(y, 0, Math.max(0, screenH - scaledHeight()));
        float cx = (x + scaledWidth() / 2f) / screenW;
        float cy = (y + scaledHeight() / 2f) / screenH;
        int col = cx < 1f / 3f ? 0 : (cx < 2f / 3f ? 1 : 2);
        int row = cy < 1f / 3f ? 0 : (cy < 2f / 3f ? 1 : 2);
        anchor = Anchor.values()[row * 3 + col];
        offsetX = x - (screenW * anchor.fx - scaledWidth() * anchor.fx);
        offsetY = y - (screenH * anchor.fy - scaledHeight() * anchor.fy);
    }

    public float scale() { return scale; }

    public void setScale(float s) { scale = MathUtil.clamp(Math.round(s * 20f) / 20f, 0.5f, 3f); }

    public void resetPosition() {
        anchor = defAnchor;
        offsetX = defX;
        offsetY = defY;
        scale = 1f;
    }

    // ------------------------------------------------------------------ persistence

    @Override
    public Map<String, Object> save() {
        Map<String, Object> m = super.save();
        Map<String, Object> pos = new LinkedHashMap<String, Object>();
        pos.put("anchor", anchor.name());
        pos.put("x", offsetX);
        pos.put("y", offsetY);
        pos.put("scale", scale);
        m.put("hud", pos);
        return m;
    }

    @Override
    public void load(Map<String, Object> m) {
        super.load(m);
        Map<String, Object> pos = Json.obj(m, "hud");
        if (pos == null) return;
        try {
            anchor = Anchor.valueOf(Json.str(pos, "anchor", defAnchor.name()));
        } catch (IllegalArgumentException e) {
            anchor = defAnchor;
        }
        offsetX = (float) Json.num(pos, "x", defX);
        offsetY = (float) Json.num(pos, "y", defY);
        setScale((float) Json.num(pos, "scale", 1));
    }
}
