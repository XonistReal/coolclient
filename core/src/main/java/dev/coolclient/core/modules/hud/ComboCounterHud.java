package dev.coolclient.core.modules.hud;

import dev.coolclient.core.event.Events;
import dev.coolclient.core.hud.TextElement;
import dev.coolclient.core.setting.BoolSetting;
import dev.coolclient.core.util.Time;

public final class ComboCounterHud extends TextElement {
    private final BoolSetting hideZero = add(new BoolSetting("Hide At Zero", false));
    private int combo;
    private long lastHit;
    private int lastTarget = -1;

    public ComboCounterHud() {
        super("Combo Counter", "Consecutive hits on a target without being hit back.");
        defaultPosition(Anchor.TOP_LEFT, 4, 148);
        on(Events.Attack.class, e -> {
            if (!e.targetIsPlayer) return;
            long now = Time.millis();
            if (e.targetId != lastTarget || now - lastHit > 2500) combo = 0;
            combo++;
            lastHit = now;
            lastTarget = e.targetId;
        });
        on(Events.SelfHurt.class, e -> combo = 0);
        on(Events.Tick.class, e -> {
            if (combo > 0 && Time.millis() - lastHit > 2500) combo = 0;
        });
    }

    @Override protected String label() { return "Combo"; }

    @Override protected String value() { return String.valueOf(combo); }

    @Override protected String previewValue() { return "3"; }

    @Override
    public boolean shouldRender() { return !(hideZero.on() && combo == 0); }
}
