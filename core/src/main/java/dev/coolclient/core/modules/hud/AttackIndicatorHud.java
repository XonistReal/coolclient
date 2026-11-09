package dev.coolclient.core.modules.hud;

import dev.coolclient.core.api.Feature;
import dev.coolclient.core.api.GuiRenderer;
import dev.coolclient.core.font.Fonts;
import dev.coolclient.core.hud.HudElement;
import dev.coolclient.core.module.Category;
import dev.coolclient.core.render.Colors;
import dev.coolclient.core.render.Draw;
import dev.coolclient.core.setting.BoolSetting;
import dev.coolclient.core.setting.ColorSetting;
import dev.coolclient.core.setting.ModeSetting;
import dev.coolclient.core.util.Time;

/** A restyled attack-cooldown indicator (1.9+): bar, ring or percentage, in any colour. */
public final class AttackIndicatorHud extends HudElement {
    private final ModeSetting style = add(new ModeSetting("Style", "Bar", "Bar", "Ring", "Percent"));
    private final BoolSetting hideReady = add(new BoolSetting("Hide When Charged", true));
    public final BoolSetting hideVanilla = add(new BoolSetting("Hide Vanilla Indicator", true));
    private final ColorSetting charging = add(new ColorSetting("Charging Color", 0xFFFFFFFF));
    private final ColorSetting ready = add(new ColorSetting("Charged Color", 0xFF2DE2C1));

    public AttackIndicatorHud() {
        super("Attack Indicator", "Restyle the attack cooldown indicator.", Category.COMBAT, Feature.ATTACK_COOLDOWN);
        defaultPosition(Anchor.CENTER, 0, 14);
        versionTag("1.21");
    }

    @Override
    public boolean shouldRender() {
        return game().inWorld() && !(hideReady.on() && game().attackCharge() >= 1f);
    }

    @Override
    protected void draw(GuiRenderer r, boolean editing) {
        float charge = editing ? (float) ((Time.seconds() * 0.6) % 1.0) : game().attackCharge();
        int c = charge >= 1f ? ready.color() : charging.color();
        if (style.is("Ring")) {
            size(16, 16);
            Draw.ring(r, 8, 8, 6, 2, 0x50000000);
            Draw.arc(r, 8, 8, 6, 2, 0, 360f * charge, c);
        } else if (style.is("Percent")) {
            String s = Math.round(charge * 100) + "%";
            float w = Fonts.vanilla().width(s);
            size(Math.max(20, w), 9);
            Fonts.vanilla().drawCentered(r, s, Math.max(20, w) / 2f, 0, c, true);
        } else {
            size(24, 4);
            Draw.rounded(r, 0, 0, 24, 4, 2, 0x70000000);
            Draw.rounded(r, 0, 0, Math.max(4, 24 * charge), 4, 2, Colors.withAlpha(c, 255));
        }
    }

    /** Read by the platform to skip vanilla's own indicator. */
    public boolean replacesVanilla() { return isEnabled() && hideVanilla.on(); }
}
