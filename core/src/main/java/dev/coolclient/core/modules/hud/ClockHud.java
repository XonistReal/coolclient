package dev.coolclient.core.modules.hud;

import dev.coolclient.core.hud.TextElement;
import dev.coolclient.core.setting.BoolSetting;
import dev.coolclient.core.setting.ModeSetting;

import java.text.SimpleDateFormat;
import java.util.Date;

public final class ClockHud extends TextElement {
    private final ModeSetting format = add(new ModeSetting("Format", "24 Hour", "24 Hour", "12 Hour"));
    private final BoolSetting seconds = add(new BoolSetting("Show Seconds", false));
    private final BoolSetting date = add(new BoolSetting("Show Date", false));

    public ClockHud() {
        super("Clock", "Real-world time.");
        defaultPosition(Anchor.TOP_LEFT, 4, 76);
        labelMode.set("None");
    }

    @Override protected String label() { return "Time"; }

    @Override
    protected String value() {
        String p = format.is("24 Hour") ? "HH:mm" : "h:mm";
        if (seconds.on()) p += ":ss";
        if (format.is("12 Hour")) p += " a";
        if (date.on()) p = "dd/MM " + p;
        return new SimpleDateFormat(p).format(new Date());
    }
}
