package dev.coolclient.core.cosmetic;

import dev.coolclient.core.cosmetic.cape.CapeCosmetic;
import dev.coolclient.core.cosmetic.cape.CapeStyles;
import dev.coolclient.core.cosmetic.model.AuraCosmetic;
import dev.coolclient.core.cosmetic.model.HeadCosmetics;
import dev.coolclient.core.cosmetic.model.WingsCosmetic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Every cosmetic in the client. Unlock tiers are in {@link Unlock}; nothing is locked until the server enforces it. */
public final class CosmeticCatalog {
    private static final Map<String, Cosmetic> ALL = new LinkedHashMap<String, Cosmetic>();

    static {
        // Thanksgiving drop: free every November, and yours to keep once you've worn it.
        add(new CapeCosmetic("cape_autumn", "Autumn", "Maple leaves tumbling down on a warm afternoon.", Rarity.RARE, CapeStyles.AUTUMN, 0xFFF29A1F, false, true, Unlock.FREE), Season.THANKSGIVING);
        add(new CapeCosmetic("cape_gobbler", "Gobbler", "A proud turkey showing off its tail. Gobbles now and then.", Rarity.EPIC, CapeStyles.GOBBLER, 0xFFE0731F, false, true, Unlock.FREE), Season.THANKSGIVING);
        add(new CapeCosmetic("cape_harvestmoon", "Harvest Moon", "A big orange moon over the fields, geese heading south.", Rarity.LEGENDARY, CapeStyles.HARVEST_MOON, 0xFFFF9A3C, false, true, Unlock.FREE), Season.THANKSGIVING);
        add(new HeadCosmetics.PilgrimHat("hat_pilgrim", "Pilgrim Hat", Rarity.RARE, 0xFF26262E), Season.THANKSGIVING);
        add(new HeadCosmetics.TurkeyHat("hat_turkey", "Turkey Hat", Rarity.EPIC, 0xFF7A4520), Season.THANKSGIVING);
        add(new AuraCosmetic("aura_leaves", "Falling Leaves", "Your own little autumn.", Rarity.RARE, AuraCosmetic.Style.LEAVES, 0xFFF29A1F, Unlock.FREE), Season.THANKSGIVING);

        // Capes
        add(new CapeCosmetic("cape_aurora", "Aurora Borealis", "Northern lights dancing over a starry sky.", Rarity.MYTHIC, CapeStyles.AURORA, 0xFF2DE2C1, false, true));
        add(new CapeCosmetic("cape_logo", "Cool Emblem", "The official Cool Client cape.", Rarity.LEGENDARY, CapeStyles.COOL_LOGO, 0xFF2DE2C1, false, true));
        add(new CapeCosmetic("cape_galaxy", "Galaxy", "A slowly turning nebula.", Rarity.LEGENDARY, CapeStyles.GALAXY, 0xFF6A2BD9, false, true));
        add(new CapeCosmetic("cape_inferno", "Inferno", "Flames that never go out.", Rarity.EPIC, CapeStyles.INFERNO, 0xFFFF5A12, false, true));
        add(new CapeCosmetic("cape_ocean", "Ocean", "Rolling waves and sunlit caustics.", Rarity.EPIC, CapeStyles.OCEAN, 0xFF1FB6D6, false, true));
        add(new CapeCosmetic("cape_rainbow", "Spectrum", "Every colour, always moving.", Rarity.RARE, CapeStyles.RAINBOW, 0xFFFF5470, false, true));
        add(new CapeCosmetic("cape_matrix", "Matrix", "Wake up.", Rarity.EPIC, CapeStyles.MATRIX, 0xFF22FF66, false, true));
        add(new CapeCosmetic("cape_synthwave", "Synthwave", "Retro sun over an endless grid.", Rarity.LEGENDARY, CapeStyles.SYNTHWAVE, 0xFFFF3DD2, false, true));
        add(new CapeCosmetic("cape_void", "Void", "Stare into it.", Rarity.EPIC, CapeStyles.VOID, 0xFFB14DFF, false, true));
        add(new CapeCosmetic("cape_sakura", "Sakura", "Cherry petals on the breeze.", Rarity.RARE, CapeStyles.SAKURA, 0xFFFF7FB0, false, true));
        add(new CapeCosmetic("cape_storm", "Thunderstorm", "Lightning strikes every few seconds.", Rarity.EPIC, CapeStyles.STORM, 0xFF8FB3FF, false, true));
        add(new CapeCosmetic("cape_founder", "Founder", "For the first players. Never sold.", Rarity.MYTHIC, CapeStyles.FOUNDER, 0xFFFFC83A, false, true, Unlock.EXCLUSIVE));
        add(new CapeCosmetic("cape_media", "Media", "Hit record.", Rarity.MYTHIC, CapeStyles.MEDIA, 0xFFFF2D4A, false, true, Unlock.MEDIA));
        add(new CapeCosmetic("cape_onair", "On Air", "The sign is always on.", Rarity.MYTHIC, CapeStyles.ON_AIR, 0xFFFF2D4A, false, true, Unlock.MEDIA));
        add(new CapeCosmetic("cape_premiere", "Premiere", "Opening night, every night.", Rarity.MYTHIC, CapeStyles.PREMIERE, 0xFFFFC83A, false, true, Unlock.MEDIA));
        add(new CapeCosmetic("cape_owner", "Owner", "Only one of these exists.", Rarity.MYTHIC, CapeStyles.OWNER, 0xFF8A5CFF, false, true, Unlock.OWNER));
        add(new CapeCosmetic("cape_gradient", "Custom Gradient", "Your colour, with a moving sheen.", Rarity.COMMON, CapeStyles.GRADIENT, 0xFF8A5CFF, true, true));

        // Wings
        add(new WingsCosmetic("wings_dragon", "Dragon Wings", "Leathery wings with bone fingers.", Rarity.LEGENDARY, WingsCosmetic.Style.DRAGON, 0xFF5A1F8A));
        add(new WingsCosmetic("wings_angel", "Angel Wings", "Soft layered feathers.", Rarity.EPIC, WingsCosmetic.Style.ANGEL, 0xFFF4F7FF));
        add(new WingsCosmetic("wings_demon", "Demon Wings", "Glowing claws and dark membranes.", Rarity.MYTHIC, WingsCosmetic.Style.DEMON, 0xFFFF2A2A));
        add(new WingsCosmetic("wings_butterfly", "Butterfly Wings", "Light and quick.", Rarity.RARE, WingsCosmetic.Style.BUTTERFLY, 0xFF4DA3FF));
        add(new WingsCosmetic("wings_energy", "Energy Wings", "Translucent, glowing, pulsing.", Rarity.MYTHIC, WingsCosmetic.Style.ENERGY, 0xFF2DE2C1));

        // Halos
        add(new HeadCosmetics.Halo("halo_gold", "Golden Halo", Rarity.EPIC, 0xFFFFD34D));
        add(new HeadCosmetics.Halo("halo_aurora", "Frost Halo", Rarity.LEGENDARY, 0xFF2DE2C1));
        add(new HeadCosmetics.Halo("halo_crimson", "Crimson Halo", Rarity.EPIC, 0xFFFF3A5A));

        // Hats
        add(new HeadCosmetics.Crown("hat_crown", "Royal Crown", Rarity.LEGENDARY, 0xFFFFC83A));
        add(new HeadCosmetics.TopHat("hat_tophat", "Top Hat", Rarity.RARE, 0xFFB0202A));
        add(new HeadCosmetics.WitchHat("hat_witch", "Witch Hat", Rarity.EPIC, 0xFF6A2BD9));
        add(new HeadCosmetics.CatEars("hat_catears", "Cat Ears", Rarity.RARE, 0xFF2A2A30));
        add(new HeadCosmetics.Horns("hat_horns", "Demon Horns", Rarity.EPIC, 0xFF3A1A1A));

        // Bandanas
        add(new HeadCosmetics.Bandana("bandana_red", "Red Bandana", Rarity.COMMON, 0xFFD62839));
        add(new HeadCosmetics.Bandana("bandana_black", "Shadow Bandana", Rarity.COMMON, 0xFF1A1C24));
        add(new HeadCosmetics.Bandana("bandana_aurora", "Frost Bandana", Rarity.RARE, 0xFF2DE2C1));

        // Auras
        add(new AuraCosmetic("aura_sparkle", "Sparkles", "Twinkling stars around you.", Rarity.RARE, AuraCosmetic.Style.SPARKLE, 0xFF8AF2FF));
        add(new AuraCosmetic("aura_flame", "Flame Aura", "Embers rising from your feet.", Rarity.EPIC, AuraCosmetic.Style.FLAME, 0xFFFF7A1A));
        add(new AuraCosmetic("aura_snow", "Snowfall", "Your own winter.", Rarity.RARE, AuraCosmetic.Style.SNOW, 0xFFFFFFFF));
        add(new AuraCosmetic("aura_hearts", "Hearts", "Spread the love.", Rarity.RARE, AuraCosmetic.Style.HEARTS, 0xFFFF4F7A));
        add(new AuraCosmetic("aura_orbit", "Orbiting Orbs", "Three comets circling you.", Rarity.LEGENDARY, AuraCosmetic.Style.ORBIT, 0xFF8A5CFF));
    }

    private CosmeticCatalog() {}

    private static void add(Cosmetic c) { ALL.put(c.id, c); }

    private static void add(Cosmetic c, Season season) {
        c.in(season);
        add(c);
    }

    public static Cosmetic get(String id) { return ALL.get(id); }

    public static List<Cosmetic> all() { return Collections.unmodifiableList(new ArrayList<Cosmetic>(ALL.values())); }

    public static List<Cosmetic> ofType(CosmeticType type) {
        List<Cosmetic> l = new ArrayList<Cosmetic>();
        for (Cosmetic c : ALL.values()) if (c.type == type) l.add(c);
        return l;
    }
}
