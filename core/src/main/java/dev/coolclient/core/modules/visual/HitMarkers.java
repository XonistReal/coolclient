package dev.coolclient.core.modules.visual;

import dev.coolclient.core.api.GuiRenderer;
import dev.coolclient.core.event.Events;
import dev.coolclient.core.module.Category;
import dev.coolclient.core.module.Module;
import dev.coolclient.core.render.Colors;
import dev.coolclient.core.render.Draw;
import dev.coolclient.core.setting.BoolSetting;
import dev.coolclient.core.setting.ColorSetting;
import dev.coolclient.core.setting.NumberSetting;
import dev.coolclient.core.util.Time;

/** A small X flashes around the crosshair when one of your hits lands. */
public final class HitMarkers extends Module {
    private final ColorSetting color = add(new ColorSetting("Color", 0xFFFFFFFF));
    private final NumberSetting size = add(new NumberSetting("Size", 5, 2, 12, 0.5));
    private final NumberSetting thickness = add(new NumberSetting("Thickness", 1.5, 0.5, 4, 0.5));
    private final NumberSetting duration = add(new NumberSetting("Duration", 250, 100, 1000, 50).suffix("ms"));
    private final BoolSetting playersOnly = add(new BoolSetting("Players Only", false));
    private long hitAt = -10_000;

    public HitMarkers() {
        super("Hit Markers", "A marker flashes on your crosshair when you hit something.", Category.COMBAT);
        on(Events.Attack.class, e -> {
            if (!playersOnly.on() || e.targetIsPlayer) hitAt = Time.millis();
        });
        on(Events.RenderHud.class, e -> render(e.renderer));
    }

    private void render(GuiRenderer r) {
        float t = (Time.millis() - hitAt) / duration.f();
        if (t >= 1f || t < 0f || platform().anyScreenOpen()) return;
        float a = 1f - t * t;
        float cx = r.width() / 2f, cy = r.height() / 2f;
        float gap = 3f + t * 1.5f, len = size.f();
        int c = Colors.fade(color.color(), a);
        float w = thickness.f();
        for (int sx = -1; sx <= 1; sx += 2)
            for (int sy = -1; sy <= 1; sy += 2) {
                float k = 0.7071f;
                Draw.line(r, cx + sx * gap * k, cy + sy * gap * k, cx + sx * (gap + len) * k, cy + sy * (gap + len) * k, w, c);
            }
    }
}
