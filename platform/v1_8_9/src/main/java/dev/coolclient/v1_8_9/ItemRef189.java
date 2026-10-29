package dev.coolclient.v1_8_9;

import dev.coolclient.core.api.ItemRef;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

final class ItemRef189 implements ItemRef {
    private final ItemStack stack;

    private ItemRef189(ItemStack stack) {
        this.stack = stack;
    }

    static ItemRef of(ItemStack stack) {
        return stack == null || stack.getItem() == null ? ItemRef.EMPTY : new ItemRef189(stack);
    }

    static String idOf(Item item) {
        Object id = Item.REGISTRY.getIdentifier(item);
        return id == null ? "minecraft:air" : id.toString();
    }

    @Override public boolean isEmpty() { return stack.count <= 0; }

    @Override public String id() { return idOf(stack.getItem()); }

    @Override public String displayName() { return stack.getCustomName(); }

    @Override public int count() { return stack.count; }

    @Override public int damage() { return stack.isDamageable() ? stack.getDamage() : 0; }

    @Override public int maxDamage() { return stack.isDamageable() ? stack.getMaxDamage() : 0; }

    @Override public Object handle() { return stack; }

    static Item item(String id) {
        return Item.REGISTRY.get(new Identifier(id));
    }
}
