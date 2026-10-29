package dev.coolclient.core.modules.hud;

import dev.coolclient.core.api.GuiRenderer;
import dev.coolclient.core.api.ItemRef;
import dev.coolclient.core.font.FontRenderer;
import dev.coolclient.core.font.Fonts;
import dev.coolclient.core.hud.HudElement;
import dev.coolclient.core.render.Colors;
import dev.coolclient.core.render.Draw;
import dev.coolclient.core.setting.BoolSetting;
import dev.coolclient.core.setting.ColorSetting;
import dev.coolclient.core.setting.ModeSetting;

import java.util.ArrayList;
import java.util.List;

public final class ArmorStatusHud extends HudElement {
    private final ModeSetting display = add(new ModeSetting("Durability", "Value", "Value", "Percent", "Bar", "None"));
    private final ModeSetting layout = add(new ModeSetting("Layout", "Vertical", "Vertical", "Horizontal"));
    private final BoolSetting heldItem = add(new BoolSetting("Show Held Item", true));
    private final BoolSetting colorByDamage = add(new BoolSetting("Color By Durability", true));
    private final ColorSetting textColor = add(new ColorSetting("Text Color", 0xFFFFFFFF));
    private final BoolSetting shadow = add(new BoolSetting("Text Shadow", true));
    private final BoolSetting background = add(new BoolSetting("Background", false));

    private static final String[] PREVIEW = {"minecraft:diamond_helmet", "minecraft:diamond_chestplate", "minecraft:diamond_leggings", "minecraft:diamond_boots"};

    public ArmorStatusHud() {
        super("Armor Status", "Your armor and held item with durability.");
        defaultPosition(Anchor.MIDDLE_RIGHT, -4, 16);
        enabledByDefault();
    }

    @Override
    protected void draw(GuiRenderer r, boolean editing) {
        List<ItemRef> items = new ArrayList<ItemRef>();
        boolean live = game().inWorld();
        boolean anyArmor = false;
        if (live) for (int slot = 0; slot < 4; slot++) anyArmor |= !game().armor(slot).isEmpty();
        // In the editor with no armour on, show sample gear so the element can be placed.
        boolean preview = !live || (editing && !anyArmor);
        for (int slot = 3; slot >= 0; slot--) {
            ItemRef it = preview ? game().itemOf(PREVIEW[3 - slot]) : game().armor(slot);
            if (!it.isEmpty() || editing) items.add(it);
        }
        if (heldItem.on()) {
            ItemRef held = preview ? game().itemOf("minecraft:diamond_sword") : game().mainHand();
            if (!held.isEmpty()) items.add(held);
        }
        FontRenderer f = Fonts.vanilla();
        boolean horizontal = layout.is("Horizontal");
        float x = 0, y = 0, maxW = 0;
        float rowH = 17;
        if (background.on()) {
            float w = horizontal ? items.size() * 38 : 58, h = horizontal ? 20 : items.size() * rowH + 2;
            Draw.rounded(r, -2, -2, w + 2, h + 2, 3, 0x70000000);
        }
        for (ItemRef it : items) {
            if (it.isEmpty()) {
                if (horizontal) x += 38; else y += rowH;
                continue;
            }
            r.item(it, x, y, false);
            String text = durabilityText(it);
            int color = textColor.color();
            if (colorByDamage.on() && it.isDamageable()) {
                float frac = it.durabilityFraction();
                color = frac > 0.6f ? Colors.lerp(0xFFFFFF55, 0xFF55FF55, (frac - 0.6f) / 0.4f)
                        : (frac > 0.25f ? Colors.lerp(0xFFFF5555, 0xFFFFFF55, (frac - 0.25f) / 0.35f) : 0xFFFF5555);
            }
            float tw = 0;
            if (display.is("Bar") && it.isDamageable()) {
                Draw.rect(r, x + 18, y + 7, 22, 3, 0xFF000000);
                Draw.rect(r, x + 18, y + 7, 22 * it.durabilityFraction(), 2, color);
                tw = 24;
            } else if (!text.isEmpty()) {
                tw = f.draw(r, text, x + 18, y + 4.5f, color, shadow.on()) + 2;
            }
            maxW = Math.max(maxW, 18 + tw);
            if (horizontal) x += 18 + Math.max(tw, 20); else y += rowH;
        }
        if (horizontal) size(Math.max(x, 16), 16);
        else size(Math.max(maxW, 16), Math.max(y - 1, 16));
    }

    private String durabilityText(ItemRef it) {
        if (display.is("None") || display.is("Bar")) return it.count() > 1 ? String.valueOf(it.count()) : "";
        if (!it.isDamageable()) return it.count() > 1 ? String.valueOf(it.count()) : "";
        if (display.is("Percent")) return Math.round(it.durabilityFraction() * 100) + "%";
        return String.valueOf(it.durability());
    }
}
