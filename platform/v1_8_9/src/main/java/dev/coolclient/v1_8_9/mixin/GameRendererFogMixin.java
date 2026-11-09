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
    @Shadow private float viewDistance;
    @Shadow private boolean thickFog;

    /** Fog module: distance fog pushed back or removed, clearer water. Lava and blindness stay vanilla. */
    @Inject(method = "renderFog", at = @At("TAIL"))
    private void cool$fog(int pass, float tickDelta, CallbackInfo ci) {
        if (!Hooks189.ready() || pass == -1) return; // -1 is the sky pass
        MinecraftClient mc = MinecraftClient.getInstance();
        Entity cam = mc.getCameraEntity();
        if (cam == null || mc.world == null) return;
        if (cam instanceof LivingEntity && ((LivingEntity) cam).hasStatusEffect(StatusEffect.BLINDNESS)) return;
        Material m = net.minecraft.client.render.Camera.getSubmergedBlock(mc.world, cam, tickDelta).getMaterial();
        if (m == Material.LAVA) return;
        if (m == Material.WATER) {
            float k = dev.coolclient.core.modules.visual.RenderTweaks.Fog.water();
            if (k == 1f) return;
            float density = cam instanceof LivingEntity && ((LivingEntity) cam).hasStatusEffect(StatusEffect.WATER_BREATHING)
                    ? 0.01f : 0.1f - net.minecraft.enchantment.EnchantmentHelper.getRespiration(cam) * 0.03f;
            GlStateManager.fogDensity(density / k);
            return;
        }
        if (thickFog) return;
        float k = dev.coolclient.core.modules.visual.RenderTweaks.Fog.terrain();
        if (k == 1f) return;
        float f = viewDistance;
        boolean thick = mc.world.dimension.isFogThick((int) cam.x, (int) cam.z);
        float start = thick ? f * 0.05f : f * 0.75f, end = thick ? Math.min(f, 192f) * 0.5f : f;
        if (Float.isInfinite(k)) {
            GlStateManager.fogStart(1e6f);
            GlStateManager.fogEnd(2e6f);
        } else {
            GlStateManager.fogStart(start * k);
            GlStateManager.fogEnd(end * k);
        }
    }

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
