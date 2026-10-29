package dev.coolclient.core.modules.cpvp;

import dev.coolclient.core.api.Feature;
import dev.coolclient.core.module.Category;
import dev.coolclient.core.module.Module;
import dev.coolclient.core.setting.BoolSetting;
import dev.coolclient.core.setting.NumberSetting;

/** Visual-only crystal PvP tweaks. None of these change gameplay. */
public final class CrystalPvpTweaks {
    private CrystalPvpTweaks() {}

    public static final class TotemPopAnimation extends Module {
        public final BoolSetting hideOverlay = add(new BoolSetting("Hide Overlay", false));
        public final NumberSetting scale = add(new NumberSetting("Scale", 0.6, 0.1, 1.5, 0.05).visibleWhen(() -> !hideOverlay.on()));
        public final BoolSetting hideParticles = add(new BoolSetting("Hide Pop Particles", false));

        public TotemPopAnimation() {
            super("Totem Animation", "Shrink or hide the big totem pop overlay.", Category.COMBAT, Feature.TOTEMS);
            versionTag("1.21");
        }
    }

    public static final class ShieldTweaks extends Module {
        public final NumberSetting lower = add(new NumberSetting("Lower By", 0.2, 0, 0.6, 0.02));
        public final NumberSetting scale = add(new NumberSetting("Scale", 0.85, 0.4, 1, 0.05));
        public final BoolSetting onlyIdle = add(new BoolSetting("Only When Not Blocking", true));

        public ShieldTweaks() {
            super("Low Shield", "Lower and shrink your shield so it blocks less of your view.", Category.COMBAT, Feature.SHIELDS);
            versionTag("1.21");
        }
    }

    public static final class CrystalTweaks extends Module {
        public final NumberSetting scale = add(new NumberSetting("Scale", 1, 0.3, 1.5, 0.05));
        public final NumberSetting spin = add(new NumberSetting("Spin Speed", 1, 0, 3, 0.1).suffix("x"));
        public final NumberSetting bounce = add(new NumberSetting("Bounce", 1, 0, 2, 0.1).suffix("x"));

        public CrystalTweaks() {
            super("Crystal Tweaks", "Resize and calm down end crystals (visual only).", Category.COMBAT, Feature.END_CRYSTALS);
            versionTag("1.21");
        }
    }
}
