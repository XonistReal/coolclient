package dev.coolclient.core.modules.hud;

import dev.coolclient.core.hud.TextElement;

public final class FpsHud extends TextElement {
    public FpsHud() {
        super("FPS", "Shows your frames per second.");
        defaultPosition(Anchor.TOP_LEFT, 4, 4);
        enabledByDefault();
    }

    @Override protected String label() { return "FPS"; }

    @Override protected String value() { return String.valueOf(game().fps()); }
}
