package dev.coolclient.core.modules.hud;

import dev.coolclient.core.hud.TextElement;

public final class DayCounterHud extends TextElement {
    public DayCounterHud() {
        super("Day Counter", "Current in-game day.");
        defaultPosition(Anchor.TOP_LEFT, 4, 220);
        labelMode.set("Prefix");
    }

    @Override protected String label() { return "Day"; }

    @Override protected String value() { return String.valueOf(game().worldTime() / 24000L); }

    @Override protected String previewValue() { return "12"; }
}
