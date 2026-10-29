package dev.coolclient.core.gui.widget;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.api.GuiRenderer;
import dev.coolclient.core.font.FontRenderer;
import dev.coolclient.core.font.Fonts;
import dev.coolclient.core.render.Colors;
import dev.coolclient.core.render.Draw;
import dev.coolclient.core.render.Theme;
import dev.coolclient.core.setting.NumberSetting;
import dev.coolclient.core.util.MathUtil;

public class Slider extends Widget {
    private final NumberSetting setting;
    private boolean dragging;
    private float shown;

    public Slider(NumberSetting setting) {
        this.setting = setting;
        this.w = 120;
        this.h = 11;
        this.shown = (float) setting.fraction();
    }

    @Override
    protected void draw(GuiRenderer r, float mx, float my) {
        FontRenderer f = Fonts.small();
        String val = setting.display();
        float valW = Math.max(f.width(val), 20);
        float trackX = x, trackW = w - valW - 6;
        if (dragging) {
            if (!CoolClient.get().platform().input().isMouseDown(0)) {
                dragging = false;
                CoolClient.get().config().markDirty();
            } else {
                setting.setFraction((mx - trackX) / trackW);
            }
        }
        shown = MathUtil.lerp(shown, (float) setting.fraction(), 0.35f);
        float ty = y + h / 2f - 1.5f;
        Draw.rounded(r, trackX, ty, trackW, 3, 1.5f, Theme.SURFACE_ACTIVE);
        float fillW = trackW * shown;
        if (fillW > 0.5f) Draw.roundedGradient(r, trackX, ty, fillW, 3, 1.5f, Theme.accent(0f), Theme.accent(1f), true);
        float kx = trackX + fillW;
        float kr = 3.5f + (dragging ? 1f : hover.get() * 0.6f);
        Draw.glow(r, kx - kr, y + h / 2f - kr, kr * 2, kr * 2, kr, 3, Colors.withAlpha(Theme.accent(shown), 80));
        Draw.circle(r, kx, y + h / 2f, kr, 0xFFFFFFFF);
        f.drawRight(r, val, x + w, y + (h - f.height()) / 2f, Theme.TEXT_DIM, false);
    }

    @Override
    public boolean mouseClicked(float mx, float my, int button) {
        if (button == 0 && MathUtil.inside(mx, my, x - 3, y - 2, w + 6, h + 4)) {
            dragging = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(float mx, float my, int button) {
        if (dragging) {
            dragging = false;
            CoolClient.get().config().markDirty();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(float mx, float my, double amount) {
        if (!isHovered(mx, my)) return false;
        setting.set(setting.get() + setting.step() * (amount > 0 ? 1 : -1));
        CoolClient.get().config().markDirty();
        return true;
    }
}
