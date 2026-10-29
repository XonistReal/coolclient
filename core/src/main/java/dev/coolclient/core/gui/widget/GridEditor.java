package dev.coolclient.core.gui.widget;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.api.GuiRenderer;
import dev.coolclient.core.font.Fonts;
import dev.coolclient.core.render.Draw;
import dev.coolclient.core.render.Theme;
import dev.coolclient.core.setting.GridSetting;
import dev.coolclient.core.util.MathUtil;

/** Pixel editor for {@link GridSetting} (left click paints, right click erases). */
public class GridEditor extends Widget {
    private final GridSetting setting;
    private final float cell;
    private int painting; // 0 none, 1 paint, 2 erase

    public GridEditor(GridSetting setting) {
        this.setting = setting;
        this.cell = 5;
        this.w = setting.size() * cell;
        this.h = setting.size() * cell + 14;
    }

    @Override
    protected void draw(GuiRenderer r, float mx, float my) {
        int n = setting.size();
        if (painting != 0) {
            if (!CoolClient.get().platform().input().isMouseDown(painting == 1 ? 0 : 1)) {
                painting = 0;
                CoolClient.get().config().markDirty();
            } else {
                paint(mx, my);
            }
        }
        float gx = x + w - n * cell;
        Draw.rect(r, gx, y, n * cell, n * cell, 0xFF0B0F18);
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                boolean on = setting.pixel(i, j);
                boolean center = i == n / 2 && j == n / 2;
                int c = on ? 0xFFFFFFFF : (center ? 0xFF2A3550 : ((i + j) % 2 == 0 ? 0xFF141A28 : 0xFF111624));
                Draw.rect(r, gx + i * cell + 0.25f, y + j * cell + 0.25f, cell - 0.5f, cell - 0.5f, c);
            }
        }
        Draw.outlineRect(r, gx, y, n * cell, n * cell, 0.75f, Theme.BORDER_STRONG);
        Fonts.small().drawRight(r, "LMB paint  RMB erase", x + w, y + n * cell + 4, Theme.TEXT_FAINT, false);
    }

    private void paint(float mx, float my) {
        int n = setting.size();
        float gx = x + w - n * cell;
        int i = (int) Math.floor((mx - gx) / cell);
        int j = (int) Math.floor((my - y) / cell);
        if (i >= 0 && j >= 0 && i < n && j < n) setting.setPixel(i, j, painting == 1);
    }

    @Override
    public boolean mouseClicked(float mx, float my, int button) {
        int n = setting.size();
        float gx = x + w - n * cell;
        if (!MathUtil.inside(mx, my, gx, y, n * cell, n * cell)) return false;
        painting = button == 1 ? 2 : 1;
        paint(mx, my);
        return true;
    }

    @Override
    public boolean mouseReleased(float mx, float my, int button) {
        if (painting != 0) {
            painting = 0;
            CoolClient.get().config().markDirty();
            return true;
        }
        return false;
    }
}
