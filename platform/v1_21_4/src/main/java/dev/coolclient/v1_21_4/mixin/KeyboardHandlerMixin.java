package dev.coolclient.v1_21_4.mixin;

import dev.coolclient.core.CoolClient;
import dev.coolclient.v1_21_4.Hooks21;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void cool$key(long window, int key, int scancode, int action, int mods, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (!Hooks21.ready() || window != mc.getWindow().getWindow() || mc.screen != null || action == 2) return;
        if (CoolClient.get().onKey(key, action == 1)) ci.cancel();
    }
}
