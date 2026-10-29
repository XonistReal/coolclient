package dev.coolclient.core.cosmetic;

/** Equipment slots; one cosmetic per slot. */
public enum CosmeticType {
    CAPE("Capes"),
    WINGS("Wings"),
    HAT("Hats"),
    HALO("Halos"),
    BANDANA("Bandanas"),
    AURA("Auras");

    public final String displayName;

    CosmeticType(String displayName) {
        this.displayName = displayName;
    }
}
