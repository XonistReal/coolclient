package dev.coolclient.v1_21_4.mixin;

import dev.coolclient.core.CoolClient;
import dev.coolclient.v1_21_4.Hooks21;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Inject(method = "onPress", at = @At("HEAD"))
    private void cool$press(long window, int button, int action, int mods, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (!Hooks21.ready() || window != mc.getWindow().getWindow() || mc.screen != null) return;
        CoolClient.get().onMouseButton(button, action == 1);
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void cool$scroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (!Hooks21.ready() || window != mc.getWindow().getWindow() || mc.screen != null || mc.player == null) return;
        if (vertical != 0 && CoolClient.get().onScroll(vertical)) ci.cancel();
    }
}
