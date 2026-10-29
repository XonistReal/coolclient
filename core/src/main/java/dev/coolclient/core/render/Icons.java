package dev.coolclient.core.render;

import dev.coolclient.core.api.GuiRenderer;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * UI icons. Every icon fits in a square of side {@code s}. Icons with an SVG in
 * {@code assets/coolclient/icons/} use it; the rest (and any SVG that fails to load) are drawn
 * as vectors with {@link Draw}.
 */
public final class Icons {
    public enum Icon {
        GEAR("settings"), SEARCH, CLOSE, CHECK, CHEVRON_LEFT, CHEVRON_RIGHT, CHEVRON_DOWN, PLUS, MINUS, BACK,
        EDIT, LAYOUT, SLIDERS, POWER, USER, GLOBE, CUBE("singleplayer"), MULTIPLAYER("multiplayer"), EXIT,
        RESET("reset_hud"), STAR, SPARKLE, CROWN, EYE, KEYBOARD, PALETTE, FOLDER, GRID("mods"),
        HANGER("cosmetics"), MOON, PLAY, TERMINAL;

        final String svg;

        Icon() { this(null); }

        Icon(String svg) { this.svg = svg; }
    }

    private static final Map<Icon, SvgIcon> SVGS = new EnumMap<Icon, SvgIcon>(Icon.class);
    private static final Set<Icon> FAILED = EnumSet.noneOf(Icon.class);

    private Icons() {}

    /** Draws an icon; a two-colour icon's second colour is derived from {@code c} and the accent. */
    public static void draw(GuiRenderer r, Icon icon, float x, float y, float s, int c) {
        draw(r, icon, x, y, s, c, secondary(c));
    }

    /** Draws an icon with explicit colours; {@code c2} tints the second layer of two-colour icons. */
    public static void draw(GuiRenderer r, Icon icon, float x, float y, float s, int c, int c2) {
        SvgIcon svg = svg(icon);
        if (svg != null) {
            svg.draw(r, x, y, s, c, c2);
            return;
        }
        vector(r, icon == Icon.MULTIPLAYER ? Icon.GLOBE : icon, x, y, s, c);
    }

    /** The default second colour: the primary pulled towards the accent, slightly see-through. */
    public static int secondary(int c) {
        return Colors.withAlpha(Colors.lerp(c, Theme.accent(0.5f), 0.7f), (int) (Colors.a(c) * 0.9f));
    }

    private static SvgIcon svg(Icon icon) {
        if (icon.svg == null || FAILED.contains(icon)) return null;
        SvgIcon s = SVGS.get(icon);
        if (s == null) {
            s = SvgIcon.load("/assets/coolclient/icons/" + icon.svg + ".svg");
            if (s == null) {
                FAILED.add(icon);
                return null;
            }
            SVGS.put(icon, s);
        }
        return s;
    }

