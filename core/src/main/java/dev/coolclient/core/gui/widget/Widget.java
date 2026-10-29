package dev.coolclient.core.gui.widget;

import dev.coolclient.core.api.GuiRenderer;
import dev.coolclient.core.render.Animation;
import dev.coolclient.core.util.MathUtil;

/** Base class for interactive UI elements. */
public abstract class Widget {
    public float x;
    public float y;
    public float w;
    public float h;
    protected final Animation hover = new Animation(140, 0f);

    public void setBounds(float x, float y, float w, float h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public final void render(GuiRenderer r, float mx, float my) {
        hover.target(isHovered(mx, my));
        draw(r, mx, my);
    }

    protected abstract void draw(GuiRenderer r, float mx, float my);

    public boolean isHovered(float mx, float my) {
        return MathUtil.inside(mx, my, x, y, w, h);
    }

    public boolean mouseClicked(float mx, float my, int button) { return false; }

    public boolean mouseReleased(float mx, float my, int button) { return false; }

    public boolean mouseScrolled(float mx, float my, double amount) { return false; }

    public boolean keyPressed(int key, int mods) { return false; }

    public boolean charTyped(char c) { return false; }

    /** Height this widget wants in a vertical list (may change, e.g. an expanded picker). */
    public float preferredHeight() { return h; }

    /** True while the widget captures keyboard input (text field, key binder). */
    public boolean capturesKeyboard() { return false; }
}
