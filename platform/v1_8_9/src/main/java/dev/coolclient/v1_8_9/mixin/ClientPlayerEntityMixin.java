package dev.coolclient.v1_8_9.mixin;

import dev.coolclient.core.modules.utility.UtilityTweaks;
import dev.coolclient.v1_8_9.Hooks189;
import dev.coolclient.v1_8_9.Platform189;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin {
    /** Drop Protection: the drop key asks twice for protected items. */
    @Inject(method = "dropSelectedItem", at = @At("HEAD"), cancellable = true)
    private void cool$drop(boolean all, CallbackInfoReturnable<ItemEntity> cir) {
        if (Hooks189.ready() && !UtilityTweaks.DropProtection.allow(Platform189.held())) cir.setReturnValue(null);
    }
}
