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

    @org.spongepowered.asm.mixin.injection.Redirect(method = "onScroll", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/Screen;mouseScrolled(DDDD)Z"))
    private boolean cool$screenScroll(net.minecraft.client.gui.screens.Screen screen, double x, double y, double sx, double sy) {
        if (Hooks21.ready() && sy != 0 && dev.coolclient.core.modules.utility.UtilityTweaks.Tooltips.scroll(sy)) return true;
        return screen.mouseScrolled(x, y, sx, sy);
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void cool$scroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (!Hooks21.ready() || window != mc.getWindow().getWindow() || mc.screen != null || mc.player == null) return;
        if (vertical != 0 && CoolClient.get().onScroll(vertical)) ci.cancel();
    }
}
