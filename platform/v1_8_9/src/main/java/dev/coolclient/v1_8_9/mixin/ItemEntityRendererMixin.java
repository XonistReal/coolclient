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
        if (!Hooks189.ready()) return;
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
        cir.setReturnValue(count);
    }
}
