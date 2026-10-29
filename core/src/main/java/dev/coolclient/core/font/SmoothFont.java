package dev.coolclient.core.font;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.api.GuiRenderer;
import dev.coolclient.core.render.Colors;
import dev.coolclient.core.util.Log;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Anti-aliased TrueType text. Glyphs are rasterised with java.awt into an atlas at the
 * physical pixel size (logical size x GUI scale x extra scale), so text is crisp at every
 * GUI scale. Atlases are cached per physical size.
 */
public final class SmoothFont implements FontRenderer {
    private static final int[] FORMAT_COLORS = {
            0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
    };
    private static final String EXTRA_CHARS =
            "•→←↑↓★☆♥⚔✔✖…—–°·●▲▼✦∞";

    private final String family;
    private final float size;
    private final Map<Integer, Atlas[]> cache = new HashMap<Integer, Atlas[]>();
    private float measureScale = 2f;

    public SmoothFont(String family, float size) {
        this.family = family;
        this.size = size;
    }

    public float size() { return size; }

    // ------------------------------------------------------------------ public API

    @Override
    public float draw(GuiRenderer r, String text, float x, float y, int color, boolean shadow) {
        if (text == null || text.isEmpty()) return 0;
        double physScale = physicalScale(r);
        Atlas[] atlases = atlases(physScale);
        if (atlases == null) return 0;
        if (shadow) {
            float off = Math.max(0.5f, size / 14f);
            int sc = Colors.argb(Colors.a(color), (Colors.r(color)) / 4, (Colors.g(color)) / 4, (Colors.b(color)) / 4);
            render(r, atlases, text, x + off, y + off, sc, true);
        }
        return render(r, atlases, text, x, y, color, false);
    }

