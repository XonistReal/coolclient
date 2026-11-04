package dev.coolclient.v1_21_4.mixin;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.modules.visual.VisualTweaks;
import dev.coolclient.v1_21_4.Hooks21;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Time Changer. The sky reads the time through {@code LevelAccessor.dayTime()}, not {@code Level.getDayTime()},
 * and both end up here, so this is the one place that covers sky, light and the moon.
 */
@Mixin(ClientLevel.ClientLevelData.class)
public abstract class ClientLevelDataMixin {
    @Inject(method = "getDayTime", at = @At("HEAD"), cancellable = true)
    private void cool$time(CallbackInfoReturnable<Long> cir) {
        if (!Hooks21.ready()) return;
        VisualTweaks.TimeChanger t = CoolClient.get().modules().get(VisualTweaks.TimeChanger.class);
        if (t != null && t.isEnabled()) cir.setReturnValue((long) t.time.d());
    }
}
