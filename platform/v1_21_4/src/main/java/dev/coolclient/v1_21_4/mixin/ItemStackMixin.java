package dev.coolclient.v1_21_4.mixin;

import dev.coolclient.core.modules.utility.UtilityTweaks;
import dev.coolclient.core.modules.visual.RenderTweaks;
import dev.coolclient.v1_21_4.Hooks21;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    /** Glint's "Shiny Potions": potions with effects get the enchantment glint again. */
    @Inject(method = "hasFoil", at = @At("RETURN"), cancellable = true)
    private void cool$shinyPotions(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() || !Hooks21.ready() || !RenderTweaks.Glint.potions()) return;
        ItemStack self = (ItemStack) (Object) this;
        if (!(self.getItem() instanceof PotionItem)) return;
        PotionContents p = self.get(DataComponents.POTION_CONTENTS);
        if (p != null && p.hasEffects()) cir.setReturnValue(true);
    }

    /** Tooltips' durability line (advanced tooltips, F3+H, already show one). */
    @Inject(method = "getTooltipLines", at = @At("RETURN"))
    private void cool$durability(Item.TooltipContext context, Player player, TooltipFlag flag, CallbackInfoReturnable<List<Component>> cir) {
        if (!Hooks21.ready() || flag.isAdvanced() || !UtilityTweaks.Tooltips.durabilityLine()) return;
        ItemStack self = (ItemStack) (Object) this;
        if (!self.isDamageableItem()) return;
        int max = self.getMaxDamage(), left = max - self.getDamageValue();
        float f = max <= 0 ? 1f : left / (float) max;
        ChatFormatting c = f > 0.5f ? ChatFormatting.GREEN : f > 0.2f ? ChatFormatting.YELLOW : ChatFormatting.RED;
        List<Component> lines = cir.getReturnValue();
        try {
            lines.add(Component.literal("Durability: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(left + " / " + max).withStyle(c)));
        } catch (UnsupportedOperationException ignored) {
            // An immutable list (e.g. the empty one for hidden tooltips).
        }
    }
}
