package dev.coolclient.core.modules.hud;

import dev.coolclient.core.hud.TextElement;

public final class PingHud extends TextElement {
    public PingHud() {
        super("Ping", "Your latency to the server.");
        defaultPosition(Anchor.TOP_LEFT, 4, 40);
    }

    @Override protected String label() { return "Ping"; }

    @Override protected String suffixLabel() { return "ms"; }

    @Override
    protected String value() {
        int p = game().ping();
        return p < 0 ? "?" : String.valueOf(p);
    }

    @Override protected String previewValue() { return "42"; }
}
