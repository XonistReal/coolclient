package dev.coolclient.v1_21_4.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.coolclient.core.render.gpu.SkyRenderer;
import dev.coolclient.v1_21_4.Gpu21;
import dev.coolclient.v1_21_4.Hooks21;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Skybox: the sky disc is the first overworld sky draw; replacing it also hides sun, moon and stars. */
@Mixin(net.minecraft.client.renderer.SkyRenderer.class)
public abstract class SkyRendererMixin {
    @Unique private static final float[] cool$projection = new float[16];
    @Unique private static final float[] cool$modelView = new float[16];

    @Inject(method = "renderSkyDisc", at = @At("HEAD"), cancellable = true)
    private void cool$sky(float r, float g, float b, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (!Hooks21.ready() || mc.level == null) return;
        RenderSystem.getProjectionMatrix().get(cool$projection);
        RenderSystem.getModelViewMatrix().get(cool$modelView);
        if (SkyRenderer.render(Gpu21.INSTANCE.world(), cool$projection, cool$modelView, mc.level.getDayTime())) ci.cancel();
    }

    @Inject(method = "renderSunMoonAndStars", at = @At("HEAD"), cancellable = true)
    private void cool$sunMoon(PoseStack pose, MultiBufferSource.BufferSource buffers, float timeOfDay, int moonPhase,
                              float rain, float stars, FogParameters fog, CallbackInfo ci) {
        if (Hooks21.ready() && SkyRenderer.replacing()) ci.cancel();
    }

    @Inject(method = "renderSunriseAndSunset", at = @At("HEAD"), cancellable = true)
    private void cool$sunrise(PoseStack pose, MultiBufferSource.BufferSource buffers, float angle, int color, CallbackInfo ci) {
        if (Hooks21.ready() && SkyRenderer.replacing()) ci.cancel();
    }

    @Inject(method = "renderDarkDisc", at = @At("HEAD"), cancellable = true)
    private void cool$darkDisc(PoseStack pose, CallbackInfo ci) {
        if (Hooks21.ready() && SkyRenderer.replacing()) ci.cancel();
    }
}
