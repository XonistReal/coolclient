package dev.coolclient.v1_21_4.mixin;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.modules.cpvp.CrystalPvpTweaks;
import dev.coolclient.core.modules.visual.VisualTweaks;
import dev.coolclient.core.modules.visual.Zoom;
import dev.coolclient.v1_21_4.Hooks21;
import dev.coolclient.v1_21_4.World3D21;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow @Final private Camera mainCamera;
    @Unique private boolean cool$totemScaled;

    @Inject(method = "render", at = @At("HEAD"))
    private void cool$frame(DeltaTracker delta, boolean renderLevel, CallbackInfo ci) {
        if (Hooks21.ready()) CoolClient.get().onFrame();
    }

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void cool$zoom(Camera camera, float tickDelta, boolean useFovSetting, CallbackInfoReturnable<Float> cir) {
        if (!useFovSetting || !Hooks21.ready()) return;
        Zoom z = CoolClient.get().modules().get(Zoom.class);
        if (z != null) {
            float m = z.fovMultiplier();
            if (m != 1f) cir.setReturnValue(cir.getReturnValueF() * m);
        }
    }

    @ModifyConstant(method = "bobHurt", constant = @Constant(doubleValue = 14.0))
    private double cool$hurtCam(double strength) {
        if (!Hooks21.ready()) return strength;
        VisualTweaks.HurtCam h = CoolClient.get().modules().get(VisualTweaks.HurtCam.class);
        return h == null ? strength : strength * h.factor();
    }

    @Inject(method = "renderItemInHand", at = @At("HEAD"))
    private void cool$handStart(Camera camera, float tickDelta, Matrix4f matrix, CallbackInfo ci) {
        Hooks21.renderingHand = true;
    }

    @Inject(method = "renderItemInHand", at = @At("RETURN"))
    private void cool$handEnd(Camera camera, float tickDelta, Matrix4f matrix, CallbackInfo ci) {
        Hooks21.renderingHand = false;
    }

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void cool$bob(PoseStack pose, float tickDelta, CallbackInfo ci) {
        if (!Hooks21.ready()) return;
        VisualTweaks.Bobbing b = CoolClient.get().modules().get(VisualTweaks.Bobbing.class);
        if (b == null || !b.isEnabled()) return;
        if (b.mode.is("Hand Only") && !Hooks21.renderingHand) ci.cancel();
        else if (b.mode.is("Camera Only") && Hooks21.renderingHand) ci.cancel();
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;renderItemInHand(Lnet/minecraft/client/Camera;FLorg/joml/Matrix4f;)V"))
    private void cool$waypoints(DeltaTracker delta, CallbackInfo ci) {
        if (Hooks21.ready()) World3D21.waypoints(mainCamera);
    }

    // ---------------------------------------------------------------- totem pop overlay

    @Inject(method = "renderItemActivationAnimation", at = @At("HEAD"), cancellable = true)
    private void cool$totemStart(GuiGraphics g, float tickDelta, CallbackInfo ci) {
        cool$totemScaled = false;
        if (!Hooks21.ready()) return;
        CrystalPvpTweaks.TotemPopAnimation t = CoolClient.get().modules().get(CrystalPvpTweaks.TotemPopAnimation.class);
        if (t == null || !t.isEnabled()) return;
        if (t.hideOverlay.on()) {
            ci.cancel();
            return;
        }
        float s = t.scale.f(), cx = g.guiWidth() / 2f, cy = g.guiHeight() / 2f;
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0f);
        g.pose().scale(s, s, 1f);
        g.pose().translate(-cx, -cy, 0f);
        cool$totemScaled = true;
    }

    @Inject(method = "renderItemActivationAnimation", at = @At("RETURN"))
    private void cool$totemEnd(GuiGraphics g, float tickDelta, CallbackInfo ci) {
        if (cool$totemScaled) {
            g.pose().popPose();
            cool$totemScaled = false;
        }
    }
}
