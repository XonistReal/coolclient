package dev.coolclient.core.modules.hud;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.api.Feature;
import dev.coolclient.core.api.GuiRenderer;
import dev.coolclient.core.api.ItemRef;
import dev.coolclient.core.font.Fonts;
import dev.coolclient.core.hud.HudElement;
import dev.coolclient.core.render.Draw;
import dev.coolclient.core.setting.BoolSetting;
import dev.coolclient.core.setting.ModeSetting;

import java.util.ArrayList;
import java.util.List;

/** Counts PvP consumables in your inventory. Crystal-PvP items appear on 1.9+. */
public final class ItemCounterHud extends HudElement {
    private static final class Entry {
        final BoolSetting toggle;
        final String icon;
        final String[] ids;

        Entry(BoolSetting toggle, String icon, String... ids) {
            this.toggle = toggle;
            this.icon = icon;
            this.ids = ids;
        }
    }

    private final ModeSetting layout = add(new ModeSetting("Layout", "Vertical", "Vertical", "Horizontal"));
    private final BoolSetting hideZero = add(new BoolSetting("Hide Empty", false));
    private final BoolSetting background = add(new BoolSetting("Background", false));
    private final List<Entry> entries = new ArrayList<Entry>();

    public ItemCounterHud() {
        super("Item Counter", "Counts arrows, pots, pearls, gapples and more.");
        defaultPosition(Anchor.BOTTOM_RIGHT, -4, -40);
        boolean modern = CoolClient.get().platform().supports(Feature.TOTEMS);
        entries.add(new Entry(add(new BoolSetting("Arrows", true)), "minecraft:arrow", "minecraft:arrow", "minecraft:spectral_arrow", "minecraft:tipped_arrow"));
        entries.add(new Entry(add(new BoolSetting("Potions", true)), modern ? "minecraft:splash_potion" : "minecraft:potion",
                "minecraft:potion", "minecraft:splash_potion", "minecraft:lingering_potion"));
        entries.add(new Entry(add(new BoolSetting("Golden Apples", true)), "minecraft:golden_apple", "minecraft:golden_apple", "minecraft:enchanted_golden_apple"));
        entries.add(new Entry(add(new BoolSetting("Ender Pearls", true)), "minecraft:ender_pearl", "minecraft:ender_pearl"));
        if (modern) {
            entries.add(new Entry(add(new BoolSetting("Totems", true)), "minecraft:totem_of_undying", "minecraft:totem_of_undying"));
            entries.add(new Entry(add(new BoolSetting("End Crystals", true)), "minecraft:end_crystal", "minecraft:end_crystal"));
            entries.add(new Entry(add(new BoolSetting("Obsidian", true)), "minecraft:obsidian", "minecraft:obsidian"));
            entries.add(new Entry(add(new BoolSetting("Respawn Anchors", false)), "minecraft:respawn_anchor", "minecraft:respawn_anchor"));
            entries.add(new Entry(add(new BoolSetting("Glowstone", false)), "minecraft:glowstone", "minecraft:glowstone"));
            entries.add(new Entry(add(new BoolSetting("XP Bottles", true)), "minecraft:experience_bottle", "minecraft:experience_bottle"));
            entries.add(new Entry(add(new BoolSetting("Wind Charges", false)), "minecraft:wind_charge", "minecraft:wind_charge"));
        } else {
            entries.add(new Entry(add(new BoolSetting("Cobwebs", false)), "minecraft:web", "minecraft:web"));
        }
    }

    @Override
    protected void draw(GuiRenderer r, boolean editing) {
        boolean horizontal = layout.is("Horizontal");
        List<Object[]> rows = new ArrayList<Object[]>();
        for (Entry e : entries) {
            if (!e.toggle.on()) continue;
            int count = game().inWorld() ? game().countItems(e.ids) : 16;
            if (count == 0 && hideZero.on() && !editing) continue;
            rows.add(new Object[]{game().itemOf(e.icon), count});
        }
        float w = 0, h = 0;
        float x = 0, y = 0;
        if (background.on() && !rows.isEmpty()) {
            float bw = horizontal ? rows.size() * 36 : 44, bh = horizontal ? 18 : rows.size() * 18;
            Draw.rounded(r, -2, -2, bw + 2, bh + 2, 3, 0x70000000);
        }
        for (Object[] row : rows) {
            ItemRef icon = (ItemRef) row[0];
            int count = (Integer) row[1];
            r.item(icon, x, y, false);
            float tw = Fonts.vanilla().draw(r, String.valueOf(count), x + 18, y + 4.5f, count == 0 ? 0xFFFF5555 : 0xFFFFFFFF, true);
            if (horizontal) {
                x += 18 + tw + 6;
                w = x;
                h = 16;
            } else {
                w = Math.max(w, 18 + tw);
                y += 18;
                h = y - 2;
            }
        }
        size(Math.max(w, 16), Math.max(h, 16));
    }
}
