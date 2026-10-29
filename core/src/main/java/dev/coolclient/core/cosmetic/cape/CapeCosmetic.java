package dev.coolclient.core.cosmetic.cape;

import dev.coolclient.core.cosmetic.Cosmetic;
import dev.coolclient.core.cosmetic.CosmeticType;
import dev.coolclient.core.cosmetic.Rarity;
import dev.coolclient.core.cosmetic.Unlock;

public final class CapeCosmetic extends Cosmetic {
    public final CapeStyle style;

    public CapeCosmetic(String id, String name, String description, Rarity rarity, CapeStyle style,
                        int defaultColor, boolean tintable, boolean animated) {
        this(id, name, description, rarity, style, defaultColor, tintable, animated, Unlock.forRarity(rarity));
    }

    public CapeCosmetic(String id, String name, String description, Rarity rarity, CapeStyle style,
                        int defaultColor, boolean tintable, boolean animated, Unlock unlock) {
        super(id, name, description, CosmeticType.CAPE, rarity, defaultColor, tintable, animated, unlock);
        this.style = style;
    }
}
