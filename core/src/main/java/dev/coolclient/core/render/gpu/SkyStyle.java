package dev.coolclient.core.render.gpu;

/** The skyboxes in sky.fsh, with the fog colour that blends terrain into each horizon. */
public enum SkyStyle {
    AURORA("Aurora Night", "SKY_AURORA", 0xFF0D1A38),
    SPACE("Deep Space", "SKY_SPACE", 0xFF06070E),
    NEBULA("Nebula", "SKY_NEBULA", 0xFF1A0F2E),
    SUNSET("Sunset", "SKY_SUNSET", 0xFFD98A6A),
    SYNTHWAVE("Synthwave", "SKY_SYNTHWAVE", 0xFFB24A78),
    COTTON_CANDY("Cotton Candy", "SKY_COTTON", 0xFFF3C6DA),
    BLOOD_MOON("Blood Moon", "SKY_BLOODMOON", 0xFF360707),
    /** Follows the day/night cycle and keeps vanilla fog. */
    DYNAMIC("Dynamic", "SKY_DYNAMIC", 0),
    IMAGE("Custom Image", "SKY_IMAGE", 0);

    public final String displayName;
    final String define;
    /** Horizon colour used for fog, or 0 to keep the vanilla fog colour. */
    public final int fog;

    SkyStyle(String displayName, String define, int fog) {
        this.displayName = displayName;
        this.define = define;
        this.fog = fog;
    }

    public static SkyStyle byName(String name) {
        for (SkyStyle s : values()) if (s.displayName.equalsIgnoreCase(name)) return s;
        return AURORA;
    }

    public static String[] names() {
        SkyStyle[] v = values();
        String[] out = new String[v.length];
        for (int i = 0; i < v.length; i++) out[i] = v[i].displayName;
        return out;
    }
}
