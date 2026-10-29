package dev.coolclient.core.modules.hud;

import dev.coolclient.core.event.Events;
import dev.coolclient.core.hud.TextElement;
import dev.coolclient.core.setting.NumberSetting;
import dev.coolclient.core.util.MathUtil;
import dev.coolclient.core.util.Time;

public final class ReachDisplayHud extends TextElement {
    private final NumberSetting decimals = add(new NumberSetting("Decimals", 2, 0, 3, 1));
    private final NumberSetting resetAfter = add(new NumberSetting("Reset After", 3, 1, 10, 1).suffix("s"));
    private double last;
    private long lastTime;

    public ReachDisplayHud() {
        super("Reach Display", "Distance of your most recent hit.");
        defaultPosition(Anchor.TOP_LEFT, 4, 130);
        on(Events.Attack.class, e -> {
            last = e.distance;
            lastTime = Time.millis();
        });
    }

    @Override protected String label() { return "Blocks"; }

    @Override
    protected String value() {
        if (Time.millis() - lastTime > resetAfter.d() * 1000) return "Idle";
        return MathUtil.format(last, decimals.i());
    }

    @Override protected String previewValue() { return "3.00"; }

    @Override
    protected String compose(boolean editing) {
        String v = value();
        if ("Idle".equals(v) && !editing) return brackets.on() ? "[Idle]" : "Idle";
        return super.compose(editing);
    }
}
