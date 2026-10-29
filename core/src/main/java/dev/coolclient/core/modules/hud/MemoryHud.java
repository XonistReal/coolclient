package dev.coolclient.core.modules.hud;

import dev.coolclient.core.hud.TextElement;
import dev.coolclient.core.setting.ModeSetting;

public final class MemoryHud extends TextElement {
    private final ModeSetting display = add(new ModeSetting("Display", "Percent", "Percent", "Megabytes"));

    public MemoryHud() {
        super("Memory", "Java heap usage.");
        defaultPosition(Anchor.TOP_LEFT, 4, 94);
        labelMode.set("Prefix");
    }

    @Override protected String label() { return "Mem"; }

    @Override
    protected String value() {
        Runtime rt = Runtime.getRuntime();
        long used = rt.totalMemory() - rt.freeMemory(), max = rt.maxMemory();
        if (display.is("Percent")) return (used * 100 / max) + "%";
        return (used / 1048576) + "/" + (max / 1048576) + "MB";
    }
}
