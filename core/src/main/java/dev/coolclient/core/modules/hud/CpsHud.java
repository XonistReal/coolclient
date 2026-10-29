package dev.coolclient.core.modules.hud;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.hud.TextElement;
import dev.coolclient.core.setting.ModeSetting;

public final class CpsHud extends TextElement {
    private final ModeSetting buttons = add(new ModeSetting("Buttons", "Left", "Left", "Right", "Both"));

    public CpsHud() {
        super("CPS", "Clicks per second for left and/or right mouse.");
        defaultPosition(Anchor.TOP_LEFT, 4, 22);
        enabledByDefault();
    }

    @Override protected String label() { return "CPS"; }

    @Override
    protected String value() {
        int l = CoolClient.get().leftCps().cps(), r = CoolClient.get().rightCps().cps();
        if (buttons.is("Right")) return String.valueOf(r);
        if (buttons.is("Both")) return l + " | " + r;
        return String.valueOf(l);
    }
}
