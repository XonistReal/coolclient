package dev.coolclient.core.gui.widget;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.api.GuiRenderer;
import dev.coolclient.core.render.Animation;
import dev.coolclient.core.render.Colors;
import dev.coolclient.core.render.Draw;
import dev.coolclient.core.render.Easing;
import dev.coolclient.core.render.Theme;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** iOS-style switch. */
public class Toggle extends Widget {
    private final BooleanSupplier getter;
    private final Consumer<Boolean> setter;
    private final Animation knob;

    public Toggle(BooleanSupplier getter, Consumer<Boolean> setter) {
        this.getter = getter;
        this.setter = setter;
        this.knob = new Animation(180, Easing.OUT_BACK, getter.getAsBoolean() ? 1f : 0f);
        this.w = 22;
        this.h = 11;
    }

    @Override
    protected void draw(GuiRenderer r, float mx, float my) {
        boolean on = getter.getAsBoolean();
        knob.target(on);
        float k = knob.get();
        float kc = Math.max(0f, Math.min(1f, k));
        int track = Colors.lerp(Theme.SURFACE_ACTIVE, Theme.accent(0.3f), kc);
        if (kc > 0.01f) Draw.glow(r, x, y, w, h, h / 2f, 4 * kc, Colors.withAlpha(Theme.accent(0.3f), (int) (60 * kc)));
        Draw.rounded(r, x, y, w, h, h / 2f, track);
        float d = h - 3;
        float kx = x + 1.5f + (w - d - 3) * k;
        Draw.circle(r, kx + d / 2f, y + h / 2f, d / 2f, Colors.lerp(0xFFB8C0D4, 0xFFFFFFFF, kc));
    }

    @Override
    public boolean mouseClicked(float mx, float my, int button) {
        if (button == 0 && isHovered(mx, my)) {
            CoolClient.get().platform().playClickSound();
            setter.accept(!getter.getAsBoolean());
            return true;
        }
        return false;
    }
}
