package dev.coolclient.core.modules.hud;

import dev.coolclient.core.api.EffectInfo;
import dev.coolclient.core.api.GuiRenderer;
import dev.coolclient.core.font.FontRenderer;
import dev.coolclient.core.hud.HudElement;
import dev.coolclient.core.hud.HudStyle;
import dev.coolclient.core.setting.BoolSetting;
import dev.coolclient.core.setting.ColorSetting;
import dev.coolclient.core.setting.Setting;
import dev.coolclient.core.util.Time;

import java.util.ArrayList;
import java.util.List;

public final class PotionEffectsHud extends HudElement {
    private final BoolSetting icons = add(new BoolSetting("Show Icons", true));
    private final BoolSetting names = add(new BoolSetting("Show Names", true));
    private final BoolSetting blink = add(new BoolSetting("Blink When Expiring", true));
    private final BoolSetting colorByType = add(new BoolSetting("Color By Type", false));
    private final ColorSetting durationColor = add(new ColorSetting("Duration Color", 0xFFAAAAAA));
    private final HudStyle style;

    public PotionEffectsHud() {
        super("Potion Effects", "Active effects with time remaining.");
        defaultPosition(Anchor.MIDDLE_LEFT, 4, 18);
        enabledByDefault();
        style = new HudStyle(this, new HudStyle.Adder() {
            @Override
            public <S extends Setting<?>> S add(S setting) {
                return PotionEffectsHud.this.add(setting);
            }
        }, false);
    }

    @Override
    protected void draw(GuiRenderer r, boolean editing) {
        List<EffectInfo> effects = game().inWorld() ? game().effects() : new ArrayList<EffectInfo>();
        if (effects.isEmpty() && editing) {
            effects = new ArrayList<EffectInfo>();
            effects.add(new EffectInfo("minecraft:speed", "Speed", 1, 20 * 95, true, null));
            effects.add(new EffectInfo("minecraft:strength", "Strength", 0, 20 * 8, true, null));
        }
        FontRenderer f = style.fontRenderer();
        float pad = style.background.on() ? 4 : 0;
        float rowH = Math.max(icons.on() ? 18 : 0, names.on() ? f.height() * 2 + 2 : f.height() + 2);
        float w = 0;
        for (EffectInfo e : effects) {
            float tw = Math.max(names.on() ? f.width(e.name + e.levelText()) : 0, f.width(e.durationText()));
            w = Math.max(w, (icons.on() ? 20 : 0) + tw);
        }
        float h = effects.size() * rowH;
        size(Math.max(w + pad * 2, 20), Math.max(h + pad * 2, 10));
        style.drawBox(r, width, height);
        float y = pad;
        for (EffectInfo e : effects) {
            float x = pad;
            boolean expiring = !e.infinite() && e.durationTicks < 200;
            float alpha = blink.on() && expiring ? (0.45f + 0.55f * (float) Math.abs(Math.sin(Time.seconds() * 4))) : 1f;
            if (icons.on()) {
                if (e.handle != null) r.effectIcon(e, x, y, 18);
                x += 20;
            }
            int nameColor = colorByType.on() ? (e.beneficial ? 0xFF7CFFB0 : 0xFFFF6B6B) : style.text(y / 50f);
            float ty = y + (rowH - (names.on() ? f.height() * 2 + 1 : f.height())) / 2f;
            if (names.on()) {
                f.draw(r, e.name + e.levelText(), x, ty, dev.coolclient.core.render.Colors.fade(nameColor, alpha), style.shadow.on());
                ty += f.height() + 1;
            }
            f.draw(r, e.durationText(), x, ty, dev.coolclient.core.render.Colors.fade(durationColor.color(), alpha), style.shadow.on());
            y += rowH;
        }
    }
}
