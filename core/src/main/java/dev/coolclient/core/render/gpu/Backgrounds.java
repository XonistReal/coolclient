package dev.coolclient.core.render.gpu;

import java.util.ArrayList;
import java.util.List;

/** The animated shader backgrounds for menus and the launcher (see background.fsh). */
public final class Backgrounds {
    public enum Style {
        AURORA("Aurora", "BG_AURORA"),
        NEBULA("Nebula", "BG_NEBULA"),
        SYNTHWAVE("Synthwave", "BG_SYNTHWAVE"),
        LIQUID("Liquid", "BG_LIQUID"),
        WAVES("Waves", "BG_WAVES"),
        WARP("Warp Speed", "BG_WARP"),
        LAVA("Lava Lamp", "BG_LAVA"),
        MINIMAL("Minimal", "BG_MINIMAL");

        public final String displayName;
        final String define;

        Style(String displayName, String define) {
            this.displayName = displayName;
            this.define = define;
        }
    }

    private Backgrounds() {}

    public static Style byName(String name) {
        for (Style s : Style.values()) if (s.displayName.equalsIgnoreCase(name) || s.name().equalsIgnoreCase(name)) return s;
        return null;
    }

    public static List<String> names() {
        List<String> out = new ArrayList<String>();
        for (Style s : Style.values()) out.add(s.displayName);
        return out;
    }
}
