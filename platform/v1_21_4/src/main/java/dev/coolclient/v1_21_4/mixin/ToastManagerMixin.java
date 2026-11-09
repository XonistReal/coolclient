package dev.coolclient.v1_21_4.mixin;

import dev.coolclient.core.modules.utility.UtilityTweaks;
import dev.coolclient.v1_21_4.Hooks21;
import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.client.gui.components.toasts.RecipeToast;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.gui.components.toasts.TutorialToast;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Toast Control: drop the kinds of pop-ups the player turned off. */
@Mixin(ToastManager.class)
public abstract class ToastManagerMixin {
    @Inject(method = "addToast", at = @At("HEAD"), cancellable = true)
    private void cool$toast(Toast toast, CallbackInfo ci) {
        if (!Hooks21.ready()) return;
        String kind = toast instanceof AdvancementToast ? "advancement" : toast instanceof RecipeToast ? "recipe"
                : toast instanceof TutorialToast ? "tutorial" : toast instanceof SystemToast ? "system" : "";
        if (UtilityTweaks.ToastControl.hide(kind)) ci.cancel();
    }
}
