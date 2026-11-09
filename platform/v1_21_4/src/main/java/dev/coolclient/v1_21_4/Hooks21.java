package dev.coolclient.v1_21_4;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.gui.screen.MainMenuScreen;
import dev.coolclient.core.modules.visual.HandChams;
import dev.coolclient.core.modules.visual.VisualTweaks;
import dev.coolclient.core.modules.visual.Zoom;
import dev.coolclient.v1_21_4.mixin.OverlayTextureAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** Static glue called from the 1.21.4 mixins. */
public final class Hooks21 {
    private static boolean cinematicApplied;
    private static boolean savedSmooth;
    private static int appliedHitColor = 0xB2FF0000;
    public static boolean renderingHand;

    private Hooks21() {}

    public static boolean ready() { return CoolClient.get() != null; }

    public static void tick() {
        if (!ready()) return;
        Minecraft mc = Minecraft.getInstance();
        CoolClient.get().onTick();

        Zoom zoom = CoolClient.get().modules().get(Zoom.class);
        boolean cine = zoom != null && zoom.cinematicActive();
        if (cine && !cinematicApplied) {
            savedSmooth = mc.options.smoothCamera;
            mc.options.smoothCamera = true;
            cinematicApplied = true;
        } else if (!cine && cinematicApplied) {
            mc.options.smoothCamera = savedSmooth;
            cinematicApplied = false;
        }
        updateHitColor(mc);
    }

    /** Repaints the red rows of the entity overlay texture when Hit Color changes. */
    private static void updateHitColor(Minecraft mc) {
        VisualTweaks.HitColor h = CoolClient.get().modules().get(VisualTweaks.HitColor.class);
        int target = 0xB2FF0000;
        if (h != null && h.isEnabled()) {
            int c = h.color.get();
            // Vanilla's overlay alpha is how much of the ORIGINAL colour shows, so invert it.
            int a = 255 - ((c >>> 24) & 0xFF);
            target = (a << 24) | (c & 0xFFFFFF);
        }
        if (target == appliedHitColor) return;
        DynamicTexture tex = ((OverlayTextureAccessor) mc.gameRenderer.overlayTexture()).cool$texture();
        if (tex == null || tex.getPixels() == null) return;
        for (int y = 0; y < 8; y++)
            for (int x = 0; x < 16; x++) tex.getPixels().setPixel(x, y, target);
        tex.upload();
        appliedHitColor = target;
    }

    public static void renderHud(GuiGraphics g, float tickDelta) {
        if (!ready()) return;
        CoolClient.get().onRenderHud(Platform21.RENDERER.frame(g), tickDelta);
        g.flush();
    }

    /** Shader colour to restore after a recoloured glint (see RenderStateShardMixin). */
    public static float[] savedShaderColor;

    /** The world is drawn and the HUD is next: motion blur, then the PostWorld effects (colour grading). */
    public static void postWorld(GuiGraphics g) {
        if (!ready() || Minecraft.getInstance().level == null) return;
        dev.coolclient.core.api.GuiRenderer r = Platform21.RENDERER.frame(g);
        dev.coolclient.core.modules.visual.RenderTweaks.motionBlur(r);
        CoolClient.get().events().post(new dev.coolclient.core.event.Events.PostWorld(r));
        g.flush();
    }

    public static Screen replaceScreen(Screen s, ClientLevel level) {
        if (!ready() || !CoolClient.get().client().customMainMenu.on()) return s;
        if (s instanceof TitleScreen || (s == null && level == null)) return new CoolScreen(new MainMenuScreen());
        return s;
    }

    public static void attack(Player player, Entity target) {
        if (!ready() || target == null) return;
        Minecraft mc = Minecraft.getInstance();
        Vec3 eye = player.getEyePosition();
        double dist;
        if (mc.hitResult instanceof EntityHitResult && ((EntityHitResult) mc.hitResult).getEntity() == target) {
            dist = eye.distanceTo(mc.hitResult.getLocation());
        } else {
            dist = Math.sqrt(target.getBoundingBox().distanceToSqr(eye));
        }
        CoolClient.get().onAttack(target.getId(), target.getName().getString(), target instanceof Player, dist);
        VisualTweaks.Particles p = CoolClient.get().modules().get(VisualTweaks.Particles.class);
        if (p != null && p.isEnabled() && target instanceof LivingEntity) {
            for (int i = 0; i < p.multiplier.i(); i++) {
                if (p.alwaysCrits.on()) mc.particleEngine.createTrackingEmitter(target, ParticleTypes.CRIT);
                if (p.alwaysSharpness.on()) mc.particleEngine.createTrackingEmitter(target, ParticleTypes.ENCHANTED_HIT);
            }
        }
    }

    /** Wraps the buffers used for the local first-person arm or item with Hand Chams. */
    public static MultiBufferSource chams(MultiBufferSource buffers, boolean arm) {
        if (!ready()) return buffers;
        HandChams c = CoolClient.get().modules().get(HandChams.class);
        if (c == null || !c.isEnabled()) return buffers;
        if (arm ? !c.arm() : !c.item()) return buffers;
        return new ChamsBuffers(buffers, c, arm ? c.armColor.color() : c.itemColor.color());
    }
}
