package dev.coolclient.core.modules.hud;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.api.GuiRenderer;
import dev.coolclient.core.event.Events;
import dev.coolclient.core.font.Fonts;
import dev.coolclient.core.hud.HudElement;
import dev.coolclient.core.render.Colors;
import dev.coolclient.core.render.Draw;
import dev.coolclient.core.setting.BoolSetting;
import dev.coolclient.core.setting.ColorSetting;

/** Rolling 6-second graph of left/right CPS. */
public final class CpsGraphHud extends HudElement {
    private final BoolSetting showRight = add(new BoolSetting("Show Right Click", true));
    private final ColorSetting leftColor = add(new ColorSetting("Left Color", 0xFF2DE2C1));
    private final ColorSetting rightColor = add(new ColorSetting("Right Color", 0xFF8A5CFF));
    private final int[] left = new int[120];
    private final int[] right = new int[120];
    private int head;

    public CpsGraphHud() {
        super("CPS Graph", "A live graph of your clicking.");
        defaultPosition(Anchor.BOTTOM_LEFT, 4, -4);
        on(Events.Tick.class, e -> {
            head = (head + 1) % left.length;
            left[head] = CoolClient.get().leftCps().cps();
            right[head] = CoolClient.get().rightCps().cps();
        });
    }

    @Override
    protected void draw(GuiRenderer r, boolean editing) {
        float w = 120, h = 40;
        size(w, h);
        Draw.rounded(r, 0, 0, w, h, 3, 0x70000000);
        int max = 10;
        for (int i = 0; i < left.length; i++) max = Math.max(max, Math.max(left[i], right[i]));
        for (int g = 5; g < max; g += 5) {
            float gy = h - 3 - (h - 12) * g / (float) max;
            Draw.rect(r, 2, gy, w - 4, 0.5f, 0x22FFFFFF);
        }
        if (showRight.on()) line(r, right, max, w, h, rightColor.color());
        line(r, left, max, w, h, leftColor.color());
        Fonts.small().draw(r, CoolClient.get().leftCps().cps() + (showRight.on() ? " | " + CoolClient.get().rightCps().cps() : "") + " CPS",
                4, 3, 0xFFFFFFFF, false);
    }

    private void line(GuiRenderer r, int[] data, int max, float w, float h, int color) {
        float[] pts = new float[data.length * 2];
        for (int i = 0; i < data.length; i++) {
            int idx = (head + 1 + i) % data.length;
            pts[i * 2] = 2 + (w - 4) * i / (data.length - 1f);
            pts[i * 2 + 1] = h - 3 - (h - 12) * data[idx] / (float) max;
        }
        for (int i = 0; i + 3 < pts.length; i += 2) {
            Draw.line(r, pts[i], pts[i + 1], pts[i + 2], pts[i + 3], 1f, color);
        }
        int last = data[head];
        Draw.circle(r, pts[pts.length - 2], pts[pts.length - 1], 1.5f, Colors.withAlpha(color, last > 0 ? 255 : 120));
    }
}
