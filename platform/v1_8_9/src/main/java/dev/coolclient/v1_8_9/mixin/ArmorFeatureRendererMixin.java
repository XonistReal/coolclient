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

    /** Glint colour on worn armor: the purple colour call inside the glint loop. */
    @org.spongepowered.asm.mixin.injection.Redirect(method = "renderGlint", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/GlStateManager;color(FFFF)V", ordinal = 1))
    private void cool$glint(float r, float g, float b, float a) {
        float[] f = Hooks189.ready() ? Hooks189.glintFactors() : null;
        if (f == null) com.mojang.blaze3d.platform.GlStateManager.color(r, g, b, a);
        else com.mojang.blaze3d.platform.GlStateManager.color(Math.min(1f, r * f[0]), Math.min(1f, g * f[1]), Math.min(1f, b * f[2]), a);
    }
}
