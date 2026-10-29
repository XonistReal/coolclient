package dev.coolclient.core.modules.hud;

import dev.coolclient.core.api.GuiRenderer;
import dev.coolclient.core.font.FontRenderer;
import dev.coolclient.core.hud.HudElement;
import dev.coolclient.core.hud.HudStyle;
import dev.coolclient.core.render.Colors;
import dev.coolclient.core.render.Draw;
import dev.coolclient.core.render.Scissor;
import dev.coolclient.core.setting.BoolSetting;
import dev.coolclient.core.setting.ColorSetting;
import dev.coolclient.core.setting.NumberSetting;
import dev.coolclient.core.setting.Setting;

/** Scrolling compass strip. */
public final class DirectionHud extends HudElement {
    private final NumberSetting width = add(new NumberSetting("Width", 140, 80, 260, 5));
    private final BoolSetting degrees = add(new BoolSetting("Show Degrees", true));
    private final ColorSetting marker = add(new ColorSetting("Marker Color", 0xFF2DE2C1));
    private final HudStyle style;
    private static final String[] LABELS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

    public DirectionHud() {
        super("Direction", "A compass strip showing your heading.");
        defaultPosition(Anchor.TOP_CENTER, 0, 4);
        style = new HudStyle(this, new HudStyle.Adder() {
            @Override
            public <S extends Setting<?>> S add(S setting) {
                return DirectionHud.this.add(setting);
            }
        }, true);
    }

    @Override
    protected void draw(GuiRenderer r, boolean editing) {
        FontRenderer f = style.fontRenderer();
        float w = width.f(), h = degrees.on() ? 24 : 16;
        size(w, h);
        style.drawBox(r, w, h);
        float yaw = game().inWorld() ? game().yaw() : 200f;
        float heading = ((yaw + 180f) % 360f + 360f) % 360f;
        float pxPerDeg = 1.2f;
        r.push();
        Scissor.push(r, 0, 0, w, h);
        float cx = w / 2f;
        for (int deg = 0; deg < 360; deg += 15) {
            float diff = deg - heading;
            if (diff > 180) diff -= 360;
            if (diff < -180) diff += 360;
            float x = cx + diff * pxPerDeg;
            if (x < -20 || x > w + 20) continue;
            float edge = 1f - Math.min(1f, Math.abs(diff) * pxPerDeg / (w / 2f));
            int col = Colors.fade(style.text(0), 0.25f + 0.75f * edge);
            if (deg % 45 == 0) {
                String label = LABELS[deg / 45];
                f.drawCentered(r, label, x, 3, deg == 0 ? Colors.fade(0xFFFF5470, 0.25f + 0.75f * edge) : col, style.shadow.on());
            } else {
                Draw.rect(r, x - 0.5f, 5, 1, 4, Colors.fade(col, 0.6f));
            }
        }
        Scissor.pop(r);
        r.pop();
        Draw.triangle(r, cx - 3, h, cx + 3, h, cx, h - 4, marker.color());
        if (degrees.on()) f.drawCentered(r, String.valueOf(Math.round(heading) % 360), cx, 13, style.dim(), style.shadow.on());
    }
}
