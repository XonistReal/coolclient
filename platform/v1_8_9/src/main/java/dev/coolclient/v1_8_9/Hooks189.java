package dev.coolclient.v1_8_9;

import com.mojang.blaze3d.platform.GlStateManager;
import dev.coolclient.core.CoolClient;
import dev.coolclient.core.gui.screen.MainMenuScreen;
import dev.coolclient.core.modules.legacy.LegacyModules;
import dev.coolclient.core.modules.visual.VisualTweaks;
import dev.coolclient.core.modules.visual.Zoom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.particle.ParticleType;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;

/** Static glue called from the 1.8.9 mixins. */
public final class Hooks189 {
    private static boolean cinematicApplied;
    private static boolean savedSmooth;
    public static boolean renderingHand;

    private Hooks189() {}

    public static boolean ready() { return CoolClient.get() != null; }

    public static void tick() {
        if (!ready()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        CoolClient.get().onTick();

        VisualTweaks.TimeChanger tc = CoolClient.get().modules().get(VisualTweaks.TimeChanger.class);
        if (mc.world != null && tc != null && tc.isEnabled()) mc.world.setTimeOfDay((long) tc.time.d());

        Zoom zoom = CoolClient.get().modules().get(Zoom.class);
        boolean cine = zoom != null && zoom.cinematicActive();
        if (cine && !cinematicApplied) {
            savedSmooth = mc.options.smoothCameraEnabled;
            mc.options.smoothCameraEnabled = true;
            cinematicApplied = true;
        } else if (!cine && cinematicApplied) {
            mc.options.smoothCameraEnabled = savedSmooth;
            cinematicApplied = false;
        }

        // 1.7 block-hitting: visually swing while holding attack during item use (no packet sent).
        LegacyModules.OldAnimations oa = CoolClient.get().modules().get(LegacyModules.OldAnimations.class);
        if (oa != null && oa.isEnabled() && oa.punchWhileUsing.on() && mc.player != null && mc.currentScreen == null
                && mc.player.isUsingItem() && mc.options.attackKey.isPressed()
                && mc.result != null && mc.result.type == BlockHitResult.Type.BLOCK) {
            if (!mc.player.handSwinging || mc.player.handSwingTicks >= 3 || mc.player.handSwingTicks < 0) {
                mc.player.handSwingTicks = -1;
                mc.player.handSwinging = true;
            }
        }
    }

    public static void renderHud(float tickDelta) {
        if (!ready()) return;
        GlStateManager.pushMatrix();
        try {
            CoolClient.get().onRenderHud(Platform189.RENDERER.frame(), tickDelta);
        } finally {
            GlStateManager.popMatrix();
            GlStateManager.color(1f, 1f, 1f, 1f);
            GlStateManager.enableAlphaTest();
        }
    }

    public static Screen replaceScreen(Screen s, ClientWorld world) {
        if (!ready() || !CoolClient.get().client().customMainMenu.on()) return s;
        if (s instanceof TitleScreen || (s == null && world == null)) return new CoolScreen(new MainMenuScreen());
        return s;
    }

    public static void attack(PlayerEntity player, Entity target) {
        if (!ready() || target == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        double ex = player.x, ey = player.y + player.getEyeHeight(), ez = player.z;
        double dist;
        if (mc.result != null && mc.result.entity == target && mc.result.pos != null) {
            double dx = mc.result.pos.x - ex, dy = mc.result.pos.y - ey, dz = mc.result.pos.z - ez;
            dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        } else {
            dist = player.distanceTo(target);
        }
        CoolClient.get().onAttack(target.getEntityId(), target.getTranslationKey(), target instanceof PlayerEntity, dist);

        VisualTweaks.Particles p = CoolClient.get().modules().get(VisualTweaks.Particles.class);
        if (p != null && p.isEnabled() && target instanceof LivingEntity) {
            int n = p.multiplier.i();
            for (int i = 0; i < n; i++) {
                if (p.alwaysCrits.on()) mc.particleManager.addEmitter(target, ParticleType.CRIT);
                if (p.alwaysSharpness.on()) mc.particleManager.addEmitter(target, ParticleType.CRIT_MAGIC);
            }
        }
    }
}
