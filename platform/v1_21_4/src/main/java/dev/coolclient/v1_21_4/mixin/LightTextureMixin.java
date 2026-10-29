package dev.coolclient.v1_21_4.mixin;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.modules.visual.VisualTweaks;
import dev.coolclient.v1_21_4.Hooks21;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LightTexture.class)
public abstract class LightTextureMixin {
    /** Fullbright: boost the gamma read (ordinal 1 is options.gamma().get()) without touching options.txt. */
    @Redirect(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;", ordinal = 1))
    private Object cool$gamma(OptionInstance<?> option) {
        if (Hooks21.ready() && CoolClient.get().modules().active(VisualTweaks.Fullbright.class)) return 15.0;
        return option.get();
    }
}
