package dev.coolclient.core.gui.widget;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.api.GuiRenderer;
import dev.coolclient.core.font.FontRenderer;
import dev.coolclient.core.font.Fonts;
import dev.coolclient.core.render.Colors;
import dev.coolclient.core.render.Draw;
import dev.coolclient.core.render.Icons;
import dev.coolclient.core.render.Scissor;
import dev.coolclient.core.render.Theme;
import dev.coolclient.core.util.Keys;
import dev.coolclient.core.util.Time;

import java.util.function.Consumer;

/** Single-line text input. */
public class TextField extends Widget {
    private String text = "";
    private String placeholder = "";
    private int cursor;
    private boolean focused;
    private int maxLength = 128;
    private Icons.Icon icon;
    private Consumer<String> onChange;
    private Consumer<String> onSubmit;

    public TextField placeholder(String p) { this.placeholder = p; return this; }

    public TextField icon(Icons.Icon i) { this.icon = i; return this; }

    public TextField maxLength(int m) { this.maxLength = m; return this; }

    public TextField onChange(Consumer<String> c) { this.onChange = c; return this; }

    public TextField onSubmit(Consumer<String> c) { this.onSubmit = c; return this; }

    public String text() { return text; }

    public void setText(String t) {
        text = t == null ? "" : t;
        cursor = text.length();
    }

    public boolean focused() { return focused; }

    public void setFocused(boolean f) { focused = f; }

    @Override
    protected void draw(GuiRenderer r, float mx, float my) {
        FontRenderer f = Fonts.body();
        int border = focused ? Theme.accent() : Colors.lerp(Theme.BORDER, Theme.BORDER_STRONG, hover.get());
        Draw.rounded(r, x, y, w, h, 4, Theme.SURFACE);
        Draw.roundedOutline(r, x, y, w, h, 4, focused ? 1f : 0.75f, border);
        float tx = x + 6;
        if (icon != null) {
            Icons.draw(r, icon, x + 5, y + (h - 8) / 2f, 8, Theme.TEXT_DIM);
            tx += 11;
        }
        float ty = y + (h - f.height()) / 2f;
        Scissor.push(r, tx, y, w - (tx - x) - 4, h);
        if (text.isEmpty() && !focused) {
            f.draw(r, placeholder, tx, ty, Theme.TEXT_FAINT, false);
        } else {
            String before = text.substring(0, Math.min(cursor, text.length()));
            float cursorX = f.width(before);
            float shift = Math.max(0, cursorX - (w - (tx - x) - 10));
            f.draw(r, text, tx - shift, ty, Theme.TEXT, false);
            if (focused && (Time.millis() / 530) % 2 == 0) {
                Draw.rect(r, tx - shift + cursorX, ty - 0.5f, 0.75f, f.height() + 1, Theme.TEXT);
            }
        }
        Scissor.pop(r);
    }

    @Override
    public boolean mouseClicked(float mx, float my, int button) {
        boolean inside = isHovered(mx, my);
        focused = inside;
        if (inside && button == 1) {
            setText("");
            fire();
        }
        return inside;
    }

    @Override
    public boolean capturesKeyboard() { return focused; }

    @Override
    public boolean keyPressed(int key, int mods) {
        if (!focused) return false;
        boolean ctrl = (mods & 2) != 0;
        switch (key) {
            case Keys.BACKSPACE:
                if (cursor > 0) {
                    if (ctrl) {
                        int to = previousWord();
                        text = text.substring(0, to) + text.substring(cursor);
                        cursor = to;
                    } else {
                        text = text.substring(0, cursor - 1) + text.substring(cursor);
                        cursor--;
                    }
                    fire();
                }
                return true;
            case Keys.DELETE:
                if (cursor < text.length()) {
                    text = text.substring(0, cursor) + text.substring(cursor + 1);
                    fire();
                }
                return true;
            case Keys.LEFT:
                cursor = Math.max(0, cursor - 1);
                return true;
            case Keys.RIGHT:
                cursor = Math.min(text.length(), cursor + 1);
                return true;
            case Keys.HOME:
                cursor = 0;
                return true;
            case Keys.END:
                cursor = text.length();
                return true;
            case Keys.ENTER:
            case Keys.KP_ENTER:
                if (onSubmit != null) onSubmit.accept(text);
                focused = false;
                return true;
            case Keys.ESCAPE:
                focused = false;
                return true;
            default:
                break;
        }
        if (ctrl && key == Keys.V) {
            insert(CoolClient.get().platform().clipboard());
            return true;
        }
        if (ctrl && key == Keys.A) {
            cursor = text.length();
            return true;
        }
        if (ctrl && key == Keys.C) {
            CoolClient.get().platform().setClipboard(text);
            return true;
        }
        return true;
    }

    @Override
    public boolean charTyped(char c) {
        if (!focused || c < 32 || c == 127 || c == '§') return false;
        insert(String.valueOf(c));
        return true;
    }

    private void insert(String s) {
        if (s == null || s.isEmpty()) return;
        s = s.replace("\n", " ").replace("\r", "");
        int room = maxLength - text.length();
        if (room <= 0) return;
        if (s.length() > room) s = s.substring(0, room);
        text = text.substring(0, cursor) + s + text.substring(cursor);
        cursor += s.length();
        fire();
    }

    private int previousWord() {
        int i = cursor - 1;
        while (i > 0 && text.charAt(i - 1) == ' ') i--;
        while (i > 0 && text.charAt(i - 1) != ' ') i--;
        return Math.max(0, i);
    }

    private void fire() {
        if (onChange != null) onChange.accept(text);
    }
}
