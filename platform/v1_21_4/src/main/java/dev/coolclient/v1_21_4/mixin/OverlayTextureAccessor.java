package dev.coolclient.v1_21_4.mixin;

import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(OverlayTexture.class)
public interface OverlayTextureAccessor {
    @Accessor("texture")
    DynamicTexture cool$texture();
}
