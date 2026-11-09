package dev.coolclient.core.modules.hud;

import dev.coolclient.core.api.ItemRef;
import dev.coolclient.core.hud.LinesElement;
import dev.coolclient.core.setting.BoolSetting;
import dev.coolclient.core.setting.NumberSetting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Enchantments and durability of the item in your hand, above the hotbar. */
public final class HeldItemInfoHud extends LinesElement {
    private final BoolSetting name = add(new BoolSetting("Item Name", false));
    private final BoolSetting enchants = add(new BoolSetting("Enchantments", true));
    private final NumberSetting maxEnchants = add(new NumberSetting("Max Enchantment Lines", 4, 1, 10, 1).visibleWhen(enchants::on));
    private final BoolSetting durability = add(new BoolSetting("Durability", true));

    public HeldItemInfoHud() {
        super("Held Item Info", "Enchantments and durability of what you're holding.");
        defaultPosition(Anchor.BOTTOM_CENTER, 0, -62);
        align.set("Center");
        style.background.set(false);
    }

    @Override
    public boolean shouldRender() { return game().inWorld() && !game().mainHand().isEmpty(); }

    @Override
    protected List<String> lines(boolean editing) {
        ItemRef held = game().inWorld() ? game().mainHand() : ItemRef.EMPTY;
        List<String> out = new ArrayList<String>();
        List<String> ench;
        String title;
        int dur = -1, max = 0;
        if (held.isEmpty()) {
            if (!editing) return out;
            title = "Diamond Sword";
            ench = Arrays.asList("Sharpness V", "Unbreaking III", "Looting III");
            dur = 1402;
            max = 1561;
        } else {
            title = held.displayName();
            ench = held.enchantments();
            if (held.isDamageable()) {
                dur = held.durability();
                max = held.maxDamage();
            }
        }
        if (name.on()) out.add(title);
        if (enchants.on()) {
            int n = Math.min(ench.size(), maxEnchants.i());
            for (int i = 0; i < n; i++) out.add("§d" + ench.get(i));
            if (ench.size() > n) out.add("§7+" + (ench.size() - n) + " more");
        }
        if (durability.on() && dur >= 0) {
            float f = max <= 0 ? 1f : dur / (float) max;
            String c = f > 0.5f ? "§a" : f > 0.2f ? "§e" : "§c";
            out.add(c + dur + "§7 / " + max);
        }
        return out;
    }
}
