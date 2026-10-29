package dev.coolclient.core.modules.hud;

import dev.coolclient.core.event.Events;
import dev.coolclient.core.hud.TextElement;
import dev.coolclient.core.setting.BoolSetting;
import dev.coolclient.core.setting.ModeSetting;
import dev.coolclient.core.util.MathUtil;

public final class SpeedHud extends TextElement {
    private final ModeSetting unit = add(new ModeSetting("Unit", "Blocks/s", "Blocks/s", "km/h"));
    private final BoolSetting vertical = add(new BoolSetting("Include Vertical", false));
    private double px, py, pz, speed;
    private boolean has;

    public SpeedHud() {
        super("Speed", "Your movement speed.");
        defaultPosition(Anchor.TOP_LEFT, 4, 166);
        on(Events.Tick.class, e -> {
            if (!game().inWorld()) {
                has = false;
                return;
            }
            double x = game().x(), y = game().y(), z = game().z();
            if (has) {
                double dx = x - px, dy = vertical.on() ? y - py : 0, dz = z - pz;
                double s = Math.sqrt(dx * dx + dy * dy + dz * dz) * 20.0;
                speed = speed * 0.6 + s * 0.4;
            }
            px = x;
            py = y;
            pz = z;
            has = true;
        });
    }

    @Override protected String label() { return "Speed"; }

    @Override protected String suffixLabel() { return unit.is("km/h") ? "km/h" : "b/s"; }

    @Override
    protected String value() {
        return MathUtil.format(unit.is("km/h") ? speed * 3.6 : speed, 2);
    }

    @Override protected String previewValue() { return "5.61"; }
}
