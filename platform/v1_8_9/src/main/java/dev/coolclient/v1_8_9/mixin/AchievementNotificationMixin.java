package dev.coolclient.v1_8_9.mixin;

import dev.coolclient.core.modules.utility.UtilityTweaks;
import dev.coolclient.v1_8_9.Hooks189;
import net.minecraft.advancement.Achievement;
import net.minecraft.client.gui.AchievementNotification;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Toast Control on 1.8: achievement pop-ups and the "open your inventory" hint. */
@Mixin(AchievementNotification.class)
public abstract class AchievementNotificationMixin {
    @Inject(method = "display", at = @At("HEAD"), cancellable = true)
    private void cool$achievement(Achievement achievement, CallbackInfo ci) {
        if (Hooks189.ready() && UtilityTweaks.ToastControl.hide("advancement")) ci.cancel();
    }

    @Inject(method = "displayRaw", at = @At("HEAD"), cancellable = true)
    private void cool$hint(Achievement achievement, CallbackInfo ci) {
        if (Hooks189.ready() && UtilityTweaks.ToastControl.hide("tutorial")) ci.cancel();
    }
}
