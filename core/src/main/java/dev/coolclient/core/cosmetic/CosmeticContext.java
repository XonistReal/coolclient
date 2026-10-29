package dev.coolclient.core.cosmetic;

import java.util.UUID;

/** Per-render information about the player wearing cosmetics. Reused between calls. */
public final class CosmeticContext {
    public UUID uuid;
    public boolean self;
    /** Entity age in ticks including partial ticks (drives animations). */
    public float age;
    public float limbSwing;
    public float limbSwingAmount;
    public boolean sneaking;
    /** Creative/elytra flight. */
    public boolean flying;
    public boolean wearingHelmet;
    public boolean wearingChestplate;
    /** Texture handle of a 1x1 white texture. */
    public int white;

    public float seconds() { return age / 20f; }
}
