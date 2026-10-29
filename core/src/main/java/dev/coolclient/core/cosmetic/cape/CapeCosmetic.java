package dev.coolclient.core.cosmetic.cape;

import dev.coolclient.core.cosmetic.Cosmetic;
import dev.coolclient.core.cosmetic.CosmeticType;
import dev.coolclient.core.cosmetic.Rarity;

public final class CapeCosmetic extends Cosmetic {
    public final CapeStyle style;

    public CapeCosmetic(String id, String name, String description, Rarity rarity, CapeStyle style,
                        int defaultColor, boolean tintable, boolean animated) {
        super(id, name, description, CosmeticType.CAPE, rarity, defaultColor, tintable, animated);
        this.style = style;
    }
}
