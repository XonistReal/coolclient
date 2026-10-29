package dev.coolclient.core.cosmetic;

public enum Rarity {
    COMMON("Common", 0xFFB0B8D0),
    RARE("Rare", 0xFF4DA3FF),
    EPIC("Epic", 0xFF8A5CFF),
    LEGENDARY("Legendary", 0xFFFFB547),
    MYTHIC("Mythic", 0xFFFF5470);

    public final String displayName;
    public final int color;

    Rarity(String displayName, int color) {
        this.displayName = displayName;
        this.color = color;
    }
}
