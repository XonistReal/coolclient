package dev.coolclient.v1_21_4.mixin;

import dev.coolclient.v1_21_4.Platform21;
import net.minecraft.client.renderer.texture.TextureAtlas;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextureAtlas.class)
public abstract class TextureAtlasMixin {
    @Inject(method = "cycleAnimationFrames", at = @At("HEAD"), cancellable = true)
    private void cool$animations(CallbackInfo ci) {
        if (!Platform21.textureAnimations) ci.cancel();
    }
}
