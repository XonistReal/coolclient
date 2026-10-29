package dev.coolclient.core.modules.hud;

import dev.coolclient.core.event.Events;
import dev.coolclient.core.hud.TextElement;
import dev.coolclient.core.setting.KeySetting;
import dev.coolclient.core.util.Keys;
import dev.coolclient.core.util.Time;

public final class StopwatchHud extends TextElement {
    private final KeySetting startStop = add(new KeySetting("Start/Stop Key", Keys.NONE));
    private final KeySetting reset = add(new KeySetting("Reset Key", Keys.NONE));
    private long accumulated;
    private long startedAt = -1;

    public StopwatchHud() {
        super("Stopwatch", "A hotkey stopwatch.");
        defaultPosition(Anchor.TOP_LEFT, 4, 238);
        labelMode.set("None");
        on(Events.Key.class, e -> {
            if (!e.pressed) return;
            if (startStop.bound() && e.key == startStop.key()) {
                if (startedAt < 0) startedAt = Time.millis();
                else {
                    accumulated += Time.millis() - startedAt;
                    startedAt = -1;
                }
            } else if (reset.bound() && e.key == reset.key()) {
                accumulated = 0;
                startedAt = -1;
            }
        });
    }

    @Override protected String label() { return "Stopwatch"; }

    @Override
    protected String value() {
        long ms = accumulated + (startedAt >= 0 ? Time.millis() - startedAt : 0);
        return String.format("%d:%02d.%02d", ms / 60000, (ms / 1000) % 60, (ms / 10) % 100);
    }
}
