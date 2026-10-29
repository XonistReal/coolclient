package dev.coolclient.core.modules.perf;

import dev.coolclient.core.module.Category;
import dev.coolclient.core.module.Module;
import dev.coolclient.core.setting.BoolSetting;
import dev.coolclient.core.setting.NumberSetting;

public final class PerformanceModules {
    private PerformanceModules() {}

    public static final class EntityCulling extends Module {
        public final NumberSetting entityDistance = add(new NumberSetting("Entity Distance", 64, 16, 128, 4).suffix(" blocks"));
        public final NumberSetting tileDistance = add(new NumberSetting("Block Entity Distance", 48, 16, 128, 4).suffix(" blocks"));
        public final BoolSetting alwaysPlayers = add(new BoolSetting("Always Render Players", true));

        public EntityCulling() {
            super("Render Distance", "Skip rendering far-away entities and block entities.", Category.PERFORMANCE);
        }

        /** Squared-distance check used by the render mixins. */
        public boolean shouldSkip(double distSq, boolean isPlayer) {
            if (!isEnabled() || (isPlayer && alwaysPlayers.on())) return false;
            double d = entityDistance.d();
            return distSq > d * d;
        }

        public boolean shouldSkipTile(double distSq) {
            if (!isEnabled()) return false;
            double d = tileDistance.d();
            return distSq > d * d;
        }
    }

    public static final class ParticleLimit extends Module {
        public final NumberSetting max = add(new NumberSetting("Max Particles", 4000, 500, 16000, 500));
        public final BoolSetting hideExplosions = add(new BoolSetting("Hide Explosion Particles", false));

        public ParticleLimit() {
            super("Particle Limit", "Cap the number of particles for steadier FPS.", Category.PERFORMANCE);
        }
    }

    public static final class FpsLimiter extends Module {
        public final NumberSetting unfocused = add(new NumberSetting("Unfocused FPS", 15, 5, 60, 5));
        public final NumberSetting menu = add(new NumberSetting("Menu FPS", 60, 30, 240, 10));

        public FpsLimiter() {
            super("Smart FPS", "Lower FPS when the game is in the background or in menus.", Category.PERFORMANCE);
            enabledByDefault();
        }
    }
}
