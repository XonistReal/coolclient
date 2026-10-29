package dev.coolclient.v1_8_9.mixin;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.modules.legacy.LegacyModules;
import dev.coolclient.v1_8_9.Hooks189;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorFeatureRenderer.class)
public abstract class ArmorFeatureRendererMixin {
    /** 1.7 animations: armour flashes red with the player when hurt. */
    @Inject(method = "combineTextures", at = @At("RETURN"), cancellable = true)
    private void cool$redArmor(CallbackInfoReturnable<Boolean> cir) {
        if (!Hooks189.ready()) return;
        LegacyModules.OldAnimations o = CoolClient.get().modules().get(LegacyModules.OldAnimations.class);
        if (o != null && o.isEnabled() && o.armorDamage.on()) cir.setReturnValue(true);
    }
}
