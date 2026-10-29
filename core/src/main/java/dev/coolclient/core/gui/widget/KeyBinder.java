package dev.coolclient.core.gui.widget;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.api.GuiRenderer;
import dev.coolclient.core.font.FontRenderer;
import dev.coolclient.core.font.Fonts;
import dev.coolclient.core.render.Colors;
import dev.coolclient.core.render.Draw;
import dev.coolclient.core.render.Theme;
import dev.coolclient.core.setting.KeySetting;
import dev.coolclient.core.util.Keys;
import dev.coolclient.core.util.Time;

/** Click, then press a key or mouse button. ESC/BACKSPACE clears the binding. */
public class KeyBinder extends Widget {
    private final KeySetting setting;
    private boolean listening;
    private long listenStart;

    public KeyBinder(KeySetting setting) {
        this.setting = setting;
        this.w = 62;
        this.h = 13;
    }

    @Override
    protected void draw(GuiRenderer r, float mx, float my) {
        FontRenderer f = Fonts.small();
        int border = listening ? Theme.accent() : Colors.lerp(Theme.BORDER, Theme.BORDER_STRONG, hover.get());
        Draw.rounded(r, x, y, w, h, 3.5f, listening ? Theme.SURFACE_ACTIVE : Theme.SURFACE);
        Draw.roundedOutline(r, x, y, w, h, 3.5f, 0.75f, border);
        String text;
        if (listening) {
            int dots = (int) ((Time.millis() / 300) % 4);
            text = "Press" + "....".substring(0, dots);
        } else {
            text = setting.bound() ? setting.keyName() : "None";
        }
        f.drawCentered(r, text, x + w / 2f, y + (h - f.height()) / 2f,
                listening ? Theme.TEXT : (setting.bound() ? Theme.TEXT : Theme.TEXT_FAINT), false);
    }

    @Override
    public boolean mouseClicked(float mx, float my, int button) {
        if (listening) {
            // Ignore the click that started listening; bind any later mouse button.
            if (Time.millis() - listenStart < 150) return true;
            setting.set(Keys.mouse(button));
            listening = false;
            CoolClient.get().config().markDirty();
            return true;
        }
        if (button == 0 && isHovered(mx, my)) {
            listening = true;
            listenStart = Time.millis();
            return true;
        }
        if (button == 1 && isHovered(mx, my)) {
            setting.set(Keys.NONE);
            CoolClient.get().config().markDirty();
            return true;
        }
        return false;
    }

    @Override
    public boolean capturesKeyboard() { return listening; }

    @Override
    public boolean keyPressed(int key, int mods) {
        if (!listening) return false;
        setting.set(key == Keys.ESCAPE || key == Keys.BACKSPACE || key == Keys.DELETE ? Keys.NONE : key);
        listening = false;
        CoolClient.get().config().markDirty();
        return true;
    }
}
