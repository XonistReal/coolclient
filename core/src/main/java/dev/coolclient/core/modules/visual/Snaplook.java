package dev.coolclient.core.modules.visual;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.event.Events;
import dev.coolclient.core.module.Category;
import dev.coolclient.core.module.Module;
import dev.coolclient.core.setting.ModeSetting;
import dev.coolclient.core.util.Keys;

/** Hold a key for a quick third-person look; let go and you're back where you were. */
public final class Snaplook extends Module {
    public final ModeSetting view = add(new ModeSetting("View", "Front", "Front", "Back"));
    public final ModeSetting mode = add(new ModeSetting("Activation", "Hold", "Hold", "Toggle"));
    private boolean active;
    private int saved;

    public Snaplook() {
        super("Snaplook", "Hold a key to see yourself in third person, release to snap back.", Category.MECHANIC);
        keybind.set(Keys.V);
        on(Events.Key.class, e -> {
            if (e.key != keybind.key() || !game().inWorld() || platform().anyScreenOpen()) return;
            if (mode.is("Hold")) set(e.pressed);
            else if (e.pressed) set(!active);
        });
        on(Events.Tick.class, e -> {
            if (active && (!game().inWorld() || (mode.is("Hold") && !isDown(keybind.key())))) set(false);
        });
    }

    @Override
    public void onKeybind() {}

    private void set(boolean on) {
        if (on == active) return;
        if (on) {
            saved = game().perspective();
            game().setPerspective(view.is("Front") ? 2 : 1);
        } else if (game().inWorld()) {
            game().setPerspective(saved);
        }
        active = on;
    }

    @Override
    protected void onDisable() { set(false); }

    /** True while Snaplook has the camera (so other modules leave the perspective alone). */
    public static boolean holding() {
        CoolClient c = CoolClient.get();
        Snaplook s = c == null ? null : c.modules().get(Snaplook.class);
        return s != null && s.isEnabled() && s.active;
    }
}
