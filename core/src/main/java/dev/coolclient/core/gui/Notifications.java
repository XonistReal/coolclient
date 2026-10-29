package dev.coolclient.core.gui;

import dev.coolclient.core.api.GuiRenderer;
import dev.coolclient.core.font.FontRenderer;
import dev.coolclient.core.font.Fonts;
import dev.coolclient.core.render.Animation;
import dev.coolclient.core.render.Colors;
import dev.coolclient.core.render.Draw;
import dev.coolclient.core.render.Easing;
import dev.coolclient.core.render.Theme;
import dev.coolclient.core.util.Time;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Toast notifications in the top-right corner. */
public final class Notifications {
    public enum Level { INFO, SUCCESS, WARNING, ERROR }

    private static final class Toast {
        final String title;
        final String message;
        final Level level;
        final long start = Time.millis();
        final long duration;
        final Animation slide = new Animation(320, Easing.OUT_BACK, 0f);
        float y = -1;

        Toast(String title, String message, Level level, long duration) {
            this.title = title;
            this.message = message;
            this.level = level;
            this.duration = duration;
            slide.target(1f);
        }
    }

    private final List<Toast> toasts = new ArrayList<Toast>();

    public synchronized void post(String title, String message, Level level) {
        post(title, message, level, 3500);
    }

    public synchronized void post(String title, String message, Level level, long durationMs) {
        if (toasts.size() >= 5) toasts.remove(0);
        toasts.add(new Toast(title, message, level, durationMs));
    }

    public void info(String title, String message) { post(title, message, Level.INFO); }

    public synchronized void render(GuiRenderer r) {
        FontRenderer tf = Fonts.body();
        FontRenderer mf = Fonts.small();
        float targetY = 6;
        Iterator<Toast> it = toasts.iterator();
        while (it.hasNext()) {
            Toast t = it.next();
            long age = Time.millis() - t.start;
            if (age > t.duration && t.slide.target() != 0f) t.slide.target(0f);
            if (t.slide.target() == 0f && t.slide.done()) {
                it.remove();
                continue;
            }
            float w = Math.max(120, Math.max(tf.width(t.title), mf.width(t.message)) + 26);
            float h = 30;
            if (t.y < 0) t.y = targetY;
            t.y += (targetY - t.y) * 0.25f;
            float s = t.slide.get();
            float x = r.width() - 6 - w * s;
            float y = t.y;
            int accent = color(t.level);
            Draw.shadow(r, x, y, w, h, 5, 8, 0x60000000);
            Draw.panel(r, x, y, w, h, 5, 1f);
            Draw.rounded(r, x, y, 2.5f, h, 5, 0, 0, 5, accent);
            tf.draw(r, t.title, x + 10, y + 5, Theme.TEXT, false);
            mf.draw(r, t.message, x + 10, y + 17, Theme.TEXT_DIM, false);
            float p = 1f - Math.min(1f, age / (float) t.duration);
            Draw.rect(r, x + 5, y + h - 1.5f, (w - 10) * p, 1, Colors.withAlpha(accent, 160));
            targetY += h + 5;
        }
    }

    private static int color(Level l) {
        switch (l) {
            case SUCCESS: return Theme.SUCCESS;
            case WARNING: return Theme.WARNING;
            case ERROR: return Theme.DANGER;
            default: return Theme.accent();
        }
    }
}
