package dev.coolclient.core.modules.hud;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.hud.TextElement;
import dev.coolclient.core.setting.ModeSetting;
import dev.coolclient.core.util.Time;

public final class SessionTimeHud extends TextElement {
    private final ModeSetting since = add(new ModeSetting("Since", "Joining World", "Joining World", "Launching Game"));

    public SessionTimeHud() {
        super("Session Time", "How long you've been playing.");
        defaultPosition(Anchor.TOP_LEFT, 4, 202);
        labelMode.set("Prefix");
    }

    @Override protected String label() { return "Session"; }

    @Override
    protected String value() {
        long ms = since.is("Joining World") ? Time.millis() - CoolClient.get().worldJoinTime() : Time.millis();
        long s = ms / 1000;
        return s >= 3600 ? String.format("%d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60)
                : String.format("%d:%02d", s / 60, s % 60);
    }
}
