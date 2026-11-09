package dev.coolclient.core.api;

/** Version-independent view of an item stack. */
public interface ItemRef {
    ItemRef EMPTY = new ItemRef() {
        @Override public boolean isEmpty() { return true; }
        @Override public String id() { return "minecraft:air"; }
        @Override public String displayName() { return ""; }
        @Override public int count() { return 0; }
        @Override public int damage() { return 0; }
        @Override public int maxDamage() { return 0; }
        @Override public Object handle() { return null; }
    };

    boolean isEmpty();

    /** Namespaced id, e.g. {@code minecraft:diamond_sword}. */
    String id();

    String displayName();

    int count();

    int damage();

    int maxDamage();

    /** The underlying ItemStack, for the platform renderer. */
    Object handle();

    default boolean isDamageable() { return maxDamage() > 0; }

    default int durability() { return maxDamage() - damage(); }

    default float durabilityFraction() {
        return maxDamage() <= 0 ? 1f : Math.max(0f, (maxDamage() - damage()) / (float) maxDamage());
    }

    /** Enchantments as display text, e.g. "Sharpness V". */
    default java.util.List<String> enchantments() { return java.util.Collections.emptyList(); }

    /** 0 common, 1 uncommon, 2 rare, 3 epic. */
    default int rarity() { return 0; }
}
