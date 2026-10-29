package dev.coolclient.v1_8_9.mixin;

import dev.coolclient.core.CoolClient;
import dev.coolclient.v1_8_9.Hooks189;
import net.minecraft.entity.player.PlayerInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerInventory.class)
public abstract class PlayerInventoryMixin {
    /** Lets Zoom (scroll-to-adjust) consume the mouse wheel. */
    @Inject(method = "scrollInHotbar", at = @At("HEAD"), cancellable = true)
    private void cool$scroll(int amount, CallbackInfo ci) {
        if (Hooks189.ready() && CoolClient.get().onScroll(amount > 0 ? 1 : -1)) ci.cancel();
    }
}
