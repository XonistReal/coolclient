package dev.coolclient.core.api;

/**
 * Capabilities that differ between Minecraft versions. Modules declare the features they
 * need and are only registered on platforms that support all of them.
 */
public enum Feature {
    /** 1.7-style animations (only meaningful on 1.8.9). */
    OLD_ANIMATIONS,
    /** The vanilla 1.8 look-vector "mouse delay" bug exists and can be fixed. */
    MOUSE_DELAY_FIX,
    /** Borderless windowed mode toggle for LWJGL2. */
    BORDERLESS,
    /** Animated texture toggle. */
    TEXTURE_ANIMATIONS,
    /** Offhand slot, totems, shields, item cooldowns (1.9+). */
    OFFHAND,
    TOTEMS,
    SHIELDS,
    COOLDOWNS,
    /** End crystal render tweaks (1.9+ crystal PvP). */
    END_CRYSTALS,
    /** Elytra texture follows cape. */
    ELYTRA,
    /** Post-processing motion blur. */
    MOTION_BLUR,
    /** The 1.9+ attack cooldown. */
    ATTACK_COOLDOWN,
    /** Shulker boxes (1.11+), for the tooltip preview. */
    SHULKER_BOXES
}
