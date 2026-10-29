package dev.coolclient.core.render;

import dev.coolclient.core.render.gpu.Backgrounds;
import dev.coolclient.core.util.Time;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * UI palette. The colours below are the active {@link Preset}'s and change when the theme
 * does; accent colours are user-configurable through the Client module.
 */
public final class Theme {
    public static int accentA = 0xFF2DE2C1;
    public static int accentB = 0xFF8A5CFF;
    public static boolean animatedAccent = true;
    public static float uiOpacity = 0.92f;
    /** Frosted-glass panels (blur what's behind) where the GPU supports it. */
    public static boolean glass = true;
    /** Glass blur strength as a mip level (2 = light, 5 = heavy). */
    public static float blur = 3.5f;
    /** Animated menu background. */
    public static Backgrounds.Style background = Backgrounds.Style.AURORA;
    /** True for light themes (dark text on light surfaces). */
    public static boolean light;

    public static int BACKDROP = 0x99060912;
    public static int PANEL = 0xF20E121D;
    public static int PANEL_LIGHT = 0xFF141A28;
    public static int SURFACE = 0xFF19202F;
    public static int SURFACE_HOVER = 0xFF212A3D;
    public static int SURFACE_ACTIVE = 0xFF2A3550;
    public static int BORDER = 0x22FFFFFF;
    public static int BORDER_STRONG = 0x40FFFFFF;
    public static int TEXT = 0xFFEDF1FA;
    public static int TEXT_DIM = 0xFF8D97B0;
    public static int TEXT_FAINT = 0xFF5B6680;
    /** Hover/selection wash: white on dark themes, black on light ones (use with low alpha). */
    public static int HIGHLIGHT = 0xFFFFFFFF;
    /** Text drawn on top of the accent gradient. */
    public static int ON_ACCENT = 0xFF0A0E18;
    public static int SUCCESS = 0xFF34D399;
    public static int DANGER = 0xFFFF5470;
    public static int WARNING = 0xFFFFB547;

    private static final List<Preset> PRESETS = new ArrayList<Preset>();
    private static Preset current;

    /** A complete palette plus the background it looks best on. */
    public static final class Preset {
        public final String name;
        public final boolean light;
        public final int accentA, accentB;
        public final Backgrounds.Style background;
        public final int backdrop, panel, panelLight, surface, surfaceHover, surfaceActive;
        public final int border, borderStrong, text, textDim, textFaint;

        Preset(String name, boolean light, int accentA, int accentB, Backgrounds.Style background,
               int backdrop, int panel, int panelLight, int surface, int surfaceHover, int surfaceActive,
               int border, int borderStrong, int text, int textDim, int textFaint) {
            this.name = name;
            this.light = light;
            this.accentA = accentA;
            this.accentB = accentB;
            this.background = background;
            this.backdrop = backdrop;
            this.panel = panel;
            this.panelLight = panelLight;
            this.surface = surface;
            this.surfaceHover = surfaceHover;
            this.surfaceActive = surfaceActive;
            this.border = border;
            this.borderStrong = borderStrong;
            this.text = text;
            this.textDim = textDim;
            this.textFaint = textFaint;
        }
    }

