package dev.coolclient.v1_8_9.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import dev.coolclient.core.CoolClient;
import dev.coolclient.core.render.gpu.SkyRenderer;
import dev.coolclient.v1_8_9.Hooks189;
import net.minecraft.block.material.Material;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Skybox "Match Fog": distant terrain and the clear colour fade into the custom sky's horizon. */
@Mixin(GameRenderer.class)
public abstract class GameRendererFogMixin {
    @Shadow private float fogRed;
    @Shadow private float fogGreen;
    @Shadow private float fogBlue;

    @Inject(method = "updateFog", at = @At("TAIL"))
    private void cool$skyFog(float tickDelta, CallbackInfo ci) {
        if (!Hooks189.ready()) return;
        Entity cam = MinecraftClient.getInstance().getCameraEntity();
        if (cam == null || cam.isSubmergedIn(Material.WATER) || cam.isSubmergedIn(Material.LAVA)) return;
        if (cam instanceof LivingEntity && ((LivingEntity) cam).hasStatusEffect(StatusEffect.BLINDNESS)) return;
        if (!"overworld".equals(CoolClient.get().platform().game().dimension())) return;
        int c = SkyRenderer.fogColor();
        if (c == 0) return;
        fogRed = ((c >> 16) & 0xFF) / 255f;
        fogGreen = ((c >> 8) & 0xFF) / 255f;
        fogBlue = (c & 0xFF) / 255f;
        GlStateManager.clearColor(fogRed, fogGreen, fogBlue, 0f);
    }
}
