package dev.coolclient.core.gui.widget;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.api.GuiRenderer;
import dev.coolclient.core.font.FontRenderer;
import dev.coolclient.core.font.Fonts;
import dev.coolclient.core.render.Colors;
import dev.coolclient.core.render.Draw;
import dev.coolclient.core.render.Icons;
import dev.coolclient.core.render.Theme;
import dev.coolclient.core.setting.ModeSetting;
import dev.coolclient.core.util.MathUtil;

/** "< Mode >" selector: left/right halves cycle backwards/forwards. */
public class ModeSelector extends Widget {
    private final ModeSetting setting;

    public ModeSelector(ModeSetting setting) {
        this.setting = setting;
        this.w = 100;
        this.h = 13;
    }

    @Override
    protected void draw(GuiRenderer r, float mx, float my) {
        FontRenderer f = Fonts.small();
        Draw.rounded(r, x, y, w, h, 3.5f, Colors.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, hover.get()));
        Draw.roundedOutline(r, x, y, w, h, 3.5f, 0.75f, Theme.BORDER);
        boolean leftHot = MathUtil.inside(mx, my, x, y, 14, h);
        boolean rightHot = MathUtil.inside(mx, my, x + w - 14, y, 14, h);
        Icons.draw(r, Icons.Icon.CHEVRON_LEFT, x + 2, y + (h - 9) / 2f, 9, leftHot ? Theme.TEXT : Theme.TEXT_DIM);
        Icons.draw(r, Icons.Icon.CHEVRON_RIGHT, x + w - 11, y + (h - 9) / 2f, 9, rightHot ? Theme.TEXT : Theme.TEXT_DIM);
        String s = f.trim(setting.get(), w - 28);
        f.drawCentered(r, s, x + w / 2f, y + (h - f.height()) / 2f, Theme.TEXT, false);
    }

    @Override
    public boolean mouseClicked(float mx, float my, int button) {
        if (!isHovered(mx, my)) return false;
        boolean back = button == 1 || mx < x + w / 2f - 10;
        setting.cycle(back ? -1 : 1);
        CoolClient.get().platform().playClickSound();
        CoolClient.get().config().markDirty();
        return true;
    }
}
