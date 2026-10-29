package dev.coolclient.core.hud;

import dev.coolclient.core.api.Feature;
import dev.coolclient.core.api.GuiRenderer;
import dev.coolclient.core.font.FontRenderer;
import dev.coolclient.core.setting.BoolSetting;
import dev.coolclient.core.setting.ModeSetting;
import dev.coolclient.core.setting.Setting;

/** A single line of "label: value" text in a styled box (FPS, CPS, Ping ...). */
public abstract class TextElement extends HudElement {
    public final HudStyle style;
    protected final ModeSetting labelMode = add(new ModeSetting("Label", "Suffix", "Suffix", "Prefix", "None"));
    protected final BoolSetting brackets = add(new BoolSetting("Brackets", false));

    protected TextElement(String name, String description, Feature... requires) {
        super(name, description, requires);
        style = new HudStyle(this, new HudStyle.Adder() {
            @Override
            public <S extends Setting<?>> S add(S setting) {
                return TextElement.this.add(setting);
            }
        }, true);
    }

    /** Short label, e.g. "FPS". */
    protected abstract String label();

    /** Current value, e.g. "144". */
    protected abstract String value();

    /** Label used in "Suffix" mode (e.g. "ms" instead of "Ping"). */
    protected String suffixLabel() { return label(); }

    /** Value used while editing the HUD with no world loaded. */
    protected String previewValue() { return value(); }

    protected String compose(boolean editing) {
        String v = editing && !game().inWorld() ? previewValue() : value();
        if (editing && (v == null || v.isEmpty())) v = previewValue();
        String text;
        if (labelMode.is("Prefix")) text = label() + ": " + v;
        else if (labelMode.is("Suffix")) text = v + " " + suffixLabel();
        else text = v;
        return brackets.on() ? "[" + text + "]" : text;
    }

    @Override
    protected void draw(GuiRenderer r, boolean editing) {
        FontRenderer f = style.fontRenderer();
        String text = compose(editing);
        float tw = f.width(text);
        if (style.background.on()) {
            float w = Math.max(56, tw + 10), h = 16;
            size(w, h);
            style.drawBox(r, w, h);
            f.draw(r, text, (w - tw) / 2f, (h - f.height()) / 2f + (f.height() >= 9 ? 0.5f : 0), style.text(0), style.shadow.on());
        } else {
            size(tw, f.height());
            f.draw(r, text, 0, 0, style.text(0), style.shadow.on());
        }
    }
}
