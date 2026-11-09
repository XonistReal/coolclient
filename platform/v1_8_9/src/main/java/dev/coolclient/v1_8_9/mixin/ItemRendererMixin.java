package dev.coolclient.v1_8_9.mixin;

import dev.coolclient.v1_8_9.Chams189;
import net.minecraft.client.render.item.ItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
    /** Hand chams: recolours quads only while the local first-person item is being drawn. */
    @ModifyVariable(method = "renderBakedItemModel(Lnet/minecraft/client/render/model/BakedModel;ILnet/minecraft/item/ItemStack;)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int cool$chamsColor(int color) {
        return Chams189.itemColor != 0 ? Chams189.itemColor : color;
    }

    /** Glint colour for items (both glint passes). */
    @org.spongepowered.asm.mixin.injection.ModifyArg(method = "renderGlint", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/item/ItemRenderer;renderBakedItemModel(Lnet/minecraft/client/render/model/BakedModel;I)V"), index = 1)
    private int cool$glint(int color) {
        return dev.coolclient.v1_8_9.Hooks189.ready() ? dev.coolclient.v1_8_9.Hooks189.glintColor(color) : color;
    }
}
