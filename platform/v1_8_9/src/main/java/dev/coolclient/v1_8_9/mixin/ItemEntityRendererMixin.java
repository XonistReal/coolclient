package dev.coolclient.v1_8_9.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import dev.coolclient.core.CoolClient;
import dev.coolclient.core.modules.visual.VisualTweaks;
import dev.coolclient.v1_8_9.Hooks189;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin {
    /** Item Physics: dropped items lie flat instead of bobbing and spinning. */
    @Inject(method = "method_10221", at = @At("HEAD"), cancellable = true)
    private void cool$physics(ItemEntity item, double x, double y, double z, float tickDelta, BakedModel model,
                                CallbackInfoReturnable<Integer> cir) {
        if (!Hooks189.ready() || dev.coolclient.core.modules.visual.RenderTweaks.FlatItems.on()) return;
        VisualTweaks.ItemPhysics p = CoolClient.get().modules().get(VisualTweaks.ItemPhysics.class);
        if (p == null || !p.isEnabled()) return;
        ItemStack stack = item.getItemStack();
        if (stack == null || stack.getItem() == null) {
            cir.setReturnValue(0);
            return;
        }
        boolean block = model.hasDepth();
        int count = stack.count > 48 ? 5 : stack.count > 32 ? 4 : stack.count > 16 ? 3 : stack.count > 1 ? 2 : 1;
        float seed = item.hoverHeight * 57.29578f;
        if (item.onGround) {
            GlStateManager.translate((float) x, (float) y + (block ? 0.12f : 0.02f), (float) z);
            GlStateManager.rotate(seed, 0f, 1f, 0f);
            if (!block) GlStateManager.rotate(90f, 1f, 0f, 0f);
        } else {
            float spin = (item.getAge() + tickDelta) * 12f * p.spin.f();
            GlStateManager.translate((float) x, (float) y + 0.15f, (float) z);
            GlStateManager.rotate(seed + spin, 0f, 1f, 0f);
            GlStateManager.rotate(spin * 0.7f, 1f, 0f, 0f);
        }
        GlStateManager.color(1f, 1f, 1f, 1f);
        cool$uhc(item);
        cir.setReturnValue(count);
    }

    /** UHC Overlay: bigger gold, golden apples and heads. Also runs after the vanilla transform. */
    @Inject(method = "method_10221", at = @At("RETURN"))
    private void cool$uhcVanilla(ItemEntity item, double x, double y, double z, float tickDelta, BakedModel model,
                                 CallbackInfoReturnable<Integer> cir) {
        cool$uhc(item);
    }

    @org.spongepowered.asm.mixin.Unique
    private static void cool$uhc(ItemEntity item) {
        if (!Hooks189.ready() || item.getItemStack() == null || item.getItemStack().getItem() == null) return;
        String id = String.valueOf(net.minecraft.item.Item.REGISTRY.getIdentifier(item.getItemStack().getItem()));
        float s = dev.coolclient.core.modules.visual.RenderTweaks.UhcOverlay.scale(id);
        if (s != 1f) GlStateManager.scale(s, s, s);
    }

    /** 2D Items: replace the spin with a turn toward the camera. */
    @org.spongepowered.asm.mixin.injection.Redirect(method = "method_10221", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/GlStateManager;rotate(FFFF)V"))
    private void cool$flat(float angle, float x, float y, float z) {
        if (Hooks189.ready() && dev.coolclient.core.modules.visual.RenderTweaks.FlatItems.on()) {
            net.minecraft.client.render.entity.EntityRenderDispatcher d = net.minecraft.client.MinecraftClient.getInstance().getEntityRenderManager();
            GlStateManager.rotate(180f - d.yaw, 0f, 1f, 0f);
            GlStateManager.rotate(-d.pitch, 1f, 0f, 0f);
            return;
        }
        GlStateManager.rotate(angle, x, y, z);
    }
}
