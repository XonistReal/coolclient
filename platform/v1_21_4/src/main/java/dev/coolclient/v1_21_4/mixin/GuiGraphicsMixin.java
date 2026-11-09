package dev.coolclient.v1_21_4.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.coolclient.core.modules.utility.UtilityTweaks;
import dev.coolclient.v1_21_4.Hooks21;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Tooltips: a tooltip taller than the screen moves with the mouse wheel. */
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
    @Shadow @Final private PoseStack pose;
    @Unique private boolean cool$shifted;

    @Shadow public abstract int guiHeight();

    @Inject(method = "renderTooltipInternal", at = @At("HEAD"))
    private void cool$tooltipStart(Font font, List<ClientTooltipComponent> lines, int x, int y, ClientTooltipPositioner positioner,
                                   ResourceLocation style, CallbackInfo ci) {
        cool$shifted = false;
        if (!Hooks21.ready() || lines.isEmpty() || !UtilityTweaks.Tooltips.scrolling()) return;
        int w = 0, h = lines.size() == 1 ? -2 : 0;
        for (ClientTooltipComponent c : lines) {
            w = Math.max(w, c.getWidth(font));
            h += c.getHeight(font);
        }
        int offset = UtilityTweaks.Tooltips.offset((w << 16) ^ h ^ (lines.size() << 8), h, guiHeight());
        if (offset == 0) return;
        pose.pushPose();
        pose.translate(0f, offset, 0f);
        cool$shifted = true;
    }

    @Inject(method = "renderTooltipInternal", at = @At("RETURN"))
    private void cool$tooltipEnd(Font font, List<ClientTooltipComponent> lines, int x, int y, ClientTooltipPositioner positioner,
                                 ResourceLocation style, CallbackInfo ci) {
        if (cool$shifted) {
            pose.popPose();
            cool$shifted = false;
        }
    }
}
