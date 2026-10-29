package dev.coolclient.core.gui;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.api.GuiRenderer;
import dev.coolclient.core.render.Animation;
import dev.coolclient.core.render.Easing;
import dev.coolclient.core.util.Keys;

/**
 * A full screen written against core APIs. Each platform wraps it in a vanilla Screen subclass
 * that forwards rendering and input.
 */
public abstract class UIScreen {
    protected float width;
    protected float height;
    protected UIScreen parent;
    protected final Animation openAnim = new Animation(260, Easing.OUT_CUBIC, 0f);
    private boolean closing;

    protected UIScreen(UIScreen parent) {
        this.parent = parent;
    }

    /** Called when shown and whenever the window is resized. */
    public final void init(float width, float height) {
        this.width = width;
        this.height = height;
        if (openAnim.target() == 0f) openAnim.target(1f);
        layout();
    }

    protected void layout() {}

    public abstract void render(GuiRenderer r, float mouseX, float mouseY, float delta);

    public void tick() {}

    public boolean mouseClicked(float mx, float my, int button) { return false; }

    public boolean mouseReleased(float mx, float my, int button) { return false; }

    public boolean mouseScrolled(float mx, float my, double amount) { return false; }

    public boolean keyPressed(int key, int mods) {
        if (key == Keys.ESCAPE && closeOnEscape()) {
            close();
            return true;
        }
        return false;
    }

    public boolean charTyped(char c) { return false; }

    public boolean closeOnEscape() { return true; }

    public boolean pausesGame() { return false; }

    /** Whether the platform should blur the world behind this screen (where supported). */
    public boolean blursWorld() { return false; }

    /** Called by the platform when the screen is removed. */
    public void onClose() {}

    public void close() {
        if (closing) return;
        closing = true;
        CoolClient.get().platform().openScreen(parent);
    }

    public UIScreen parent() { return parent; }

    public float width() { return width; }

    public float height() { return height; }

    public float openProgress() { return openAnim.get(); }
}
