package dev.coolclient.core.modules.visual;

import dev.coolclient.core.event.Events;
import dev.coolclient.core.module.Category;
import dev.coolclient.core.module.Module;
import dev.coolclient.core.setting.BoolSetting;
import dev.coolclient.core.setting.ModeSetting;

/** Switches to third person while you glide or ride, and back afterwards. */
public final class AutoPerspective extends Module {
    private final BoolSetting gliding = add(new BoolSetting("While Gliding", true));
    private final BoolSetting riding = add(new BoolSetting("While Riding", true).describe("Horses, boats, minecarts ..."));
    private final ModeSetting view = add(new ModeSetting("View", "Back", "Back", "Front"));
    private boolean switched;
    private int saved;

    public AutoPerspective() {
        super("Auto Perspective", "Third person while gliding or riding, first person again after.", Category.MECHANIC);
        on(Events.Tick.class, e -> {
            if (!game().inWorld()) {
                switched = false;
                return;
            }
            boolean want = (gliding.on() && game().gliding()) || (riding.on() && game().riding());
            if (want && !switched) {
                saved = game().perspective();
                game().setPerspective(view.is("Front") ? 2 : 1);
                switched = true;
            } else if (!want && switched) {
                game().setPerspective(saved);
                switched = false;
            }
        });
    }

    @Override
    protected void onDisable() {
        if (switched && game().inWorld()) game().setPerspective(saved);
        switched = false;
    }
}
