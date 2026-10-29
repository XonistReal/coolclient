package dev.coolclient.core.modules.legacy;

import dev.coolclient.core.api.Feature;
import dev.coolclient.core.module.Category;
import dev.coolclient.core.module.Module;
import dev.coolclient.core.setting.BoolSetting;

/** 1.8.9-only modules. */
public final class LegacyModules {
    private LegacyModules() {}

    public static final class OldAnimations extends Module {
        public final BoolSetting blockHit = add(new BoolSetting("Block Hitting", true));
        public final BoolSetting rod = add(new BoolSetting("Fishing Rod", true));
        public final BoolSetting bow = add(new BoolSetting("Bow", true));
        public final BoolSetting eating = add(new BoolSetting("Eating & Drinking", true));
        public final BoolSetting swordBlock = add(new BoolSetting("Sword Blocking", true));
        public final BoolSetting armorDamage = add(new BoolSetting("Red Armor On Hit", true));
        public final BoolSetting sneak = add(new BoolSetting("Instant Sneak", false));
        public final BoolSetting punchWhileUsing = add(new BoolSetting("Swing While Using", true));

        public OldAnimations() {
            super("1.7 Animations", "Classic 1.7 item animations.", Category.COMBAT, Feature.OLD_ANIMATIONS);
            versionTag("1.8");
            enabledByDefault();
        }
    }

    public static final class MouseDelayFix extends Module {
        public MouseDelayFix() {
            super("Mouse Delay Fix", "Fixes 1.8's delayed look direction when attacking.", Category.COMBAT, Feature.MOUSE_DELAY_FIX);
            versionTag("1.8");
            enabledByDefault();
        }
    }

    public static final class Borderless extends Module {
        public Borderless() {
            super("Borderless Window", "Borderless fullscreen; alt-tab without minimising.", Category.UTILITY, Feature.BORDERLESS);
            versionTag("1.8");
        }

        @Override
        protected void onEnable() { platform().setBorderless(true); }

        @Override
        protected void onDisable() { platform().setBorderless(false); }
    }

    public static final class TextureAnimations extends Module {
        public TextureAnimations() {
            super("No Texture Animations", "Pause animated textures (water, lava, fire) for FPS.", Category.PERFORMANCE, Feature.TEXTURE_ANIMATIONS);
        }

        @Override
        protected void onEnable() { platform().setTextureAnimations(false); }

        @Override
        protected void onDisable() { platform().setTextureAnimations(true); }
    }
}
