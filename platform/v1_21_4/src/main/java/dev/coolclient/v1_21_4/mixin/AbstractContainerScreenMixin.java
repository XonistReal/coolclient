package dev.coolclient.v1_21_4.mixin;

import dev.coolclient.core.modules.utility.UtilityTweaks;
import dev.coolclient.core.modules.visual.RenderTweaks;
import dev.coolclient.v1_21_4.Hooks21;
import dev.coolclient.v1_21_4.Platform21;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
    @Shadow protected Slot hoveredSlot;

    /** Menu Blur behind inventories and containers (vanilla only darkens them). */
    @Inject(method = "renderBackground", at = @At("HEAD"))
    private void cool$blur(GuiGraphics g, int mx, int my, float delta, CallbackInfo ci) {
        if (!Hooks21.ready() || Minecraft.getInstance().level == null) return;
        if (RenderTweaks.MenuBlur.draw(Platform21.RENDERER.frame(g), true)) g.flush();
    }

    /** Tooltips' shulker box preview: the box's 27 slots drawn as a grid above the tooltip. */
    @Inject(method = "renderTooltip", at = @At("TAIL"))
    private void cool$shulker(GuiGraphics g, int mx, int my, CallbackInfo ci) {
        if (!Hooks21.ready() || !UtilityTweaks.Tooltips.shulkerPreview()) return;
        Slot slot = hoveredSlot;
        if (slot == null || !slot.hasItem()) return;
        ItemStack box = slot.getItem();
        if (!(box.getItem() instanceof BlockItem) || !(((BlockItem) box.getItem()).getBlock() instanceof ShulkerBoxBlock)) return;
        ItemContainerContents contents = box.get(DataComponents.CONTAINER);
        if (contents == null || !contents.nonEmptyItems().iterator().hasNext()) return;
        net.minecraft.core.NonNullList<ItemStack> items = net.minecraft.core.NonNullList.withSize(27, ItemStack.EMPTY);
        contents.copyInto(items);

        int w = 9 * 18, h = 3 * 18;
        int x = mx + 12, y = my - h - 18;
        if (y < 4) y = my + 24;
        if (x + w + 4 > g.guiWidth()) x = Math.max(4, g.guiWidth() - w - 4);
        Font font = Minecraft.getInstance().font;
        g.pose().pushPose();
        g.pose().translate(0, 0, 500);
        TooltipRenderUtil.renderTooltipBackground(g, x, y, w, h, 0, null);
        for (int i = 0; i < 27; i++) {
            int sx = x + (i % 9) * 18 + 1, sy = y + (i / 9) * 18 + 1;
            g.fill(sx, sy, sx + 16, sy + 16, 0x30FFFFFF);
            ItemStack s = items.get(i);
            if (s.isEmpty()) continue;
            g.renderItem(s, sx, sy);
            g.renderItemDecorations(font, s, sx, sy);
        }
        g.pose().popPose();
    }
}
