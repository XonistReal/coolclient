package dev.coolclient.v1_21_4.mixin;

import dev.coolclient.core.modules.visual.RenderTweaks;
import dev.coolclient.v1_21_4.CoolScreen;
import dev.coolclient.v1_21_4.Hooks21;
import dev.coolclient.v1_21_4.Platform21;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Menu Blur for in-world menus (pause, options ...): Cool Client's blur replaces vanilla's. */
@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Unique private boolean cool$blurred;

    @Inject(method = "renderBackground", at = @At("HEAD"))
    private void cool$blur(GuiGraphics g, int mx, int my, float delta, CallbackInfo ci) {
        cool$blurred = false;
        if (!Hooks21.ready() || (Object) this instanceof CoolScreen || Minecraft.getInstance().level == null) return;
        cool$blurred = RenderTweaks.MenuBlur.draw(Platform21.RENDERER.frame(g), false);
        if (cool$blurred) g.flush();
    }

    @Redirect(method = "renderBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;renderBlurredBackground()V"))
    private void cool$vanillaBlur(Screen screen) {
        if (!cool$blurred) ((ScreenInvoker) screen).cool$renderBlurredBackground();
    }
}
