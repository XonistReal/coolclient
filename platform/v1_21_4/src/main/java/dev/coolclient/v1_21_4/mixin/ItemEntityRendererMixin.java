package dev.coolclient.v1_21_4.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.coolclient.core.CoolClient;
import dev.coolclient.core.modules.visual.VisualTweaks;
import dev.coolclient.v1_21_4.Hooks21;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.world.entity.item.ItemEntity;
import org.joml.Quaternionf;
import dev.coolclient.v1_21_4.ItemStateDuck;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Item Physics: dropped items lie flat instead of bobbing and spinning. */
@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin {
    @Unique private static ItemEntityRenderState cool$state;
    @Unique private static VisualTweaks.ItemPhysics cool$physics;

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;F)V",
            at = @At("TAIL"))
    private void cool$extract(ItemEntity item, ItemEntityRenderState state, float tickDelta, CallbackInfo ci) {
        ((ItemStateDuck) state).cool$setOnGround(item.onGround());
    }

    @Inject(method = "render(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"))
    private void cool$begin(ItemEntityRenderState state, PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo ci) {
        cool$state = state;
        VisualTweaks.ItemPhysics p = Hooks21.ready() ? CoolClient.get().modules().get(VisualTweaks.ItemPhysics.class) : null;
        cool$physics = p != null && p.isEnabled() ? p : null;
    }

    @Redirect(method = "render(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"))
    private void cool$translate(PoseStack pose, float x, float y, float z) {
        if (cool$physics == null) {
            pose.translate(x, y, z);
            return;
        }
        boolean ground = ((ItemStateDuck) cool$state).cool$onGround();
        pose.translate(0f, ground ? 0.03f : 0.15f, 0f);
    }

    @Redirect(method = "render(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionf;)V"))
    private void cool$rotate(PoseStack pose, Quaternionf q) {
        VisualTweaks.ItemPhysics p = cool$physics;
        if (p == null) {
            pose.mulPose(q);
            return;
        }
        ItemEntityRenderState s = cool$state;
        float seed = s.bobOffset * 57.29578f;
        if (((ItemStateDuck) s).cool$onGround()) {
            pose.mulPose(new Quaternionf().rotationY((float) Math.toRadians(seed)));
            pose.mulPose(new Quaternionf().rotationX((float) Math.toRadians(90)));
        } else {
            float spin = s.ageInTicks * 12f * p.spin.f();
            pose.mulPose(new Quaternionf().rotationY((float) Math.toRadians(seed + spin)));
            pose.mulPose(new Quaternionf().rotationX((float) Math.toRadians(spin * 0.7f)));
        }
    }
}
