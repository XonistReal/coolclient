package dev.coolclient.core.modules.hud;

import dev.coolclient.core.hud.TextElement;
import dev.coolclient.core.util.MathUtil;

public final class SaturationHud extends TextElement {
    public SaturationHud() {
        super("Saturation", "Hidden hunger saturation value.");
        defaultPosition(Anchor.BOTTOM_CENTER, 120, -40);
        labelMode.set("Prefix");
    }

    @Override protected String label() { return "Saturation"; }

    @Override protected String value() { return MathUtil.format(game().saturation(), 1); }

    @Override protected String previewValue() { return "5.0"; }
}
