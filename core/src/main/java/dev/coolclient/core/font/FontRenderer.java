package dev.coolclient.core.font;

import dev.coolclient.core.api.GuiRenderer;

/** Text drawing. Supports section-sign colour codes (&#167;a, &#167;l, &#167;r ...). */
public interface FontRenderer {
    /** Draws text with its top-left at (x, y); returns the advance width. */
    float draw(GuiRenderer r, String text, float x, float y, int color, boolean shadow);

    float width(String text);

    /** Line height in GUI pixels. */
    float height();

    default float drawCentered(GuiRenderer r, String text, float cx, float y, int color, boolean shadow) {
        return draw(r, text, cx - width(text) / 2f, y, color, shadow);
    }

    default float drawRight(GuiRenderer r, String text, float rightX, float y, int color, boolean shadow) {
        return draw(r, text, rightX - width(text), y, color, shadow);
    }

    /** Trims text with an ellipsis so it fits in {@code maxWidth}. */
    default String trim(String text, float maxWidth) {
        if (width(text) <= maxWidth) return text;
        String ell = "...";
        float ew = width(ell);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            if (width(sb.toString() + text.charAt(i)) + ew > maxWidth) break;
            sb.append(text.charAt(i));
        }
        return sb + ell;
    }

    /** Strips section-sign formatting codes. */
    static String strip(String text) {
        if (text.indexOf('§') < 0) return text;
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                i++;
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