    private static void vector(GuiRenderer r, Icon icon, float x, float y, float s, int c) {
        float t = Math.max(1f, s / 9f);
        float cx = x + s / 2f, cy = y + s / 2f;
        switch (icon) {
            case GEAR: {
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI * 2 * i / 8;
                    float ox = (float) Math.cos(a), oy = (float) Math.sin(a);
                    Draw.line(r, cx + ox * s * 0.22f, cy + oy * s * 0.22f, cx + ox * s * 0.46f, cy + oy * s * 0.46f, s * 0.16f, c);
                }
                Draw.ring(r, cx, cy, s * 0.30f, s * 0.14f, c);
                break;
            }
            case SEARCH:
                Draw.ring(r, x + s * 0.42f, y + s * 0.42f, s * 0.28f, t * 1.2f, c);
                Draw.line(r, x + s * 0.62f, y + s * 0.62f, x + s * 0.9f, y + s * 0.9f, t * 1.4f, c);
                break;
            case CLOSE:
                Draw.line(r, x + s * 0.2f, y + s * 0.2f, x + s * 0.8f, y + s * 0.8f, t * 1.3f, c);
                Draw.line(r, x + s * 0.8f, y + s * 0.2f, x + s * 0.2f, y + s * 0.8f, t * 1.3f, c);
                break;
            case CHECK:
                Draw.polyline(r, new float[]{x + s * 0.18f, y + s * 0.52f, x + s * 0.42f, y + s * 0.75f, x + s * 0.84f, y + s * 0.27f}, t * 1.4f, c);
                break;
            case CHEVRON_LEFT:
                Draw.polyline(r, new float[]{x + s * 0.62f, y + s * 0.2f, x + s * 0.32f, y + s * 0.5f, x + s * 0.62f, y + s * 0.8f}, t * 1.3f, c);
                break;
            case CHEVRON_RIGHT:
                Draw.polyline(r, new float[]{x + s * 0.38f, y + s * 0.2f, x + s * 0.68f, y + s * 0.5f, x + s * 0.38f, y + s * 0.8f}, t * 1.3f, c);
                break;
            case CHEVRON_DOWN:
                Draw.polyline(r, new float[]{x + s * 0.2f, y + s * 0.38f, x + s * 0.5f, y + s * 0.68f, x + s * 0.8f, y + s * 0.38f}, t * 1.3f, c);
                break;
            case PLUS:
                Draw.line(r, cx, y + s * 0.18f, cx, y + s * 0.82f, t * 1.3f, c);
                Draw.line(r, x + s * 0.18f, cy, x + s * 0.82f, cy, t * 1.3f, c);
                break;
            case MINUS:
                Draw.line(r, x + s * 0.18f, cy, x + s * 0.82f, cy, t * 1.3f, c);
                break;
            case BACK:
                Draw.line(r, x + s * 0.2f, cy, x + s * 0.85f, cy, t * 1.3f, c);
                Draw.polyline(r, new float[]{x + s * 0.48f, y + s * 0.22f, x + s * 0.2f, cy, x + s * 0.48f, y + s * 0.78f}, t * 1.3f, c);
                break;
            case EDIT:
                Draw.line(r, x + s * 0.22f, y + s * 0.78f, x + s * 0.74f, y + s * 0.26f, t * 2f, c);
                Draw.triangle(r, x + s * 0.12f, y + s * 0.88f, x + s * 0.16f, y + s * 0.66f, x + s * 0.34f, y + s * 0.84f, c);
                break;
            case LAYOUT:
                Draw.roundedOutline(r, x + s * 0.1f, y + s * 0.1f, s * 0.36f, s * 0.36f, s * 0.08f, t, c);
                Draw.roundedOutline(r, x + s * 0.54f, y + s * 0.1f, s * 0.36f, s * 0.36f, s * 0.08f, t, c);
                Draw.roundedOutline(r, x + s * 0.1f, y + s * 0.54f, s * 0.36f, s * 0.36f, s * 0.08f, t, c);
                Draw.rounded(r, x + s * 0.54f, y + s * 0.54f, s * 0.36f, s * 0.36f, s * 0.08f, c);
                break;
            case SLIDERS:
                for (int i = 0; i < 3; i++) {
                    float ly = y + s * (0.25f + i * 0.25f);
                    Draw.line(r, x + s * 0.12f, ly, x + s * 0.88f, ly, t, c);
                    float kx = x + s * (i == 1 ? 0.65f : (i == 0 ? 0.35f : 0.5f));
                    Draw.circle(r, kx, ly, s * 0.1f, c);
                }
                break;
            case POWER:
                Draw.arc(r, cx, cy + s * 0.04f, s * 0.33f, t * 1.3f, 40, 280, c);
                Draw.line(r, cx, y + s * 0.1f, cx, cy, t * 1.3f, c);
                break;
            case USER:
                Draw.circle(r, cx, y + s * 0.34f, s * 0.18f, c);
                Draw.rounded(r, x + s * 0.2f, y + s * 0.6f, s * 0.6f, s * 0.3f, s * 0.15f, s * 0.15f, s * 0.04f, s * 0.04f, c);
                break;
            case GLOBE:
                Draw.ring(r, cx, cy, s * 0.4f, t, c);
                Draw.line(r, x + s * 0.1f, cy, x + s * 0.9f, cy, t * 0.8f, c);
                Draw.line(r, cx, y + s * 0.1f, cx, y + s * 0.9f, t * 0.8f, c);
                Draw.arc(r, cx, cy, s * 0.4f, t * 0.8f, -30, 60, c);
                break;
            case CUBE:
                Draw.polyline(r, new float[]{cx, y + s * 0.1f, x + s * 0.88f, y + s * 0.3f, x + s * 0.88f, y + s * 0.72f, cx, y + s * 0.92f,
                        x + s * 0.12f, y + s * 0.72f, x + s * 0.12f, y + s * 0.3f, cx, y + s * 0.1f}, t, c);
                Draw.polyline(r, new float[]{x + s * 0.12f, y + s * 0.3f, cx, y + s * 0.5f, x + s * 0.88f, y + s * 0.3f}, t, c);
                Draw.line(r, cx, y + s * 0.5f, cx, y + s * 0.92f, t, c);
                break;
            case EXIT:
                Draw.polyline(r, new float[]{x + s * 0.55f, y + s * 0.15f, x + s * 0.15f, y + s * 0.15f, x + s * 0.15f, y + s * 0.85f, x + s * 0.55f, y + s * 0.85f}, t, c);
                Draw.line(r, x + s * 0.4f, cy, x + s * 0.9f, cy, t * 1.2f, c);
                Draw.polyline(r, new float[]{x + s * 0.72f, y + s * 0.32f, x + s * 0.9f, cy, x + s * 0.72f, y + s * 0.68f}, t * 1.2f, c);
                break;
            case RESET:
                Draw.arc(r, cx, cy, s * 0.34f, t * 1.2f, 60, 280, c);
                Draw.triangle(r, cx + s * 0.18f, y + s * 0.08f, cx + s * 0.38f, y + s * 0.3f, cx + s * 0.08f, y + s * 0.36f, c);
                break;
            case STAR:
            case SPARKLE: {
                int points = icon == Icon.STAR ? 5 : 4;
                float outer = s * 0.46f, inner = icon == Icon.STAR ? s * 0.2f : s * 0.12f;
                r.beginTriangles();
                for (int i = 0; i < points * 2; i++) {
                    double a1 = Math.PI * i / points - Math.PI / 2, a2 = Math.PI * (i + 1) / points - Math.PI / 2;
                    float r1 = i % 2 == 0 ? outer : inner, r2 = i % 2 == 0 ? inner : outer;
                    r.vertex(cx, cy, c);
                    r.vertex(cx + (float) Math.cos(a1) * r1, cy + (float) Math.sin(a1) * r1, c);
                    r.vertex(cx + (float) Math.cos(a2) * r2, cy + (float) Math.sin(a2) * r2, c);
                }
                r.endTriangles();
                break;
            }
            case CROWN:
                r.beginTriangles();
                float by = y + s * 0.78f;
                float[] xs = {0.1f, 0.3f, 0.5f, 0.7f, 0.9f};
                float[] ys = {0.25f, 0.55f, 0.18f, 0.55f, 0.25f};
                for (int i = 0; i < 4; i++) {
                    r.vertex(x + s * xs[i], y + s * ys[i], c);
                    r.vertex(x + s * xs[i], by, c);
                    r.vertex(x + s * xs[i + 1], by, c);
                    r.vertex(x + s * xs[i], y + s * ys[i], c);
                    r.vertex(x + s * xs[i + 1], by, c);
                    r.vertex(x + s * xs[i + 1], y + s * ys[i + 1], c);
                }
                r.endTriangles();
                Draw.rect(r, x + s * 0.1f, by + s * 0.04f, s * 0.8f, s * 0.1f, c);
                break;
            case EYE:
                Draw.arc(r, cx, cy + s * 0.32f, s * 0.48f, t, -50, 100, c);
                Draw.arc(r, cx, cy - s * 0.32f, s * 0.48f, t, 130, 100, c);
                Draw.circle(r, cx, cy, s * 0.13f, c);
                break;
            case KEYBOARD:
                Draw.roundedOutline(r, x + s * 0.06f, y + s * 0.24f, s * 0.88f, s * 0.52f, s * 0.08f, t, c);
                for (int i = 0; i < 4; i++) Draw.rect(r, x + s * (0.18f + i * 0.18f), y + s * 0.36f, s * 0.1f, s * 0.08f, c);
                Draw.rect(r, x + s * 0.28f, y + s * 0.56f, s * 0.44f, s * 0.08f, c);
                break;
            case PALETTE:
                Draw.ring(r, cx, cy, s * 0.38f, t * 1.2f, c);
                Draw.circle(r, cx - s * 0.14f, cy - s * 0.12f, s * 0.07f, c);
                Draw.circle(r, cx + s * 0.12f, cy - s * 0.16f, s * 0.07f, c);
                Draw.circle(r, cx + s * 0.18f, cy + s * 0.08f, s * 0.07f, c);
                break;
            case FOLDER:
                Draw.rounded(r, x + s * 0.08f, y + s * 0.22f, s * 0.38f, s * 0.2f, s * 0.05f, c);
                Draw.rounded(r, x + s * 0.08f, y + s * 0.32f, s * 0.84f, s * 0.48f, s * 0.07f, c);
                break;
            case GRID:
                for (int i = 0; i < 3; i++)
                    for (int j = 0; j < 3; j++)
                        Draw.rounded(r, x + s * (0.1f + i * 0.3f), y + s * (0.1f + j * 0.3f), s * 0.2f, s * 0.2f, s * 0.04f, c);
                break;
            case HANGER:
                Draw.arc(r, cx, y + s * 0.26f, s * 0.12f, t, -90, 270, c);
                Draw.polyline(r, new float[]{cx, y + s * 0.38f, x + s * 0.08f, y + s * 0.74f, x + s * 0.92f, y + s * 0.74f, cx, y + s * 0.38f}, t, c);
                break;
            case PLAY:
                r.beginTriangles();
                r.vertex(x + s * 0.26f, y + s * 0.14f, c);
                r.vertex(x + s * 0.26f, y + s * 0.86f, c);
                r.vertex(x + s * 0.86f, cy, c);
                r.endTriangles();
                break;
            case TERMINAL:
                Draw.roundedOutline(r, x + s * 0.06f, y + s * 0.14f, s * 0.88f, s * 0.72f, s * 0.1f, t, c);
                Draw.polyline(r, new float[]{x + s * 0.24f, y + s * 0.36f, x + s * 0.42f, cy, x + s * 0.24f, y + s * 0.64f}, t, c);
                Draw.line(r, x + s * 0.5f, y + s * 0.64f, x + s * 0.74f, y + s * 0.64f, t, c);
                break;
            case MOON:
                Draw.circle(r, cx, cy, s * 0.38f, c);
                Draw.circle(r, cx + s * 0.16f, cy - s * 0.12f, s * 0.32f, Colors.withAlpha(Theme.PANEL, 255));
                break;
            default:
                break;
        }
    }
}
