package dev.coolclient.core.gui.widget;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.api.GuiRenderer;
import dev.coolclient.core.font.FontRenderer;
import dev.coolclient.core.font.Fonts;
import dev.coolclient.core.render.Animation;
import dev.coolclient.core.render.Colors;
import dev.coolclient.core.render.Draw;
import dev.coolclient.core.render.Theme;
import dev.coolclient.core.setting.ColorSetting;
import dev.coolclient.core.util.MathUtil;

/** Swatch that expands into an HSV picker with alpha and chroma controls. */
public class ColorPicker extends Widget {
    private static final float COLLAPSED = 13;
    private static final float EXPANDED = 86;

    private final ColorSetting setting;
    private final Animation expand = new Animation(200, 0f);
    private boolean open;
    private int dragging; // 0 none, 1 sv, 2 hue, 3 alpha, 4 speed
    private float hue, sat, bri;

    public ColorPicker(ColorSetting setting) {
        this.setting = setting;
        this.w = 150;
        this.h = COLLAPSED;
        syncFromSetting();
    }

    private void syncFromSetting() {
        float[] hsb = Colors.toHsb(setting.get());
        if (hsb[1] > 0.001f && hsb[2] > 0.001f) hue = hsb[0];
        sat = hsb[1];
        bri = hsb[2];
    }

    @Override
    public float preferredHeight() {
        return COLLAPSED + (EXPANDED - COLLAPSED) * expand.get();
    }

    public boolean isOpen() { return open; }

    @Override
    protected void draw(GuiRenderer r, float mx, float my) {
        FontRenderer f = Fonts.small();
        expand.target(open);
        float e = expand.get();

        // Header: swatch + hex
        float sw = 22;
        float sx = x + w - sw;
        drawChecker(r, sx, y + 1, sw, 11);
        Draw.rounded(r, sx, y + 1, sw, 11, 3, setting.color());
        Draw.roundedOutline(r, sx, y + 1, sw, 11, 3, 0.75f, Theme.BORDER_STRONG);
        String hex = setting.chroma() ? "Chroma" : Colors.hex(setting.get(), setting.allowAlpha());
        f.drawRight(r, hex, sx - 5, y + (COLLAPSED - f.height()) / 2f, Theme.TEXT_DIM, false);

        if (e < 0.02f) return;
        updateDrag(mx, my);
        float py = y + COLLAPSED + 4;
        float ph = (EXPANDED - COLLAPSED - 8) * e;
        if (ph < 4) return;
        dev.coolclient.core.render.Scissor.push(r, x, py, w, ph);

        float svW = w - 48, svH = 52;
        // Saturation/value square
        int hueColor = Colors.hsb(hue, 1, 1);
        Draw.gradientH(r, x, py, svW, svH, 0xFFFFFFFF, hueColor);
        Draw.gradientV(r, x, py, svW, svH, 0x00000000, 0xFF000000);
        Draw.roundedOutline(r, x, py, svW, svH, 2, 0.75f, Theme.BORDER);
        float cx = x + sat * svW, cy = py + (1 - bri) * svH;
        Draw.ring(r, cx, cy, 3, 1.2f, 0xFFFFFFFF);

        // Hue bar
        float hx = x + svW + 6;
        for (int i = 0; i < 6; i++) {
            Draw.gradientV(r, hx, py + svH * i / 6f, 8, svH / 6f + 0.3f, Colors.hsb(i / 6f, 1, 1), Colors.hsb((i + 1) / 6f, 1, 1));
        }
        Draw.rect(r, hx - 1, py + hue * svH - 0.75f, 10, 1.5f, 0xFFFFFFFF);

        // Alpha bar
        if (setting.allowAlpha()) {
            float ax = hx + 14;
            drawChecker(r, ax, py, 8, svH);
            Draw.gradientV(r, ax, py, 8, svH, Colors.withAlpha(setting.get(), 255), Colors.withAlpha(setting.get(), 0));
            float a = Colors.a(setting.get()) / 255f;
            Draw.rect(r, ax - 1, py + (1 - a) * svH - 0.75f, 10, 1.5f, 0xFFFFFFFF);
        }

        // Chroma toggle + speed
        float ry = py + svH + 6;
        boolean ch = setting.chroma();
        Draw.rounded(r, x, ry, 9, 9, 2, ch ? Theme.accent() : Theme.SURFACE_ACTIVE);
        if (ch) dev.coolclient.core.render.Icons.draw(r, dev.coolclient.core.render.Icons.Icon.CHECK, x, ry, 9, Theme.ON_ACCENT);
        f.draw(r, "Chroma", x + 13, ry + (9 - f.height()) / 2f, Theme.TEXT, false);
        if (ch) {
            float tx = x + 52, tw = w - 52 - 30;
            Draw.rounded(r, tx, ry + 3, tw, 3, 1.5f, Theme.SURFACE_ACTIVE);
            float frac = (float) ((setting.chromaSpeed() - 0.1) / 4.9);
            Draw.roundedGradient(r, tx, ry + 3, tw * frac, 3, 1.5f, Theme.accent(0), Theme.accent(1), true);
            Draw.circle(r, tx + tw * frac, ry + 4.5f, 3, 0xFFFFFFFF);
            f.drawRight(r, MathUtil.format(setting.chromaSpeed(), 1) + "x", x + w, ry + (9 - f.height()) / 2f, Theme.TEXT_DIM, false);
        }
        dev.coolclient.core.render.Scissor.pop(r);
    }

