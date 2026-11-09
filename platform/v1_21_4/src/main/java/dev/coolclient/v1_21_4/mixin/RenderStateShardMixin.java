package dev.coolclient.v1_21_4.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.coolclient.core.modules.visual.RenderTweaks;
import dev.coolclient.v1_21_4.Hooks21;
import net.minecraft.client.renderer.RenderStateShard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Glint colour: the glint shaders multiply the (purple) glint texture by ColorModulator, so while a
 * glint render type is set up the shader colour becomes target / texture colour. {@link RenderSystemMixin}
 * puts it back when the glint's texture matrix is reset.
 */
@Mixin(RenderStateShard.class)
public abstract class RenderStateShardMixin {
    /** Brightest texel of the vanilla glint texture (167, 85, 255). */
    private static final float BASE_R = 167 / 255f, BASE_G = 85 / 255f, BASE_B = 1f;

    @Inject(method = "setupGlintTexturing", at = @At("TAIL"))
    private static void cool$glintColor(float scale, CallbackInfo ci) {
        if (!Hooks21.ready()) return;
        float[] c = RenderTweaks.Glint.rgb();
        if (c == null) return;
        float[] cur = RenderSystem.getShaderColor();
        Hooks21.savedShaderColor = new float[] {cur[0], cur[1], cur[2], cur[3]};
        RenderSystem.setShaderColor(c[0] / BASE_R, c[1] / BASE_G, c[2] / BASE_B, cur[3]);
    }
}
