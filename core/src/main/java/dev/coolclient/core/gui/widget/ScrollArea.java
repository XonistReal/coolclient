package dev.coolclient.core.gui.widget;

import dev.coolclient.core.api.GuiRenderer;
import dev.coolclient.core.render.Colors;
import dev.coolclient.core.render.Draw;
import dev.coolclient.core.render.Theme;
import dev.coolclient.core.util.MathUtil;
import dev.coolclient.core.util.Time;

/** Smooth vertical scrolling with a slim scrollbar. */
public final class ScrollArea {
    public float x, y, w, h;
    private float contentHeight;
    private float target;
    private float current;
    private boolean dragging;
    private float dragOffset;

    public void setBounds(float x, float y, float w, float h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        clamp();
    }

    public void setContentHeight(float contentHeight) {
        this.contentHeight = contentHeight;
        clamp();
    }

    public float maxScroll() { return Math.max(0, contentHeight - h); }

    /** Current (animated) scroll offset; subtract from content y. */
    public float offset() { return current; }

    public void reset() {
        target = 0;
        current = 0;
    }

    public void scrollTo(float value) {
        target = value;
        clamp();
    }

    public boolean scroll(float mx, float my, double amount) {
        if (!MathUtil.inside(mx, my, x, y, w, h) || maxScroll() <= 0) return false;
        target -= (float) amount * 28f;
        clamp();
        return true;
    }

    private void clamp() {
        target = MathUtil.clamp(target, 0, maxScroll());
        current = MathUtil.clamp(current, 0, Math.max(maxScroll(), 0));
    }

    /** Updates animation and draws the scrollbar. Call after drawing content. */
    public void update(GuiRenderer r, float mx, float my, boolean mouseDown) {
        if (dragging) {
            if (!mouseDown) {
                dragging = false;
            } else {
                float trackH = h - thumbHeight();
                float frac = trackH <= 0 ? 0 : (my - dragOffset - y) / trackH;
                target = MathUtil.clamp(frac, 0, 1) * maxScroll();
                current = target;
            }
        }
        current = MathUtil.lerp(current, target, MathUtil.smooth(18f, Time.delta()));
        if (Math.abs(current - target) < 0.05f) current = target;
        if (maxScroll() <= 0) return;
        float th = thumbHeight();
        float ty = y + (h - th) * (current / maxScroll());
        boolean hot = dragging || MathUtil.inside(mx, my, x + w - 6, y, 6, h);
        Draw.rounded(r, x + w - 3, ty, 2.5f, th, 1.25f, Theme.highlight(hot ? 110 : 55));
    }

    public boolean mouseClicked(float mx, float my) {
        if (maxScroll() <= 0 || !MathUtil.inside(mx, my, x + w - 6, y, 6, h)) return false;
        float th = thumbHeight();
        float ty = y + (h - th) * (current / maxScroll());
        dragging = true;
        dragOffset = (my >= ty && my <= ty + th) ? my - ty : th / 2f;
        return true;
    }

    private float thumbHeight() {
        return Math.max(16, h * h / Math.max(h, contentHeight));
    }

    public boolean contains(float mx, float my) {
        return MathUtil.inside(mx, my, x, y, w, h);
    }
}
