package dev.coolclient.v1_8_9.mixin;

import dev.coolclient.v1_8_9.Platform189;
import net.minecraft.client.texture.SpriteAtlasTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpriteAtlasTexture.class)
public abstract class SpriteAtlasTextureMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void cool$animations(CallbackInfo ci) {
        if (!Platform189.textureAnimations) ci.cancel();
    }
}
