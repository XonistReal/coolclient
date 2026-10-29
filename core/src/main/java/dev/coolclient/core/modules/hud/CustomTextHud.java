package dev.coolclient.core.modules.hud;

import dev.coolclient.core.hud.TextElement;
import dev.coolclient.core.setting.TextSetting;

public final class CustomTextHud extends TextElement {
    private final TextSetting text = add(new TextSetting("Text", "Cool Client", 64));

    public CustomTextHud() {
        super("Custom Text", "Any text you like. Supports & colour codes.");
        defaultPosition(Anchor.TOP_LEFT, 4, 256);
        labelMode.set("None");
    }

    @Override protected String label() { return ""; }

    @Override
    protected String value() { return text.get().replace('&', '§'); }
}
