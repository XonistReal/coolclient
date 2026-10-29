package dev.coolclient.core.modules.visual;

import dev.coolclient.core.event.Events;
import dev.coolclient.core.module.Category;
import dev.coolclient.core.module.Module;
import dev.coolclient.core.setting.BoolSetting;
import dev.coolclient.core.setting.ModeSetting;
import dev.coolclient.core.setting.NumberSetting;
import dev.coolclient.core.util.Keys;
import dev.coolclient.core.util.MathUtil;
import dev.coolclient.core.util.Time;

/** OptiFine-style zoom with scroll adjust and smooth transitions. */
public final class Zoom extends Module {
    public final ModeSetting mode = add(new ModeSetting("Activation", "Hold", "Hold", "Toggle"));
    public final NumberSetting factor = add(new NumberSetting("Zoom Factor", 4, 2, 20, 0.5).suffix("x"));
    public final BoolSetting smooth = add(new BoolSetting("Smooth Zoom", true));
    public final BoolSetting scroll = add(new BoolSetting("Scroll To Adjust", true));
    public final BoolSetting cinematic = add(new BoolSetting("Cinematic Camera", false));
    public final BoolSetting resetScroll = add(new BoolSetting("Reset Scroll On Release", true));

    private boolean active;
    private float extra = 1f;
    private float current = 1f;

    public Zoom() {
        super("Zoom", "Hold a key to zoom in.", Category.MECHANIC);
        keybind.set(Keys.C);
        enabledByDefault();
        on(Events.Key.class, e -> {
            if (e.key != keybind.key()) return;
            if (mode.is("Hold")) setActive(e.pressed);
            else if (e.pressed) setActive(!active);
        });
        on(Events.MouseButton.class, e -> {
            if (Keys.mouse(e.button) != keybind.key()) return;
            if (mode.is("Hold")) setActive(e.pressed);
            else if (e.pressed) setActive(!active);
        });
        on(Events.Scroll.class, e -> {
            if (!active || !scroll.on()) return;
            extra = MathUtil.clamp(extra * (e.amount > 0 ? 1.18f : 1f / 1.18f), 0.5f, 6f);
            e.cancel();
        });
        on(Events.Tick.class, e -> {
            if (active && mode.is("Hold") && !isDown(keybind.key())) setActive(false);
        });
    }

    /** The module keybind is used as the zoom key, so don't toggle the module itself. */
    @Override
    public void onKeybind() {}

    private void setActive(boolean on) {
        if (on == active) return;
        active = on;
        if (!on && resetScroll.on()) extra = 1f;
    }

    @Override
    protected void onDisable() {
        active = false;
        current = 1f;
    }

    public boolean zooming() { return isEnabled() && active; }

    /** Multiplier applied to the FOV this frame (1 = no zoom). */
    public float fovMultiplier() {
        if (!isEnabled()) return 1f;
        float target = active ? factor.f() * extra : 1f;
        if (smooth.on()) {
            current = MathUtil.lerp(current, target, MathUtil.smooth(14f, Time.delta()));
            if (Math.abs(current - target) < 0.01f) current = target;
        } else {
            current = target;
        }
        return 1f / current;
    }

    public boolean cinematicActive() { return zooming() && cinematic.on(); }
}