    private void drawChecker(GuiRenderer r, float x, float y, float w, float h) {
        Draw.rect(r, x, y, w, h, 0xFFBFBFBF);
        float s = 3;
        for (float iy = 0; iy < h; iy += s) {
            for (float ix = ((int) (iy / s) % 2) * s; ix < w; ix += s * 2) {
                Draw.rect(r, x + ix, y + iy, Math.min(s, w - ix), Math.min(s, h - iy), 0xFF808080);
            }
        }
    }

    private void updateDrag(float mx, float my) {
        if (dragging == 0) return;
        if (!CoolClient.get().platform().input().isMouseDown(0)) {
            dragging = 0;
            CoolClient.get().config().markDirty();
            return;
        }
        float py = y + COLLAPSED + 4;
        float svW = w - 48, svH = 52;
        switch (dragging) {
            case 1:
                sat = MathUtil.clamp((mx - x) / svW, 0, 1);
                bri = 1 - MathUtil.clamp((my - py) / svH, 0, 1);
                apply();
                break;
            case 2:
                hue = MathUtil.clamp((my - py) / svH, 0, 0.999f);
                apply();
                break;
            case 3: {
                float a = 1 - MathUtil.clamp((my - py) / svH, 0, 1);
                setting.set(Colors.withAlpha(setting.get(), Math.round(a * 255)));
                break;
            }
            case 4: {
                float tx = x + 52, tw = w - 52 - 30;
                setting.setChromaSpeed(0.1 + 4.9 * MathUtil.clamp((mx - tx) / tw, 0, 1));
                break;
            }
            default:
                break;
        }
    }

    private void apply() {
        int rgb = Colors.hsb(hue, sat, bri);
        setting.set((setting.get() & 0xFF000000) | (rgb & 0xFFFFFF));
    }

    @Override
    public boolean mouseClicked(float mx, float my, int button) {
        if (MathUtil.inside(mx, my, x, y, w, COLLAPSED)) {
            open = !open;
            if (open) syncFromSetting();
            CoolClient.get().platform().playClickSound();
            return true;
        }
        if (!open || button != 0) return false;
        float py = y + COLLAPSED + 4;
        float svW = w - 48, svH = 52;
        if (MathUtil.inside(mx, my, x, py, svW, svH)) dragging = 1;
        else if (MathUtil.inside(mx, my, x + svW + 5, py, 10, svH)) dragging = 2;
        else if (setting.allowAlpha() && MathUtil.inside(mx, my, x + svW + 19, py, 10, svH)) dragging = 3;
        else if (MathUtil.inside(mx, my, x, py + svH + 5, 48, 11)) {
            setting.setChroma(!setting.chroma());
            CoolClient.get().config().markDirty();
            return true;
        } else if (setting.chroma() && MathUtil.inside(mx, my, x + 50, py + svH + 5, w - 80, 11)) dragging = 4;
        else return MathUtil.inside(mx, my, x, y, w, preferredHeight());
        updateDrag(mx, my);
        return true;
    }

    @Override
    public boolean mouseReleased(float mx, float my, int button) {
        if (dragging != 0) {
            dragging = 0;
            CoolClient.get().config().markDirty();
            return true;
        }
        return false;
    }
}
