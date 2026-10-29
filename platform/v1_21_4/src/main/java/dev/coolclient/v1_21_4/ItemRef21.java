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
}
