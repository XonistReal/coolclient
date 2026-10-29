package dev.coolclient.v1_8_9.mixin;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.modules.visual.Freelook;
import dev.coolclient.core.modules.visual.VisualTweaks;
import dev.coolclient.core.modules.visual.Zoom;
import dev.coolclient.v1_8_9.Hooks189;
import dev.coolclient.v1_8_9.MotionBlur189;
import dev.coolclient.v1_8_9.World3D189;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.Entity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void cool$frame(float tickDelta, long nanoTime, CallbackInfo ci) {
        if (Hooks189.ready()) CoolClient.get().onFrame();
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;render(F)V"))
    private void cool$motionBlur(float tickDelta, long nanoTime, CallbackInfo ci) {
        if (Hooks189.ready()) MotionBlur189.render(tickDelta);
    }

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void cool$zoom(float tickDelta, boolean useFovSetting, CallbackInfoReturnable<Float> cir) {
        if (!useFovSetting || !Hooks189.ready()) return;
        Zoom z = CoolClient.get().modules().get(Zoom.class);
        if (z != null) {
            float m = z.fovMultiplier();
            if (m != 1f) cir.setReturnValue(cir.getReturnValue() * m);
        }
    }

    @ModifyConstant(method = "bobViewWhenHurt", constant = @Constant(floatValue = 14.0f))
    private float cool$hurtCam(float strength) {
        if (!Hooks189.ready()) return strength;
        VisualTweaks.HurtCam h = CoolClient.get().modules().get(VisualTweaks.HurtCam.class);
        return h == null ? strength : strength * h.factor();
    }

    @Inject(method = "renderHand", at = @At("HEAD"))
    private void cool$handStart(float tickDelta, int pass, CallbackInfo ci) {
        Hooks189.renderingHand = true;
    }

    @Inject(method = "renderHand", at = @At("RETURN"))
    private void cool$handEnd(float tickDelta, int pass, CallbackInfo ci) {
        Hooks189.renderingHand = false;
    }

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void cool$bob(float tickDelta, CallbackInfo ci) {
        if (!Hooks189.ready()) return;
        VisualTweaks.Bobbing b = CoolClient.get().modules().get(VisualTweaks.Bobbing.class);
        if (b == null || !b.isEnabled()) return;
        if (b.mode.is("Hand Only") && !Hooks189.renderingHand) ci.cancel();
        else if (b.mode.is("Camera Only") && Hooks189.renderingHand) ci.cancel();
    }

    @Inject(method = "renderWorld(IFJ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;renderHand(FI)V"))
    private void cool$world(int pass, float tickDelta, long limitTime, CallbackInfo ci) {
        if (Hooks189.ready()) World3D189.waypoints(tickDelta);
    }

    @Redirect(method = "updateLightmap", at = @At(value = "FIELD", target = "Lnet/minecraft/client/option/GameOptions;gamma:F", opcode = Opcodes.GETFIELD))
    private float cool$gamma(GameOptions options) {
        if (Hooks189.ready() && CoolClient.get().modules().active(VisualTweaks.Fullbright.class)) return 15f;
        return options.gamma;
    }

    // ---------------------------------------------------------------- freelook camera

    @Unique
    private static Freelook cool$freelook() {
        if (!Hooks189.ready()) return null;
        Freelook f = CoolClient.get().modules().get(Freelook.class);
        return f != null && f.active() ? f : null;
    }

    @Redirect(method = "transformCamera", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;yaw:F", opcode = Opcodes.GETFIELD))
    private float cool$yaw(Entity e) {
        Freelook f = cool$freelook();
        return f != null ? f.yaw() : e.yaw;
    }

    @Redirect(method = "transformCamera", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;prevYaw:F", opcode = Opcodes.GETFIELD))
    private float cool$prevYaw(Entity e) {
        Freelook f = cool$freelook();
        return f != null ? f.yaw() : e.prevYaw;
    }

    @Redirect(method = "transformCamera", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;pitch:F", opcode = Opcodes.GETFIELD))
    private float cool$pitch(Entity e) {
        Freelook f = cool$freelook();
        return f != null ? f.pitch() : e.pitch;
    }

    @Redirect(method = "transformCamera", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;prevPitch:F", opcode = Opcodes.GETFIELD))
    private float cool$prevPitch(Entity e) {
        Freelook f = cool$freelook();
        return f != null ? f.pitch() : e.prevPitch;
    }
}
