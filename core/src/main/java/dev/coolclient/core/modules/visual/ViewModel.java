package dev.coolclient.core.modules.visual;

import dev.coolclient.core.module.Category;
import dev.coolclient.core.module.Module;
import dev.coolclient.core.setting.BoolSetting;
import dev.coolclient.core.setting.NumberSetting;

/** First-person item position, rotation, scale and swing animation speed (visual only). */
public final class ViewModel extends Module {
    public final NumberSetting x = add(new NumberSetting("Offset X", 0, -1, 1, 0.02));
    public final NumberSetting y = add(new NumberSetting("Offset Y", 0, -1, 1, 0.02));
    public final NumberSetting z = add(new NumberSetting("Offset Z", 0, -1, 1, 0.02));
    public final NumberSetting scale = add(new NumberSetting("Scale", 1, 0.3, 1.5, 0.05));
    public final NumberSetting rotX = add(new NumberSetting("Rotation X", 0, -180, 180, 1));
    public final NumberSetting rotY = add(new NumberSetting("Rotation Y", 0, -180, 180, 1));
    public final NumberSetting rotZ = add(new NumberSetting("Rotation Z", 0, -180, 180, 1));
    public final NumberSetting swingSpeed = add(new NumberSetting("Swing Speed", 1, 0.25, 2, 0.05)
            .describe("Only changes the animation, never your attack timing"));
    public final BoolSetting noEquipAnimation = add(new BoolSetting("No Re-equip Animation", false));

    public ViewModel() {
        super("View Model", "Move, rotate and scale your first-person hand.", Category.VISUAL);
    }

    public boolean hasTransform() {
        return isEnabled() && (x.d() != 0 || y.d() != 0 || z.d() != 0 || scale.d() != 1
                || rotX.d() != 0 || rotY.d() != 0 || rotZ.d() != 0);
    }
}
