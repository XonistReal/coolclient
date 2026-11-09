package dev.coolclient.core.hud;

import dev.coolclient.core.api.Feature;
import dev.coolclient.core.api.GuiRenderer;
import dev.coolclient.core.font.FontRenderer;
import dev.coolclient.core.module.Category;
import dev.coolclient.core.setting.ModeSetting;
import dev.coolclient.core.setting.Setting;

import java.util.List;

/** A few lines of text in one styled box (System Info, Horse Stats ...). */
public abstract class LinesElement extends HudElement {
    public final HudStyle style;
    protected final ModeSetting align = add(new ModeSetting("Align", "Left", "Left", "Center", "Right"));

    protected LinesElement(String name, String description, Feature... requires) {
        this(name, description, Category.HUD, requires);
    }

    protected LinesElement(String name, String description, Category category, Feature... requires) {
        super(name, description, category, requires);
        style = new HudStyle(this, new HudStyle.Adder() {
            @Override
            public <S extends Setting<?>> S add(S setting) {
                return LinesElement.this.add(setting);
            }
        }, true);
    }

    /** The lines to show right now ({@code editing}: the HUD editor is open, so show sample values if needed). */
    protected abstract List<String> lines(boolean editing);

    @Override
    protected void draw(GuiRenderer r, boolean editing) {
        FontRenderer f = style.fontRenderer();
        List<String> lines = lines(editing);
        if (lines.isEmpty()) {
            size(40, 12);
            return;
        }
        float tw = 0;
        for (String l : lines) tw = Math.max(tw, f.width(l));
        float lh = f.height() + 2;
        float pad = style.background.on() ? 5 : 0;
        float w = tw + pad * 2, h = lines.size() * lh - 2 + pad * 2;
        size(w, h);
        style.drawBox(r, w, h);
        float y = pad + (style.background.on() && f.height() >= 9 ? 0.5f : 0);
        for (int i = 0; i < lines.size(); i++) {
            String l = lines.get(i);
            float lw = f.width(l);
            float x = align.is("Center") ? (w - lw) / 2f : align.is("Right") ? w - pad - lw : pad;
            f.draw(r, l, x, y, style.text(i * 0.12f), style.shadow.on());
            y += lh;
        }
    }
}
