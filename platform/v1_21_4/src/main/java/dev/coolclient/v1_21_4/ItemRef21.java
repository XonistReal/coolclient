package dev.coolclient.v1_21_4;

import dev.coolclient.core.api.ItemRef;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class ItemRef21 implements ItemRef {
    private final ItemStack stack;

    private ItemRef21(ItemStack stack) {
        this.stack = stack;
    }

    static ItemRef of(ItemStack stack) {
        return stack == null || stack.isEmpty() ? ItemRef.EMPTY : new ItemRef21(stack);
    }

    static Item item(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) return null;
        Item it = BuiltInRegistries.ITEM.getValue(rl);
        return it == Items.AIR ? null : it;
    }

    @Override public boolean isEmpty() { return stack.isEmpty(); }

    @Override public String id() { return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(); }

    @Override public String displayName() { return stack.getHoverName().getString(); }

    @Override public int count() { return stack.getCount(); }

    @Override public int damage() { return stack.isDamageableItem() ? stack.getDamageValue() : 0; }

    @Override public int maxDamage() { return stack.isDamageableItem() ? stack.getMaxDamage() : 0; }

    @Override public Object handle() { return stack; }

    @Override
    public java.util.List<String> enchantments() {
        java.util.List<String> out = new java.util.ArrayList<String>();
        net.minecraft.world.item.enchantment.ItemEnchantments e = stack.getEnchantments();
        if (e.isEmpty()) e = stack.getOrDefault(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS, e);
        for (it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment>> en : e.entrySet()) {
            out.add(net.minecraft.world.item.enchantment.Enchantment.getFullname(en.getKey(), en.getIntValue()).getString());
        }
        return out;
    }

    @Override public int rarity() { return stack.getRarity().ordinal(); }
}
