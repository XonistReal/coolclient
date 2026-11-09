package dev.coolclient.v1_21_4.mixin;

import dev.coolclient.core.modules.utility.UtilityTweaks;
import dev.coolclient.v1_21_4.Hooks21;
import dev.coolclient.v1_21_4.Platform21;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
    /** Drop Protection: the drop key asks twice for protected items. */
    @Inject(method = "drop", at = @At("HEAD"), cancellable = true)
    private void cool$drop(boolean fullStack, CallbackInfoReturnable<Boolean> cir) {
        if (!Hooks21.ready()) return;
        if (!UtilityTweaks.DropProtection.allow(Platform21.held())) cir.setReturnValue(false);
    }
}
