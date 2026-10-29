package dev.coolclient.core.font;

import java.awt.Font;
import java.util.HashMap;
import java.util.Map;

/** Font registry. UI uses smooth fonts; HUD elements may pick vanilla or smooth. */
public final class Fonts {
    /** Families tried in order when the configured one is missing. */
    private static final String[] FALLBACKS = {"Segoe UI", "Inter", "Roboto", "Helvetica Neue", "Arial", "SansSerif"};

    private static final Map<String, SmoothFont> SMOOTH = new HashMap<String, SmoothFont>();
    private static final Map<String, Boolean> AVAILABLE = new HashMap<String, Boolean>();
    private static final VanillaFont VANILLA = new VanillaFont(1f);
    private static String family = "Segoe UI";
    private static float extraScale = 1f;

    private Fonts() {}

    public static FontRenderer vanilla() { return VANILLA; }

    public static SmoothFont get(float size) {
        String key = family + "@" + size;
        SmoothFont f = SMOOTH.get(key);
        if (f == null) {
            f = new SmoothFont(family, size);
            SMOOTH.put(key, f);
        }
        return f;
    }

    /** Small UI text (descriptions, labels). */
    public static SmoothFont small() { return get(7f); }

    /** Default UI text. */
    public static SmoothFont body() { return get(8.5f); }

    /** Headings. */
    public static SmoothFont title() { return get(12f); }

    public static SmoothFont huge() { return get(26f); }

    public static String family() { return family; }

    public static void setFamily(String name) {
        if (name == null || name.equals(family)) return;
        family = name;
        for (SmoothFont f : SMOOTH.values()) f.clearCache();
        SMOOTH.clear();
    }

    /** Extra scale applied by the current draw (e.g. a scaled HUD element), for crisp atlases. */
    public static float extraScale() { return extraScale; }

    public static void setExtraScale(float s) { extraScale = s <= 0 ? 1f : s; }

    static Font resolveFont(String preferred, int style, int px) {
        String name = isAvailable(preferred) ? preferred : null;
        if (name == null) {
            for (String f : FALLBACKS) {
                if (isAvailable(f)) { name = f; break; }
            }
        }
        if (name == null) name = Font.SANS_SERIF;
        return new Font(name, style, px);
    }

    private static boolean isAvailable(String familyName) {
        Boolean b = AVAILABLE.get(familyName);
        if (b == null) {
            Font f = new Font(familyName, Font.PLAIN, 12);
            b = f.getFamily().equalsIgnoreCase(familyName) || Font.SANS_SERIF.equalsIgnoreCase(familyName);
            AVAILABLE.put(familyName, b);
        }
        return b;
    }
}
