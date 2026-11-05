package dev.coolclient.core.cosmetic;

/**
 * How a cosmetic is unlocked when the API server enforces ownership. The code is open source,
 * so anyone can equip anything locally; what a paid unlock buys is that other players see it.
 * Keep this in sync with {@code server/catalog.json} (CatalogSyncTest checks it).
 */
public enum Unlock {
    FREE("Free", 0xFF8D97B0),
    /** Included with the Supporter rank (or Supporter+). */
    SUPPORTER("Supporter", 0xFFFFC83A),
    /** Sold individually in the store, or included with Supporter+. */
    STORE("Store", 0xFFFF5470),
    /** Never sold: granted by the server (the Founder cape goes to the first players). */
    EXCLUSIVE("Exclusive", 0xFFFFD34D),
    /** Never sold: comes with the Media rank, which the team gives to creators. */
    MEDIA("Media", 0xFFFF3B4E);

    public final String displayName;
    public final int color;

    Unlock(String displayName, int color) {
        this.displayName = displayName;
        this.color = color;
    }

    /** Whether there's a store page for it (exclusive and Media cosmetics are only ever given). */
    public boolean sold() {
        return this != EXCLUSIVE && this != MEDIA;
    }

    /** Default tier: common and rare are free, epic and legendary come with Supporter, mythic is sold. */
    public static Unlock forRarity(Rarity r) {
        switch (r) {
            case COMMON:
            case RARE:
                return FREE;
            case EPIC:
            case LEGENDARY:
                return SUPPORTER;
            default:
                return STORE;
        }
    }
}
