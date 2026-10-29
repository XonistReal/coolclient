package dev.coolclient.v1_8_9.mixin;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.modules.legacy.LegacyModules;
import dev.coolclient.core.modules.visual.ViewModel;
import dev.coolclient.v1_8_9.Hooks189;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    /**
     * Mouse Delay Fix: 1.8 computes the local player's look vector from the (lagging) head yaw.
     * Use the interpolated body rotation like 1.7 did.
     */
    @Inject(method = "getRotationVector(F)Lnet/minecraft/util/math/Vec3d;", at = @At("HEAD"), cancellable = true)
    private void cool$mouseDelayFix(float tickDelta, CallbackInfoReturnable<Vec3d> cir) {
        if (!Hooks189.ready() || (Object) this != MinecraftClient.getInstance().player) return;
        if (!CoolClient.get().modules().active(LegacyModules.MouseDelayFix.class)) return;
        Entity e = (Entity) (Object) this;
        float pitch = e.prevPitch + (e.pitch - e.prevPitch) * tickDelta;
        float yaw = e.prevYaw + (e.yaw - e.prevYaw) * tickDelta;
        float f = (float) Math.cos(-yaw * 0.017453292F - (float) Math.PI);
        float f1 = (float) Math.sin(-yaw * 0.017453292F - (float) Math.PI);
        float f2 = (float) -Math.cos(-pitch * 0.017453292F);
        float f3 = (float) Math.sin(-pitch * 0.017453292F);
        cir.setReturnValue(new Vec3d(f1 * f2, f3, f * f2));
    }

    /** View Model swing speed (animation length only). */
    @Inject(method = "getMiningSpeedMultiplier", at = @At("RETURN"), cancellable = true)
    private void cool$swingSpeed(CallbackInfoReturnable<Integer> cir) {
        if (!Hooks189.ready() || (Object) this != MinecraftClient.getInstance().player) return;
        ViewModel vm = CoolClient.get().modules().get(ViewModel.class);
        if (vm != null && vm.isEnabled() && vm.swingSpeed.d() != 1.0) {
            cir.setReturnValue(Math.max(1, Math.round(cir.getReturnValueI() / vm.swingSpeed.f())));
        }
    }
}
