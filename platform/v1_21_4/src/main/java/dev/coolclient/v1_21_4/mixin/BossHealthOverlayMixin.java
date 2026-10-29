package dev.coolclient.v1_21_4.mixin;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.modules.utility.UtilityModules;
import dev.coolclient.v1_21_4.Hooks21;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.BossHealthOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BossHealthOverlay.class)
public abstract class BossHealthOverlayMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void cool$hide(GuiGraphics g, CallbackInfo ci) {
        if (!Hooks21.ready()) return;
        UtilityModules.BossBarTweaks b = CoolClient.get().modules().get(UtilityModules.BossBarTweaks.class);
        if (b != null && b.isEnabled() && b.hide.on()) ci.cancel();
    }
}
