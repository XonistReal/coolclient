package dev.coolclient.core.modules.hud;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.hud.TextElement;
import dev.coolclient.core.setting.BoolSetting;

public final class ServerAddressHud extends TextElement {
    private final BoolSetting hideSingleplayer = add(new BoolSetting("Hide In Singleplayer", true));

    public ServerAddressHud() {
        super("Server Address", "The server you're connected to.");
        defaultPosition(Anchor.TOP_LEFT, 4, 112);
        labelMode.set("None");
    }

    @Override protected String label() { return "IP"; }

    @Override
    protected String value() {
        String s = CoolClient.get().server();
        return s == null ? "Singleplayer" : s;
    }

    @Override protected String previewValue() { return "play.example.net"; }

    @Override
    public boolean shouldRender() {
        return !(hideSingleplayer.on() && CoolClient.get().server() == null);
    }
}
