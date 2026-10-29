package dev.coolclient.v1_21_4.mixin;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.modules.visual.ViewModel;
import dev.coolclient.v1_21_4.Hooks21;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    /** View Model swing speed: only the local player's swing animation length. */
    @Inject(method = "getCurrentSwingDuration", at = @At("RETURN"), cancellable = true)
    private void cool$swingSpeed(CallbackInfoReturnable<Integer> cir) {
        if (!Hooks21.ready() || (Object) this != Minecraft.getInstance().player) return;
        ViewModel vm = CoolClient.get().modules().get(ViewModel.class);
        if (vm != null && vm.isEnabled() && vm.swingSpeed.d() != 1.0) {
            cir.setReturnValue(Math.max(1, Math.round(cir.getReturnValueI() / vm.swingSpeed.f())));
        }
    }
}