    static {
        add(new Preset("Aurora", false, 0xFF2DE2C1, 0xFF8A5CFF, Backgrounds.Style.AURORA,
                0x99060912, 0xF20E121D, 0xFF141A28, 0xFF19202F, 0xFF212A3D, 0xFF2A3550,
                0x22FFFFFF, 0x40FFFFFF, 0xFFEDF1FA, 0xFF8D97B0, 0xFF5B6680));
        add(new Preset("Midnight", false, 0xFF4DA3FF, 0xFF6A5CFF, Backgrounds.Style.NEBULA,
                0x99050A16, 0xF20B1222, 0xFF101A30, 0xFF15203A, 0xFF1C2A4A, 0xFF25365C,
                0x22A0C0FF, 0x4AA0C0FF, 0xFFEAF0FF, 0xFF8A9AC0, 0xFF56658A));
        add(new Preset("Synthwave", false, 0xFFFF3DD2, 0xFF3DE8FF, Backgrounds.Style.SYNTHWAVE,
                0x990D0518, 0xF2160B24, 0xFF1E1030, 0xFF26143B, 0xFF32194D, 0xFF3F2061,
                0x26FF8AF0, 0x50FF8AF0, 0xFFFBEFFF, 0xFFB596C8, 0xFF7A5E8F));
        add(new Preset("Sakura", false, 0xFFFF7FB0, 0xFFFFC2D9, Backgrounds.Style.LIQUID,
                0x99120910, 0xF21C1018, 0xFF24141F, 0xFF2C1926, 0xFF3A2131, 0xFF4A2A3F,
                0x26FFB0D0, 0x50FFB0D0, 0xFFFFF0F6, 0xFFC09AAE, 0xFF876475));
        add(new Preset("Inferno", false, 0xFFFF7A1A, 0xFFFF3A5A, Backgrounds.Style.LAVA,
                0x990C0806, 0xF2140F0D, 0xFF1C1512, 0xFF241B16, 0xFF30231C, 0xFF3E2C22,
                0x22FFB080, 0x48FFB080, 0xFFFFF4EC, 0xFFB89C8A, 0xFF7E6656));
        add(new Preset("Ocean", false, 0xFF1FD6C6, 0xFF2D7CFF, Backgrounds.Style.WAVES,
                0x99040C0E, 0xF2081619, 0xFF0C1E23, 0xFF10262C, 0xFF16323A, 0xFF1D404A,
                0x2280E0FF, 0x4880E0FF, 0xFFE8FBFF, 0xFF86AAB4, 0xFF547880));
        add(new Preset("Terminal", false, 0xFF22FF66, 0xFF00C2A0, Backgrounds.Style.WARP,
                0x99010301, 0xF2050805, 0xFF0A100A, 0xFF0E170F, 0xFF132014, 0xFF1A2C1C,
                0x2622FF66, 0x5022FF66, 0xFFD8FFE0, 0xFF7FB08A, 0xFF4C7055));
        add(new Preset("Royal", false, 0xFFFFC83A, 0xFFFF8A3A, Backgrounds.Style.NEBULA,
                0x99070710, 0xF20E0F1C, 0xFF141526, 0xFF1A1C30, 0xFF23253F, 0xFF2D3050,
                0x26FFD98A, 0x50FFD98A, 0xFFFFF8E8, 0xFFB0A88C, 0xFF75705C));
        add(new Preset("OLED", false, 0xFFFFFFFF, 0xFF9AA0AA, Backgrounds.Style.MINIMAL,
                0xAA000000, 0xF2000000, 0xFF0A0A0A, 0xFF121212, 0xFF1C1C1C, 0xFF262626,
                0x22FFFFFF, 0x44FFFFFF, 0xFFFFFFFF, 0xFF9A9A9A, 0xFF5E5E5E));
        add(new Preset("Snow", true, 0xFF2D7CFF, 0xFF8A5CFF, Backgrounds.Style.LIQUID,
                0x66DDE3EE, 0xF2F4F6FB, 0xFFFFFFFF, 0xFFE6EAF3, 0xFFDCE2EE, 0xFFCFD7E8,
                0x22000000, 0x40000000, 0xFF141A28, 0xFF5A6478, 0xFF8A93A6));
        current = PRESETS.get(0);
    }

    private Theme() {}

    private static void add(Preset p) { PRESETS.add(p); }

    public static List<Preset> presets() { return Collections.unmodifiableList(PRESETS); }

    public static String[] presetNames() {
        String[] out = new String[PRESETS.size()];
        for (int i = 0; i < out.length; i++) out[i] = PRESETS.get(i).name;
        return out;
    }

    public static Preset preset(String name) {
        for (Preset p : PRESETS) if (p.name.equalsIgnoreCase(name)) return p;
        return PRESETS.get(0);
    }

    public static Preset current() { return current; }

    /** Switches every palette colour (accents are applied separately by the Client module). */
    public static void apply(Preset p) {
        current = p;
        light = p.light;
        BACKDROP = p.backdrop;
        PANEL = p.panel;
        PANEL_LIGHT = p.panelLight;
        SURFACE = p.surface;
        SURFACE_HOVER = p.surfaceHover;
        SURFACE_ACTIVE = p.surfaceActive;
        BORDER = p.border;
        BORDER_STRONG = p.borderStrong;
        TEXT = p.text;
        TEXT_DIM = p.textDim;
        TEXT_FAINT = p.textFaint;
        HIGHLIGHT = p.light ? 0xFF000000 : 0xFFFFFFFF;
        ON_ACCENT = p.light ? 0xFFFFFFFF : 0xFF0A0E18;
    }

    /** Accent gradient sample at t (0..1), gently animated when enabled. */
    public static int accent(float t) {
        if (animatedAccent) {
            t = t + (float) (Math.sin(Time.seconds() * 0.8) * 0.25);
        }
        t = Math.max(0f, Math.min(1f, t));
        return Colors.lerp(accentA, accentB, t);
    }

    public static int accent() { return accent(0f); }

    public static int panel() { return Colors.fade(PANEL, uiOpacity); }

    /** Hover wash of the given strength (0..255 alpha). */
    public static int highlight(int alpha) { return Colors.withAlpha(HIGHLIGHT, alpha); }
}
