package dev.coolclient.v1_8_9.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import dev.coolclient.v1_8_9.Chams189;
import dev.coolclient.v1_8_9.CosmeticsFeature;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin extends LivingEntityRenderer<AbstractClientPlayerEntity> {
    private PlayerEntityRendererMixin(EntityRenderDispatcher d, EntityModel m, float f) {
        super(d, m, f);
    }

    @Inject(method = "<init>(Lnet/minecraft/client/render/entity/EntityRenderDispatcher;Z)V", at = @At("TAIL"))
    private void cool$addCosmetics(EntityRenderDispatcher dispatcher, boolean slim, CallbackInfo ci) {
        this.addFeature(new CosmeticsFeature((PlayerEntityRenderer) (Object) this));
    }

    /** Hand Chams: vanilla forces white right before drawing the first-person arm. */
    @Redirect(method = "renderRightArm", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;color(FFF)V"))
    private void cool$armColor(float r, float g, float b) {
        int c = Chams189.armColor;
        if (c == 0) {
            GlStateManager.color(r, g, b);
            return;
        }
        GlStateManager.color(((c >> 16) & 0xFF) / 255f, ((c >> 8) & 0xFF) / 255f, (c & 0xFF) / 255f, ((c >>> 24) & 0xFF) / 255f);
    }
}
