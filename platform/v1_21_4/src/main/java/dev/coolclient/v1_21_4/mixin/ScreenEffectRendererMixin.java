package dev.coolclient.v1_21_4.mixin;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.modules.visual.VisualTweaks;
import dev.coolclient.v1_21_4.Hooks21;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Low Fire: lower and fade the first-person fire overlay. */
@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererMixin {
    @Unique
    private static VisualTweaks.LowFire cool$lowFire() {
        if (!Hooks21.ready()) return null;
        VisualTweaks.LowFire l = CoolClient.get().modules().get(VisualTweaks.LowFire.class);
        return l != null && l.isEnabled() ? l : null;
    }

    @ModifyConstant(method = "renderFire", constant = @Constant(floatValue = -0.3f))
    private static float cool$height(float y) {
        VisualTweaks.LowFire l = cool$lowFire();
        return l == null ? y : y - l.height.f();
    }

    @ModifyConstant(method = "renderFire", constant = @Constant(floatValue = 0.9f))
    private static float cool$alpha(float alpha) {
        VisualTweaks.LowFire l = cool$lowFire();
        return l == null ? alpha : alpha * l.opacity.f() / 100f;
    }
}
