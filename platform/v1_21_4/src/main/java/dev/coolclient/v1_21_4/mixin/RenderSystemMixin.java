package dev.coolclient.v1_21_4.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.coolclient.v1_21_4.Hooks21;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Restores the shader colour that {@link RenderStateShardMixin} changed for a recoloured glint. */
@Mixin(RenderSystem.class)
public abstract class RenderSystemMixin {
    @Inject(method = "resetTextureMatrix", at = @At("HEAD"))
    private static void cool$glintDone(CallbackInfo ci) {
        float[] c = Hooks21.savedShaderColor;
        if (c == null) return;
        Hooks21.savedShaderColor = null;
        RenderSystem.setShaderColor(c[0], c[1], c[2], c[3]);
    }
}
