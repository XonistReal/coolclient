package dev.coolclient.core.modules.visual;

import dev.coolclient.core.api.Feature;
import dev.coolclient.core.module.Category;
import dev.coolclient.core.module.Module;
import dev.coolclient.core.setting.BoolSetting;
import dev.coolclient.core.setting.ColorSetting;
import dev.coolclient.core.setting.ModeSetting;
import dev.coolclient.core.setting.NumberSetting;

/** Small settings-only visual modules read by the version mixins. */
public final class VisualTweaks {
    private VisualTweaks() {}

    public static final class HitColor extends Module {
        public final ColorSetting color = add(new ColorSetting("Hit Color", 0x66FF0000));

        public HitColor() {
            super("Hit Color", "Change the red flash when an entity is hurt.", Category.COMBAT);
        }
    }

    public static final class HurtCam extends Module {
        public final NumberSetting strength = add(new NumberSetting("Strength", 30, 0, 100, 5).suffix("%"));

        public HurtCam() {
            super("Hurt Cam", "Reduce or remove the camera shake when you take damage.", Category.COMBAT);
        }

        public float factor() { return isEnabled() ? strength.f() / 100f : 1f; }
    }

    public static final class Hitboxes extends Module {
        public final ColorSetting color = add(new ColorSetting("Box Color", 0xFFFFFFFF));
        public final NumberSetting lineWidth = add(new NumberSetting("Line Width", 1.5, 0.5, 5, 0.5));
        public final BoolSetting showLook = add(new BoolSetting("Show Look Vector", false));
        public final BoolSetting showEyes = add(new BoolSetting("Show Eye Height", true));
        public final BoolSetting playersOnly = add(new BoolSetting("Players Only", false));

        public Hitboxes() {
            super("Hitboxes", "Style the F3+B hitboxes.", Category.COMBAT);
        }
    }

    public static final class Particles extends Module {
        public final NumberSetting multiplier = add(new NumberSetting("Multiplier", 2, 1, 10, 1).suffix("x"));
        public final BoolSetting alwaysCrits = add(new BoolSetting("Always Show Crits", false));
        public final BoolSetting alwaysSharpness = add(new BoolSetting("Always Show Sharpness", true));

        public Particles() {
            super("Particles", "More hit particles, every hit.", Category.COMBAT);
        }
    }

    public static final class Nametags extends Module {
        public final BoolSetting showOwn = add(new BoolSetting("Show Own Nametag", true));
        public final NumberSetting background = add(new NumberSetting("Background Opacity", 25, 0, 100, 5).suffix("%"));
        public final BoolSetting shadow = add(new BoolSetting("Text Shadow", true));

        public Nametags() {
            super("Nametags", "Nametag tweaks, including your own in third person.", Category.VISUAL);
            enabledByDefault();
        }
    }

    public static final class ItemPhysics extends Module {
        public final NumberSetting spin = add(new NumberSetting("Spin While Falling", 1, 0, 3, 0.25).suffix("x"));

        public ItemPhysics() {
            super("Item Physics", "Dropped items lie flat on the ground.", Category.VISUAL);
        }
    }

    public static final class LowFire extends Module {
        public final NumberSetting height = add(new NumberSetting("Lower By", 0.3, 0, 0.5, 0.05));
        public final NumberSetting opacity = add(new NumberSetting("Opacity", 70, 10, 100, 5).suffix("%"));

        public LowFire() {
            super("Low Fire", "Shrinks the on-fire overlay so you can see.", Category.VISUAL);
        }
    }

    public static final class MotionBlur extends Module {
        public final NumberSetting strength = add(new NumberSetting("Strength", 4, 1, 9, 1));

        public MotionBlur() {
            super("Motion Blur", "Smooth frame blending.", Category.VISUAL, Feature.MOTION_BLUR);
        }

        public float amount() { return 0.2f + strength.f() * 0.08f; }
    }

    public static final class Fullbright extends Module {
        public Fullbright() {
            super("Fullbright", "See in the dark.", Category.VISUAL);
        }
    }

    public static final class TimeChanger extends Module {
        public final NumberSetting time = add(new NumberSetting("Time", 6000, 0, 24000, 250).describe("6000 = noon, 18000 = midnight"));

        public TimeChanger() {
            super("Time Changer", "Client-side time of day.", Category.VISUAL);
        }
    }

    public static final class WeatherChanger extends Module {
        public WeatherChanger() {
            super("Clear Weather", "Hides rain and snow (client-side).", Category.VISUAL);
        }
    }

    public static final class FovChanger extends Module {
        public final BoolSetting noSpeedFov = add(new BoolSetting("Static Speed FOV", true).describe("Speed/slowness don't change FOV"));
        public final BoolSetting noBowFov = add(new BoolSetting("Static Bow FOV", false));

        public FovChanger() {
            super("FOV Changer", "Stop FOV from changing with speed, sprinting or bows.", Category.VISUAL);
        }
    }

    public static final class Bobbing extends Module {
        public final ModeSetting mode = add(new ModeSetting("Mode", "Hand Only", "Hand Only", "Camera Only"));

        public Bobbing() {
            super("Bobbing", "Keep walking bob on your hand but not the camera (or the reverse).", Category.VISUAL);
        }
    }
}
