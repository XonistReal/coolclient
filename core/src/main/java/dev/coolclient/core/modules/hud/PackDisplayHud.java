package dev.coolclient.core.modules.hud;

import dev.coolclient.core.hud.TextElement;

public final class PackDisplayHud extends TextElement {
    public PackDisplayHud() {
        super("Pack Display", "Name of your active resource pack.");
        defaultPosition(Anchor.TOP_LEFT, 4, 184);
        labelMode.set("None");
    }

    @Override protected String label() { return "Pack"; }

    @Override
    protected String value() {
        String p = game().resourcePack();
        return p == null || p.isEmpty() ? "Default" : p;
    }
}
