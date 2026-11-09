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

    /** Glint colour of the vanilla 1.8 enchantment glint (-8372020 = 0xFF8040CC). */
    private static final int VANILLA_GLINT = 0xFF8040CC;

    /**
     * The 1.8 glint tint for Glint's colour. The setting is in "1.21 terms" (its default 0xA755FF is the
     * brightest 1.21 glint texel), so the same colour gives the same look on both versions.
     */
    public static int glintColor(int vanilla) {
        float[] c = dev.coolclient.core.modules.visual.RenderTweaks.Glint.rgb();
        if (c == null) return vanilla;
        int r = Math.min(255, Math.round(c[0] / (167 / 255f) * 0x80));
        int g = Math.min(255, Math.round(c[1] / (85 / 255f) * 0x40));
        int b = Math.min(255, Math.round(c[2] * 0xCC));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /** {r, g, b} multipliers for the armor glint colour call, or null for vanilla. */
    public static float[] glintFactors() {
        float[] c = dev.coolclient.core.modules.visual.RenderTweaks.Glint.rgb();
        if (c == null) return null;
        int t = glintColor(VANILLA_GLINT);
        return new float[] {((t >> 16) & 0xFF) / (float) 0x80, ((t >> 8) & 0xFF) / (float) 0x40, (t & 0xFF) / (float) 0xCC};
    }

    private static long lastServerTime = Long.MIN_VALUE;

    /** World-time packet (seen on the network thread and again on the game thread): one ServerTime event each. */
    public static void serverTime(final long gameTime) {
        if (!ready() || gameTime == lastServerTime) return;
        lastServerTime = gameTime;
        dev.coolclient.core.util.Scheduler.main(() ->
                CoolClient.get().events().post(new dev.coolclient.core.event.Events.ServerTime(gameTime)));
    }

    /** The world is drawn and the HUD is next: colour grading and other PostWorld effects. */
    public static void postWorld() {
        if (!ready() || MinecraftClient.getInstance().world == null) return;
        MinecraftClient.getInstance().gameRenderer.setupHudMatrixMode();
        GlStateManager.pushMatrix();
        try {
            CoolClient.get().events().post(new dev.coolclient.core.event.Events.PostWorld(Platform189.RENDERER.frame()));
        } finally {
            GlStateManager.popMatrix();
            GlStateManager.color(1f, 1f, 1f, 1f);
            GlStateManager.enableAlphaTest();
        }
    }

    /** Menu Blur behind a vanilla screen; false if nothing was drawn. */
    public static boolean menuBlur(boolean container) {
        if (!ready()) return false;
        boolean done = dev.coolclient.core.modules.visual.RenderTweaks.MenuBlur.draw(Platform189.RENDERER.frame(), container);
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.enableAlphaTest();
        return done;
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
