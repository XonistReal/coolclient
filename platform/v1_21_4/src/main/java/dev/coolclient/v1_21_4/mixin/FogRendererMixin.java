package dev.coolclient.v1_21_4.mixin;

import dev.coolclient.core.render.gpu.SkyRenderer;
import dev.coolclient.v1_21_4.Hooks21;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Skybox "Match Fog": distant terrain and the clear colour fade into the custom sky's horizon. */
@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
    @Inject(method = "computeFogColor", at = @At("RETURN"))
    private static void cool$skyFog(Camera camera, float partialTick, ClientLevel level, int renderDistance,
                                    float darken, CallbackInfoReturnable<Vector4f> cir) {
        if (!Hooks21.ready() || camera.getFluidInCamera() != FogType.NONE) return;
        if (level.effects().skyType() != DimensionSpecialEffects.SkyType.OVERWORLD) return;
        if (camera.getEntity() instanceof LivingEntity e
                && (e.hasEffect(MobEffects.BLINDNESS) || e.hasEffect(MobEffects.DARKNESS))) return;
        int c = SkyRenderer.fogColor();
        if (c == 0) return;
        Vector4f v = cir.getReturnValue();
        v.set(((c >> 16) & 0xFF) / 255f, ((c >> 8) & 0xFF) / 255f, (c & 0xFF) / 255f, v.w);
    }

    /** Fog module: distance fog pushed back or removed, clearer water. Lava, powder snow and blindness/darkness stay vanilla. */
    @Inject(method = "setupFog", at = @At("RETURN"), cancellable = true)
    private static void cool$fog(Camera camera, FogRenderer.FogMode mode, Vector4f color, float renderDistance,
                                 boolean thick, float partialTick, CallbackInfoReturnable<net.minecraft.client.renderer.FogParameters> cir) {
        if (!Hooks21.ready()) return;
        if (camera.getEntity() instanceof LivingEntity e
                && (e.hasEffect(MobEffects.BLINDNESS) || e.hasEffect(MobEffects.DARKNESS))) return;
        net.minecraft.client.renderer.FogParameters p = cir.getReturnValue();
        if (p == net.minecraft.client.renderer.FogParameters.NO_FOG) return;
        FogType fluid = camera.getFluidInCamera();
        if (fluid == FogType.NONE && mode == FogRenderer.FogMode.FOG_TERRAIN) {
            float m = dev.coolclient.core.modules.visual.RenderTweaks.Fog.terrain();
            if (m == 1f) return;
            if (Float.isInfinite(m)) {
                cir.setReturnValue(net.minecraft.client.renderer.FogParameters.NO_FOG);
                return;
            }
            cir.setReturnValue(new net.minecraft.client.renderer.FogParameters(p.start() * m, p.end() * m, p.shape(), p.red(), p.green(), p.blue(), p.alpha()));
        } else if (fluid == FogType.WATER) {
            float m = dev.coolclient.core.modules.visual.RenderTweaks.Fog.water();
            if (m == 1f) return;
            float end = Math.min(renderDistance, p.end() * m);
            cir.setReturnValue(new net.minecraft.client.renderer.FogParameters(p.start(), end, p.shape(), p.red(), p.green(), p.blue(), p.alpha()));
        }
    }
}
