package dev.coolclient.core.modules.hud;

import dev.coolclient.core.api.Feature;
import dev.coolclient.core.api.GuiRenderer;
import dev.coolclient.core.hud.HudElement;
import dev.coolclient.core.render.Colors;
import dev.coolclient.core.render.Draw;
import dev.coolclient.core.render.Theme;
import dev.coolclient.core.setting.BoolSetting;

import java.util.ArrayList;
import java.util.List;

/** Item cooldown bars (pearls, chorus, wind charges, shields ...). */
public final class CooldownsHud extends HudElement {
    private static final String[] ITEMS = {
            "minecraft:ender_pearl", "minecraft:chorus_fruit", "minecraft:wind_charge",
            "minecraft:shield", "minecraft:goat_horn", "minecraft:mace"
    };
    private final BoolSetting background = add(new BoolSetting("Background", true));

    public CooldownsHud() {
        super("Cooldowns", "Shows item cooldowns as bars.", Feature.COOLDOWNS);
        versionTag("1.21");
        defaultPosition(Anchor.BOTTOM_CENTER, 0, -56);
    }

    @Override
    protected void draw(GuiRenderer r, boolean editing) {
        List<Object[]> active = new ArrayList<Object[]>();
        for (String id : ITEMS) {
            float cd = game().inWorld() ? game().cooldown(id) : 0f;
            if (cd > 0f) active.add(new Object[]{id, cd});
        }
        if (active.isEmpty() && editing) {
            active.add(new Object[]{"minecraft:ender_pearl", 0.6f});
            active.add(new Object[]{"minecraft:shield", 0.3f});
        }
        float w = 80, rowH = 18;
        size(w, Math.max(rowH, active.size() * rowH));
        float y = 0;
        for (Object[] a : active) {
            float cd = (Float) a[1];
            if (background.on()) Draw.rounded(r, 0, y, w, rowH - 2, 3, 0x70000000);
            r.item(game().itemOf((String) a[0]), 1, y, false);
            Draw.rounded(r, 20, y + 6, w - 24, 4, 2, 0x50FFFFFF);
            Draw.rounded(r, 20, y + 6, (w - 24) * cd, 4, 2, Colors.lerp(Theme.accent(0), 0xFFFF5470, cd));
            y += rowH;
        }
    }

    @Override
    public boolean shouldRender() {
        for (String id : ITEMS) if (game().cooldown(id) > 0f) return true;
        return false;
    }
}
