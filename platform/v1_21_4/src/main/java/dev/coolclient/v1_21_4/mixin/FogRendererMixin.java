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
}
