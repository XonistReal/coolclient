package dev.coolclient.core.modules.visual;

import dev.coolclient.core.module.Category;
import dev.coolclient.core.module.Module;
import dev.coolclient.core.setting.ColorSetting;
import dev.coolclient.core.setting.ModeSetting;
import dev.coolclient.core.setting.NumberSetting;

/**
 * Recolours YOUR OWN first-person hand and held item. It never affects other players or how
 * anything else renders; it is purely a look for your viewmodel.
 */
public final class HandChams extends Module {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Tint", "Tint", "Flat", "Glow", "Wireframe"));
    public final ModeSetting target = add(new ModeSetting("Apply To", "Both", "Both", "Arm", "Item"));
    public final ColorSetting armColor = add(new ColorSetting("Arm Color", 0xB32DE2C1));
    public final ColorSetting itemColor = add(new ColorSetting("Item Color", 0xB38A5CFF));
    public final NumberSetting lineWidth = add(new NumberSetting("Wireframe Width", 1.5, 0.5, 4, 0.5)
            .visibleWhen(() -> mode.is("Wireframe")));

    public HandChams() {
        super("Hand Chams", "Custom colours and effects for your own first-person hand and item.", Category.VISUAL);
    }

    public boolean arm() { return isEnabled() && !target.is("Item"); }

    public boolean item() { return isEnabled() && !target.is("Arm"); }

    public boolean textured() { return mode.is("Tint") || mode.is("Wireframe"); }

    public boolean glow() { return mode.is("Glow"); }

    public boolean wireframe() { return mode.is("Wireframe"); }
}
