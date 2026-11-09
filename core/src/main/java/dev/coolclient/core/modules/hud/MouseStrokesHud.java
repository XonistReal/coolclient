package dev.coolclient.core.modules.hud;

import dev.coolclient.core.api.GuiRenderer;
import dev.coolclient.core.hud.HudElement;
import dev.coolclient.core.render.Colors;
import dev.coolclient.core.render.Draw;
import dev.coolclient.core.setting.BoolSetting;
import dev.coolclient.core.setting.ColorSetting;
import dev.coolclient.core.setting.NumberSetting;
import dev.coolclient.core.util.MathUtil;
import dev.coolclient.core.util.Time;

/** A pad with a dot that follows your mouse movement, with a fading trail. */
public final class MouseStrokesHud extends HudElement {
    private static final int TRAIL = 14;

    private final NumberSetting size = add(new NumberSetting("Size", 40, 24, 80, 2));
    private final NumberSetting sensitivity = add(new NumberSetting("Sensitivity", 1, 0.25, 4, 0.25).suffix("x"));
    private final BoolSetting trail = add(new BoolSetting("Trail", true));
    private final ColorSetting dot = add(new ColorSetting("Dot Color", 0xFF2DE2C1));
    private final ColorSetting background = add(new ColorSetting("Background Color", 0x80000000));

    private float lastYaw = Float.NaN, lastPitch;
    private float vx, vy;
    private final float[] tx = new float[TRAIL], ty = new float[TRAIL];
    private int head;

    public MouseStrokesHud() {
        super("Mouse Strokes", "Shows which way your mouse is moving.");
        defaultPosition(Anchor.BOTTOM_LEFT, 4, -100);
    }

    @Override
    protected void draw(GuiRenderer r, boolean editing) {
        float s = size.f();
        size(s, s);
        float dt = Math.max(1e-4f, Time.delta());
        float dx, dy;
        if (game().inWorld() && !editing) {
            float yaw = game().yaw(), pitch = game().pitch();
            if (Float.isNaN(lastYaw)) {
                lastYaw = yaw;
                lastPitch = pitch;
            }
            dx = MathUtil.wrapDegrees(yaw - lastYaw);
            dy = pitch - lastPitch;
            lastYaw = yaw;
            lastPitch = pitch;
        } else {
            // Editor preview: a slow circle.
            double t = Time.seconds() * 2;
            dx = (float) Math.cos(t) * 60f * dt;
            dy = (float) Math.sin(t) * 60f * dt;
        }
        // Degrees per second, smoothed, mapped onto the pad.
        float k = Math.min(1f, dt * 14f);
        vx += (dx / dt - vx) * k;
        vy += (dy / dt - vy) * k;
        float half = s / 2f - 4;
        float px = MathUtil.clamp(vx * 0.05f * sensitivity.f(), -half, half);
        float py = MathUtil.clamp(vy * 0.05f * sensitivity.f(), -half, half);

        Draw.rounded(r, 0, 0, s, s, 4, background.color());
        int line = 0x30FFFFFF;
        Draw.rect(r, s / 2f - 0.5f, 3, 1, s - 6, line);
        Draw.rect(r, 3, s / 2f - 0.5f, s - 6, 1, line);
        head = (head + 1) % TRAIL;
        tx[head] = px;
        ty[head] = py;
        if (trail.on()) {
            for (int i = 1; i < TRAIL; i++) {
                int idx = (head - i + TRAIL) % TRAIL;
                float a = 1f - i / (float) TRAIL;
                Draw.circle(r, s / 2f + tx[idx], s / 2f + ty[idx], 2.2f * a + 0.5f, Colors.fade(dot.color(), a * 0.5f));
            }
        }
        Draw.circle(r, s / 2f + px, s / 2f + py, 3f, dot.color());
    }
}