    @Override
    public float width(String text) {
        if (text == null || text.isEmpty()) return 0;
        Atlas[] atlases = atlases(measureScale);
        if (atlases == null) return text.length() * size * 0.5f;
        float w = 0;
        boolean bold = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                char code = Character.toLowerCase(text.charAt(++i));
                if (code == 'l') bold = true;
                else if (code == 'r' || isColorCode(code)) bold = false;
                continue;
            }
            Glyph g = (bold ? atlases[1] : atlases[0]).glyph(c);
            if (g != null) w += g.advance / atlases[0].scale;
        }
        return w;
    }

    @Override
    public float height() { return size; }

    // ------------------------------------------------------------------ rendering

    private float render(GuiRenderer r, Atlas[] atlases, String text, float x, float y, int color, boolean shadowPass) {
        float startX = x;
        int base = color;
        int current = color;
        boolean bold = false;
        boolean underline = false;
        boolean strike = false;
        Atlas active = atlases[0];
        r.beginTextured(active.texture, true);
        int boundTexture = active.texture;
        float scale = (float) atlases[0].scale;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                char code = Character.toLowerCase(text.charAt(++i));
                int idx = "0123456789abcdef".indexOf(code);
                if (idx >= 0) {
                    int rgb = FORMAT_COLORS[idx];
                    if (shadowPass) rgb = ((rgb >> 2) & 0x3F3F3F);
                    current = (base & 0xFF000000) | rgb;
                    bold = underline = strike = false;
                } else if (code == 'l') {
                    bold = true;
                } else if (code == 'n') {
                    underline = true;
                } else if (code == 'm') {
                    strike = true;
                } else if (code == 'r') {
                    current = base;
                    bold = underline = strike = false;
                }
                continue;
            }
            Atlas a = bold ? atlases[1] : atlases[0];
            Glyph g = a.glyph(c);
            if (g == null) continue;
            if (a.texture != boundTexture) {
                r.endTextured();
                r.beginTextured(a.texture, true);
                boundTexture = a.texture;
            }
            float gx = x - a.pad / scale;
            float gy = y - a.pad / scale + a.yOffset / scale;
            float gw = g.cellW / scale, gh = g.cellH / scale;
            if (c != ' ') {
                r.vertexUV(gx, gy, g.u0, g.v0, current);
                r.vertexUV(gx, gy + gh, g.u0, g.v1, current);
                r.vertexUV(gx + gw, gy + gh, g.u1, g.v1, current);
                r.vertexUV(gx + gw, gy, g.u1, g.v0, current);
            }
            float adv = g.advance / scale;
            if (underline || strike) {
                r.endTextured();
                float lineY = underline ? y + size * 0.95f : y + size * 0.5f;
                dev.coolclient.core.render.Draw.rect(r, x, lineY, adv, Math.max(0.5f, size / 12f), current);
                r.beginTextured(boundTexture, true);
            }
            x += adv;
        }
        r.endTextured();
        return x - startX;
    }

    private static boolean isColorCode(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f');
    }

    private double physicalScale(GuiRenderer r) {
        double s = r.scaleFactor() * Fonts.extraScale();
        // Quantise so that tiny scale changes reuse atlases.
        s = Math.max(1.0, Math.round(s * 4.0) / 4.0);
        measureScale = (float) s;
        return s;
    }

    private Atlas[] atlases(double physScale) {
        int key = (int) Math.round(physScale * 100);
        Atlas[] a = cache.get(key);
        if (a == null) {
            if (cache.size() > 6) clearCache();
            try {
                a = new Atlas[]{build(Font.PLAIN, physScale), build(Font.BOLD, physScale)};
            } catch (Throwable t) {
                Log.error("Failed to build font atlas for " + family, t);
                a = null;
            }
            cache.put(key, a);
        }
        return a;
    }

    public void clearCache() {
        for (Atlas[] arr : cache.values()) {
            if (arr == null) continue;
            for (Atlas a : arr) CoolClient.get().platform().textures().delete(a.texture);
        }
        cache.clear();
    }

    // ------------------------------------------------------------------ atlas building

    private static final class Glyph {
        float u0, v0, u1, v1;
        int cellW, cellH;
        float advance;
    }

    private static final class Atlas {
        int texture;
        double scale;
        int pad;
        float yOffset;
        final Glyph[] latin = new Glyph[256];
        final Map<Character, Glyph> extra = new HashMap<Character, Glyph>();

        Glyph glyph(char c) {
            Glyph g = c < 256 ? latin[c] : extra.get(c);
            if (g == null) g = latin['?'];
            return g;
        }
    }

    private Atlas build(int style, double physScale) {
        int px = Math.max(6, (int) Math.round(size * physScale));
        Font font = Fonts.resolveFont(family, style, px);

        BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D pg = probe.createGraphics();
        pg.setFont(font);
        FontMetrics fm = pg.getFontMetrics();
        FontRenderContext frc = new FontRenderContext(null, true, true);
        int ascent = fm.getAscent();
        int descent = fm.getDescent();
        pg.dispose();

        StringBuilder chars = new StringBuilder();
        for (char c = 32; c < 127; c++) chars.append(c);
        for (char c = 160; c < 256; c++) chars.append(c);
        for (char c : EXTRA_CHARS.toCharArray()) if (font.canDisplay(c)) chars.append(c);

        int pad = 2;
        int cellH = ascent + descent + pad * 2;
        int texW = 1024;
        int x = 0, y = 0;
        int[] widths = new int[chars.length()];
        float[] advances = new float[chars.length()];
        for (int i = 0; i < chars.length(); i++) {
            char c = chars.charAt(i);
            advances[i] = (float) font.getStringBounds(String.valueOf(c), frc).getWidth();
            widths[i] = (int) Math.ceil(advances[i]) + pad * 2 + (style == Font.BOLD ? 1 : 0);
            if (x + widths[i] > texW) {
                x = 0;
                y += cellH;
            }
            x += widths[i];
        }
        int texH = 1;
        while (texH < y + cellH) texH <<= 1;

        BufferedImage img = new BufferedImage(texW, texH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setFont(font);
        g.setColor(java.awt.Color.WHITE);

        Atlas atlas = new Atlas();
        atlas.scale = physScale;
        atlas.pad = pad;
        // Put the baseline at ~78% of the line box so capitals sit centred in [y, y + size].
        atlas.yOffset = 0.78f * px - ascent;
        x = 0;
        y = 0;
        for (int i = 0; i < chars.length(); i++) {
            char c = chars.charAt(i);
            if (x + widths[i] > texW) {
                x = 0;
                y += cellH;
            }
            g.drawString(String.valueOf(c), x + pad, y + pad + ascent);
            Glyph gl = new Glyph();
            gl.cellW = widths[i];
            gl.cellH = cellH;
            gl.u0 = x / (float) texW;
            gl.v0 = y / (float) texH;
            gl.u1 = (x + widths[i]) / (float) texW;
            gl.v1 = (y + cellH) / (float) texH;
            gl.advance = advances[i];
            if (c < 256) atlas.latin[c] = gl; else atlas.extra.put(c, gl);
            x += widths[i];
        }
        g.dispose();

        int[] pixels = img.getRGB(0, 0, texW, texH, null, 0, texW);
        // Force RGB to white; keep coverage in alpha (avoids dark fringes when filtering).
        for (int i = 0; i < pixels.length; i++) pixels[i] = (pixels[i] & 0xFF000000) | 0x00FFFFFF;
        atlas.texture = CoolClient.get().platform().textures().create(
                "font/" + family.toLowerCase().replace(' ', '_') + "_" + style + "_" + px, texW, texH, pixels, true);
        return atlas;
    }
}
