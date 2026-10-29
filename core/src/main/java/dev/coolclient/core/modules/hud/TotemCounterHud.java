package dev.coolclient.core.modules.hud;

import dev.coolclient.core.api.Feature;
import dev.coolclient.core.api.GuiRenderer;
import dev.coolclient.core.font.Fonts;
import dev.coolclient.core.hud.HudElement;
import dev.coolclient.core.render.Draw;
import dev.coolclient.core.setting.BoolSetting;
import dev.coolclient.core.setting.NumberSetting;
import dev.coolclient.core.util.Time;

/** Big totem count for crystal PvP; flashes red when you're out. */
public final class TotemCounterHud extends HudElement {
    private final BoolSetting background = add(new BoolSetting("Background", true));
    private final NumberSetting warnAt = add(new NumberSetting("Warn At", 1, 0, 5, 1));

    public TotemCounterHud() {
        super("Totem Counter", "How many totems you have left.", Feature.TOTEMS);
        versionTag("1.21");
        defaultPosition(Anchor.BOTTOM_CENTER, -110, -22);
        enabledByDefault();
    }

    @Override
    protected void draw(GuiRenderer r, boolean editing) {
        int count = game().inWorld() ? game().countItems("minecraft:totem_of_undying") : 4;
        float w = 38, h = 20;
        size(w, h);
        if (background.on()) {
            int bg = count <= warnAt.i() && (Time.millis() / 400) % 2 == 0 ? 0xA0FF2040 : 0x70000000;
            Draw.rounded(r, 0, 0, w, h, 4, bg);
        }
        r.item(game().itemOf("minecraft:totem_of_undying"), 2, 2, false);
        Fonts.vanilla().draw(r, String.valueOf(count), 21, 6, count == 0 ? 0xFFFF5555 : 0xFFFFFFFF, true);
    }
}
