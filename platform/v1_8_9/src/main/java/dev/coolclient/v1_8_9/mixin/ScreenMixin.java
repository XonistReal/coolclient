package dev.coolclient.v1_8_9.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import dev.coolclient.core.modules.utility.UtilityTweaks;
import dev.coolclient.v1_8_9.CoolScreen;
import dev.coolclient.v1_8_9.Hooks189;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Shadow public int height;
    @Unique private boolean cool$shifted;

    /** Menu Blur: blur the world under the dark in-game menu background. */
    @Inject(method = "renderBackground(I)V", at = @At("HEAD"))
    private void cool$blur(int alpha, CallbackInfo ci) {
        if (!Hooks189.ready() || (Object) this instanceof CoolScreen || MinecraftClient.getInstance().world == null) return;
        Hooks189.menuBlur((Object) this instanceof HandledScreen);
    }

    /** Tooltips: the mouse wheel moves a tooltip that is taller than the screen. */
    @Inject(method = "handleMouse", at = @At("HEAD"), cancellable = true)
    private void cool$wheel(CallbackInfo ci) {
        if (!Hooks189.ready()) return;
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0 && UtilityTweaks.Tooltips.scroll(Math.signum(wheel))) ci.cancel();
    }

    @Inject(method = "renderTooltip(Ljava/util/List;II)V", at = @At("HEAD"))
    private void cool$tooltipStart(List<String> text, int x, int y, CallbackInfo ci) {
        cool$shifted = false;
        if (!Hooks189.ready() || text.isEmpty() || !UtilityTweaks.Tooltips.scrolling()) return;
        int h = 8 + (text.size() > 1 ? 2 + (text.size() - 1) * 10 : 0);
        int offset = UtilityTweaks.Tooltips.offset(text.get(0) + "|" + text.size(), h + 8, height);
        // 1.8 pins a tall tooltip to the bottom of the screen, so start at the top and scroll down from there.
        int over = Math.max(0, h + 8 - height + 16);
        if (over == 0) return;
        GlStateManager.pushMatrix();
        GlStateManager.translate(0f, over + offset, 0f);
        cool$shifted = true;
    }

    @Inject(method = "renderTooltip(Ljava/util/List;II)V", at = @At("RETURN"))
    private void cool$tooltipEnd(List<String> text, int x, int y, CallbackInfo ci) {
        if (cool$shifted) {
            GlStateManager.popMatrix();
            cool$shifted = false;
        }
    }
}
