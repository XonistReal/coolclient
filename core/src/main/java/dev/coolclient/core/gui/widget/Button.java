package dev.coolclient.core.gui.widget;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.api.GuiRenderer;
import dev.coolclient.core.font.FontRenderer;
import dev.coolclient.core.font.Fonts;
import dev.coolclient.core.render.Colors;
import dev.coolclient.core.render.Draw;
import dev.coolclient.core.render.Icons;
import dev.coolclient.core.render.Theme;
import dev.coolclient.core.util.Time;

/** Rounded button with optional icon; "primary" buttons use the accent gradient. */
public class Button extends Widget {
    private String label;
    private Icons.Icon icon;
    private Runnable action;
    private boolean primary;
    private boolean danger;
    private float radius = 4f;
    private float iconSize = 9f;
    private FontRenderer font = Fonts.body();

    public Button(String label, Runnable action) {
        this.label = label;
        this.action = action;
    }

    public Button icon(Icons.Icon icon) { this.icon = icon; return this; }

    public Button primary() { this.primary = true; return this; }

    public Button danger() { this.danger = true; return this; }

    public Button radius(float r) { this.radius = r; return this; }

    /** Largest icon size; the icon still shrinks to fit short buttons. */
    public Button iconSize(float s) { this.iconSize = s; return this; }

    public Button font(FontRenderer f) { this.font = f; return this; }

    public void setLabel(String label) { this.label = label; }

    public String label() { return label; }

    @Override
    protected void draw(GuiRenderer r, float mx, float my) {
        float hv = hover.get();
        if (primary) {
            Draw.glow(r, x, y, w, h, radius, 6 * hv, Colors.withAlpha(Theme.accent(0.5f), 70));
            // A highlight sweeps across primary buttons every few seconds.
            float cycle = (float) ((Time.seconds() + x * 0.01) % 4.5);
            Draw.shine(r, x, y, w, h, radius, Theme.accent(0f), Theme.accent(1f), cycle < 1.2f ? cycle / 1.2f : -1f);
            if (hv > 0) Draw.rounded(r, x, y, w, h, radius, Colors.withAlpha(0xFFFFFF, (int) (28 * hv)));
        } else {
            int base = danger ? Colors.lerp(Theme.SURFACE, Theme.DANGER, 0.25f + 0.35f * hv)
                    : Colors.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, hv);
            Draw.rounded(r, x, y, w, h, radius, base);
            Draw.roundedOutline(r, x, y, w, h, radius, 0.75f, Colors.lerp(Theme.BORDER, Theme.BORDER_STRONG, hv));
        }
        int tc = primary ? Theme.ON_ACCENT : Theme.TEXT;
        float textW = label.isEmpty() ? 0 : font.width(label);
        float iconS = Math.min(h - 6, iconSize);
        float total = textW + (icon != null ? iconS + (textW > 0 ? 4 : 0) : 0);
        float sx = x + (w - total) / 2f;
        if (icon != null) {
            Icons.draw(r, icon, sx, y + (h - iconS) / 2f, iconS, tc);
            sx += iconS + 4;
        }
        if (!label.isEmpty()) font.draw(r, label, sx, y + (h - font.height()) / 2f, tc, false);
    }

    @Override
    public boolean mouseClicked(float mx, float my, int button) {
        if (button == 0 && isHovered(mx, my)) {
            CoolClient.get().platform().playClickSound();
            if (action != null) action.run();
            return true;
        }
        return false;
    }
}
