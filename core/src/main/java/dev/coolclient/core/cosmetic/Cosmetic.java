package dev.coolclient.core.cosmetic;

import dev.coolclient.core.api.ModelRenderer;

/** A wearable cosmetic. Capes are texture-based; everything else is a procedural mesh. */
public abstract class Cosmetic {
    public final String id;
    public final String name;
    public final String description;
    public final CosmeticType type;
    public final Rarity rarity;
    public final int defaultColor;
    /** Whether the player can recolour it. */
    public final boolean tintable;
    public final boolean animated;
    /** How it's unlocked when the API server enforces ownership. */
    public final Unlock unlock;

    protected Cosmetic(String id, String name, String description, CosmeticType type, Rarity rarity,
                       int defaultColor, boolean tintable, boolean animated) {
        this(id, name, description, type, rarity, defaultColor, tintable, animated, Unlock.forRarity(rarity));
    }

    protected Cosmetic(String id, String name, String description, CosmeticType type, Rarity rarity,
                       int defaultColor, boolean tintable, boolean animated, Unlock unlock) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.rarity = rarity;
        this.defaultColor = defaultColor;
        this.tintable = tintable;
        this.animated = animated;
        this.unlock = unlock;
    }

    /**
     * Renders a mesh cosmetic. The pose is already at the attachment point (head or body) and
     * scaled to model pixels. Not used by capes.
     */
    public void render(ModelRenderer mr, CosmeticContext ctx, int color) {}

    /** Model pixels this cosmetic reaches above the top of the head (used to lift nametags). */
    public float headroom() { return 0; }
}
