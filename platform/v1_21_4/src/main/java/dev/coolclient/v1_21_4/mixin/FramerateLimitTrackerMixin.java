package dev.coolclient.v1_21_4.mixin;

import com.mojang.blaze3d.platform.FramerateLimitTracker;
import dev.coolclient.core.CoolClient;
import dev.coolclient.core.modules.perf.PerformanceModules;
import dev.coolclient.v1_21_4.Hooks21;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FramerateLimitTracker.class)
public abstract class FramerateLimitTrackerMixin {
    @Inject(method = "getFramerateLimit", at = @At("RETURN"), cancellable = true)
    private void cool$limit(CallbackInfoReturnable<Integer> cir) {
        if (!Hooks21.ready()) return;
        PerformanceModules.FpsLimiter l = CoolClient.get().modules().get(PerformanceModules.FpsLimiter.class);
        if (l == null || !l.isEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (!mc.isWindowActive()) cir.setReturnValue(Math.min(cir.getReturnValueI(), l.unfocused.i()));
        else if (mc.level == null && mc.screen != null) cir.setReturnValue(l.menu.i());
    }
}
