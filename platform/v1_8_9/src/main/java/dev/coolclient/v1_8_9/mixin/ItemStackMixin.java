package dev.coolclient.v1_8_9.mixin;

import dev.coolclient.core.modules.utility.UtilityTweaks;
import dev.coolclient.v1_8_9.Hooks189;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    /** Tooltips' durability line (advanced tooltips, F3+H, already show one). */
    @Inject(method = "getTooltip", at = @At("RETURN"))
    private void cool$durability(PlayerEntity player, boolean advanced, CallbackInfoReturnable<List<String>> cir) {
        if (!Hooks189.ready() || advanced || !UtilityTweaks.Tooltips.durabilityLine()) return;
        ItemStack self = (ItemStack) (Object) this;
        if (!self.isDamageable()) return;
        int max = self.getMaxDamage(), left = max - self.getDamage();
        float f = max <= 0 ? 1f : left / (float) max;
        String c = f > 0.5f ? "§a" : f > 0.2f ? "§e" : "§c";
        cir.getReturnValue().add("§7Durability: " + c + left + " / " + max);
    }
}
