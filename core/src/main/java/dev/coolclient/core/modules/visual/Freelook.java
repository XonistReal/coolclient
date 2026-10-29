package dev.coolclient.core.modules.visual;

import dev.coolclient.core.event.Events;
import dev.coolclient.core.module.Category;
import dev.coolclient.core.module.Module;
import dev.coolclient.core.setting.BoolSetting;
import dev.coolclient.core.setting.ModeSetting;
import dev.coolclient.core.util.Keys;
import dev.coolclient.core.util.MathUtil;

/**
 * Look around your character in third person without changing where you're facing.
 * Automatically disabled on servers that don't allow it (see ServerRules).
 */
public final class Freelook extends Module {
    public final ModeSetting mode = add(new ModeSetting("Activation", "Hold", "Hold", "Toggle"));
    public final BoolSetting invertPitch = add(new BoolSetting("Invert Pitch", false));
    public final ModeSetting view = add(new ModeSetting("View", "Back", "Back", "Front"));

    private boolean active;
    private float yaw, pitch;
    private int previousPerspective;

    public Freelook() {
        super("Freelook", "Hold a key to orbit the camera around yourself.", Category.MECHANIC);
        keybind.set(Keys.LEFT_ALT);
        on(Events.Key.class, e -> {
            if (e.key != keybind.key()) return;
            if (mode.is("Hold")) setActive(e.pressed);
            else if (e.pressed) setActive(!active);
        });
        on(Events.Tick.class, e -> {
            if (active && (!game().inWorld() || (mode.is("Hold") && !isDown(keybind.key())))) setActive(false);
        });
    }

    @Override
    public void onKeybind() {}

    private void setActive(boolean on) {
        if (on == active) return;
        if (on) {
            yaw = game().yaw();
            pitch = game().pitch();
            previousPerspective = game().perspective();
            game().setPerspective(view.is("Front") ? 2 : 1);
        } else {
            game().setPerspective(previousPerspective);
        }
        active = on;
    }

    @Override
    protected void onDisable() {
        setActive(false);
    }

    public boolean active() { return isEnabled() && active; }

    /** Consumes mouse movement while active (same units as Entity#turn). */
    public void turn(double dx, double dy) {
        yaw += (float) dx * 0.15f;
        pitch = MathUtil.clamp(pitch + (float) (invertPitch.on() ? -dy : dy) * 0.15f, -90f, 90f);
    }

    public float yaw() { return yaw; }

    public float pitch() { return pitch; }
}
