package dev.coolclient.core.render;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.api.GuiRenderer;
import dev.coolclient.core.util.Log;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An icon loaded from an SVG file and rasterised with AWT at the exact pixel size it is drawn at.
 *
 * Supports the subset that icon editors and tracers emit: {@code <g>}/{@code <path>} with
 * translate/scale/matrix transforms, and embedded PNGs ({@code <image>}, optionally placed with
 * {@code <use>}). Everything is treated as a tintable mask. Distinct colours become separate
 * layers so they can be tinted independently: layer 0 is the darkest colour (the "primary"),
 * layer 1 the lighter one (the "secondary"). At most two layers are kept.
 */
final class SvgIcon {
    /** Fraction of the icon square the content's longer side fills. */
    private static final float FILL = 0.9f;

    private final List<List<Painter>> layers;
    private final Rectangle2D bounds;
    private final Map<Integer, Integer> textures = new LinkedHashMap<Integer, Integer>();

    private SvgIcon(List<List<Painter>> layers, Rectangle2D bounds) {
        this.layers = layers;
        this.bounds = bounds;
    }

    int layerCount() { return layers.size(); }

    /** Draws the icon into the square (x, y, s); {@code c2} tints the second colour layer. */
    void draw(GuiRenderer r, float x, float y, float s, int c, int c2) {
        int px = Math.max(4, (int) Math.round(s * r.scaleFactor()));
        if (px > 64) px = (px + 7) / 8 * 8;
        Integer tex = textures.get(px);
        if (tex == null) {
            if (textures.size() >= 12) release();
            int[] argb = rasterize(px);
            tex = CoolClient.get().platform().textures().create("icon", cellW(px) * layers.size(), px + 2, argb, true);
            textures.put(px, tex);
        }
        float texW = cellW(px) * layers.size(), texH = px + 2;
        float v0 = 1f / texH, v1 = (px + 1f) / texH;
        r.beginTextured(tex, true);
        // Secondary first so the primary colour sits on top where the two meet.
        for (int i = layers.size() - 1; i >= 0; i--) {
            int col = i == 0 ? c : c2;
            float u0 = (i * cellW(px) + 1f) / texW, u1 = (i * cellW(px) + 1f + px) / texW;
            r.vertexUV(x, y, u0, v0, col);
            r.vertexUV(x, y + s, u0, v1, col);
            r.vertexUV(x + s, y + s, u1, v1, col);
            r.vertexUV(x + s, y, u1, v0, col);
        }
        r.endTextured();
    }

    void release() {
        for (int t : textures.values()) CoolClient.get().platform().textures().delete(t);
        textures.clear();
    }

    private static int cellW(int px) { return px + 2; }

    /** All layers side by side, each in a cell with a 1px transparent border (no filter bleed). */
    int[] rasterize(int px) {
        int cw = cellW(px), w = cw * layers.size(), h = px + 2;
        int[] out = new int[w * h];
        double k = px * FILL / Math.max(bounds.getWidth(), bounds.getHeight());
        double tx = (px - bounds.getWidth() * k) / 2 - bounds.getX() * k;
        double ty = (px - bounds.getHeight() * k) / 2 - bounds.getY() * k;
        for (int i = 0; i < layers.size(); i++) {
            BufferedImage img = new BufferedImage(px, px, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setColor(java.awt.Color.WHITE);
            g.translate(tx, ty);
            g.scale(k, k);
            for (Painter p : layers.get(i)) p.paint(g, k);
            g.dispose();
            int[] px0 = img.getRGB(0, 0, px, px, null, 0, px);
            for (int yy = 0; yy < px; yy++) {
                for (int xx = 0; xx < px; xx++) {
                    out[(yy + 1) * w + i * cw + 1 + xx] = (px0[yy * px + xx] & 0xFF000000) | 0x00FFFFFF;
                }
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ painters

    private interface Painter {
        /** Paints in SVG user space; {@code k} is output pixels per user unit. */
        void paint(Graphics2D g, double k);
    }

    private static final class ShapePainter implements Painter {
        final Path2D shape;

        ShapePainter(Path2D shape) { this.shape = shape; }

        @Override
        public void paint(Graphics2D g, double k) { g.fill(shape); }
    }

    private static final class ImagePainter implements Painter {
        final BufferedImage mask;
        final AffineTransform place;

        ImagePainter(BufferedImage mask, AffineTransform place) {
            this.mask = mask;
            this.place = place;
        }

        @Override
        public void paint(Graphics2D g, double k) {
            // Halve the mask until bilinear sampling no longer skips source pixels.
            double scale = k * Math.sqrt(Math.abs(place.getDeterminant()));
            BufferedImage img = mask;
            AffineTransform at = new AffineTransform(place);
            while (scale < 0.5 && img.getWidth() > 2 && img.getHeight() > 2) {
                img = halve(img);
                at.scale(2, 2);
                scale *= 2;
            }
            g.drawImage(img, at, null);
        }
    }

    private static BufferedImage halve(BufferedImage src) {
        int w = Math.max(1, src.getWidth() / 2), h = Math.max(1, src.getHeight() / 2);
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    // ------------------------------------------------------------------ loading

    private static final Pattern TAG = Pattern.compile("<(/?)([A-Za-z][\\w:-]*)((?:[^>\"']|\"[^\"]*\"|'[^']*')*?)(/?)>");
    private static final Pattern ATTR = Pattern.compile("([\\w:-]+)\\s*=\\s*(\"([^\"]*)\"|'([^']*)')");
    private static final Pattern TRANSFORM = Pattern.compile("(matrix|translate|scale|rotate)\\s*\\(([^)]*)\\)");

    static SvgIcon load(String resource) {
        try (InputStream in = SvgIcon.class.getResourceAsStream(resource)) {
            if (in == null) {
                Log.warn("Icon resource missing: " + resource);
                return null;
            }
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            for (int n; (n = in.read(chunk)) > 0; ) buf.write(chunk, 0, n);
            return parse(new String(buf.toByteArray(), StandardCharsets.UTF_8));
        } catch (Throwable t) {
            Log.error("Failed to load icon " + resource, t);
            return null;
        }
    }

    static SvgIcon parse(String svg) throws Exception {
        // Colour key -> painters. Paths are keyed by fill colour; images split into tones.
        Map<Integer, List<Painter>> byColour = new LinkedHashMap<Integer, List<Painter>>();
        Map<String, Map<String, String>> images = new HashMap<String, Map<String, String>>();
        List<Object[]> uses = new ArrayList<Object[]>();
        ArrayDeque<AffineTransform> stack = new ArrayDeque<AffineTransform>();
        ArrayDeque<Integer> fills = new ArrayDeque<Integer>();
        stack.push(new AffineTransform());
        fills.push(0xFF000000);
        Rectangle2D bounds = null;
        int defsDepth = 0;

        Matcher m = TAG.matcher(svg);
        while (m.find()) {
            boolean closing = !m.group(1).isEmpty(), selfClosing = !m.group(4).isEmpty();
            String name = m.group(2);
            if (closing) {
                if (name.equals("g") && stack.size() > 1) {
                    stack.pop();
                    fills.pop();
                } else if (name.equals("defs")) {
                    defsDepth = Math.max(0, defsDepth - 1);
                }
                continue;
            }
            Map<String, String> a = attrs(m.group(3));
            AffineTransform t = new AffineTransform(stack.peek());
            if (a.containsKey("transform")) t.concatenate(transform(a.get("transform")));
            int fill = fill(a, fills.peek());
            if (name.equals("defs")) {
                if (!selfClosing) defsDepth++;
            } else if (name.equals("g")) {
                if (!selfClosing) {
                    stack.push(t);
                    fills.push(fill);
                }
            } else if (name.equals("path")) {
                if (fill == 0 || !a.containsKey("d")) continue;
                Path2D p = PathData.parse(a.get("d"));
                p.transform(t);
                layer(byColour, fill).add(new ShapePainter(p));
                bounds = union(bounds, p.getBounds2D());
            } else if (name.equals("image")) {
                if (a.containsKey("id")) images.put(a.get("id"), a);
                if (defsDepth == 0) uses.add(new Object[]{a, t, 0f, 0f});
            } else if (name.equals("use")) {
                String href = a.containsKey("href") ? a.get("href") : a.get("xlink:href");
                if (href != null && href.startsWith("#")) {
                    uses.add(new Object[]{href.substring(1), t, num(a.get("x")), num(a.get("y"))});
                }
            }
        }

        for (Object[] u : uses) {
            @SuppressWarnings("unchecked")
            Map<String, String> img = u[0] instanceof String ? images.get(u[0]) : (Map<String, String>) u[0];
            if (img == null) continue;
            String href = img.containsKey("href") ? img.get("href") : img.get("xlink:href");
            if (href == null || !href.startsWith("data:image/")) continue;
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(Base64.getMimeDecoder().decode(href.substring(href.indexOf(',') + 1))));
            if (src == null) continue;
            float w = img.containsKey("width") ? num(img.get("width")) : src.getWidth();
            float h = img.containsKey("height") ? num(img.get("height")) : src.getHeight();
            AffineTransform place = new AffineTransform((AffineTransform) u[1]);
            place.translate((Float) u[2] + num(img.get("x")), (Float) u[3] + num(img.get("y")));
            place.scale(w / src.getWidth(), h / src.getHeight());
            BufferedImage[] tones = splitTones(src);
            Rectangle2D content = opaqueBounds(src);
            if (content != null) bounds = union(bounds, place.createTransformedShape(content).getBounds2D());
            for (int i = 0; i < tones.length; i++) {
                // Image tones sort after path colours: darker tone first.
                layer(byColour, 0x7F000000 | i).add(new ImagePainter(tones[i], place));
            }
        }

        if (bounds == null || bounds.isEmpty()) throw new IllegalArgumentException("SVG has no drawable content");
        List<Map.Entry<Integer, List<Painter>>> entries = new ArrayList<Map.Entry<Integer, List<Painter>>>(byColour.entrySet());
        // Darkest colour first (the primary layer); merge anything past two into the secondary.
        entries.sort((x, y) -> Float.compare(lum(x.getKey()), lum(y.getKey())));
        List<List<Painter>> layers = new ArrayList<List<Painter>>();
        for (Map.Entry<Integer, List<Painter>> e : entries) {
            if (layers.size() < 2) layers.add(new ArrayList<Painter>(e.getValue()));
            else layers.get(1).addAll(e.getValue());
        }
        return new SvgIcon(layers, bounds);
    }

    private static List<Painter> layer(Map<Integer, List<Painter>> byColour, int key) {
        List<Painter> l = byColour.get(key);
        if (l == null) byColour.put(key, l = new ArrayList<Painter>());
        return l;
    }

    /** Sort key: real colours by luminance; image tones (alpha 0x7F) by tone index. */
    private static float lum(int key) {
        if ((key >>> 24) == 0x7F) return 1000 + (key & 0xFF);
        return Colors.luminance(key | 0xFF000000);
    }

    /**
     * Splits an image into white masks by tone. With two clearly different colours, each pixel's
     * coverage is shared between a dark and a light mask by how close it is to each, so
     * anti-aliased edges between the two colours stay smooth.
     */
    static BufferedImage[] splitTones(BufferedImage src) {
        int w = src.getWidth(), h = src.getHeight();
        int[] p = src.getRGB(0, 0, w, h, null, 0, w);
        float lo = 255, hi = 0;
        for (int c : p) {
            if ((c >>> 24) < 200) continue;
            float l = luma(c);
            lo = Math.min(lo, l);
            hi = Math.max(hi, l);
        }
        // Two-means on luminance, seeded with the extremes.
        for (int iter = 0; iter < 12 && hi - lo > 6; iter++) {
            double s0 = 0, s1 = 0;
            int n0 = 0, n1 = 0;
            float mid = (lo + hi) / 2;
            for (int c : p) {
                if ((c >>> 24) < 200) continue;
                float l = luma(c);
                if (l < mid) { s0 += l; n0++; } else { s1 += l; n1++; }
            }
            if (n0 == 0 || n1 == 0) break;
            lo = (float) (s0 / n0);
            hi = (float) (s1 / n1);
        }
        boolean twoTone = hi - lo > 6;
        int[] dark = new int[p.length], light = twoTone ? new int[p.length] : null;
        for (int i = 0; i < p.length; i++) {
            int a = p[i] >>> 24;
            if (a == 0) continue;
            float t = twoTone ? Math.max(0f, Math.min(1f, (luma(p[i]) - lo) / (hi - lo))) : 0f;
            dark[i] = (Math.round(a * (1 - t)) << 24) | 0xFFFFFF;
            if (twoTone) light[i] = (Math.round(a * t) << 24) | 0xFFFFFF;
        }
        BufferedImage d = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        d.setRGB(0, 0, w, h, dark, 0, w);
        if (!twoTone) return new BufferedImage[]{d};
        BufferedImage l = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        l.setRGB(0, 0, w, h, light, 0, w);
        return new BufferedImage[]{d, l};
    }

    private static float luma(int c) {
        return 0.299f * ((c >> 16) & 0xFF) + 0.587f * ((c >> 8) & 0xFF) + 0.114f * (c & 0xFF);
    }

    private static Rectangle2D opaqueBounds(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        int[] p = img.getRGB(0, 0, w, h, null, 0, w);
        int x0 = w, y0 = h, x1 = -1, y1 = -1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if ((p[y * w + x] >>> 24) < 16) continue;
                if (x < x0) x0 = x;
                if (x > x1) x1 = x;
                if (y < y0) y0 = y;
                if (y > y1) y1 = y;
            }
        }
        return x1 < 0 ? null : new Rectangle2D.Float(x0, y0, x1 - x0 + 1, y1 - y0 + 1);
    }

    private static Rectangle2D union(Rectangle2D a, Rectangle2D b) {
        if (a == null) return b;
        Rectangle2D u = new Rectangle2D.Double();
        Rectangle2D.union(a, b, u);
        return u;
    }

    private static Map<String, String> attrs(String s) {
        Map<String, String> out = new HashMap<String, String>();
        Matcher m = ATTR.matcher(s);
        while (m.find()) out.put(m.group(1), m.group(3) != null ? m.group(3) : m.group(4));
        // Inline style declarations override presentation attributes.
        String style = out.get("style");
        if (style != null) {
            for (String decl : style.split(";")) {
                int c = decl.indexOf(':');
                if (c > 0) out.put(decl.substring(0, c).trim(), decl.substring(c + 1).trim());
            }
        }
        return out;
    }

    /** Fill colour as ARGB, 0 for "none", inheriting when unset. */
    private static int fill(Map<String, String> a, int inherited) {
        String f = a.get("fill");
        if (f == null || f.equals("inherit") || f.equals("currentColor")) return inherited;
        if (f.equals("none") || f.equals("transparent")) return 0;
        if (f.startsWith("#")) {
            String hex = f.substring(1);
            if (hex.length() == 3) hex = "" + hex.charAt(0) + hex.charAt(0) + hex.charAt(1) + hex.charAt(1) + hex.charAt(2) + hex.charAt(2);
            try {
                return 0xFF000000 | Integer.parseInt(hex.substring(0, 6), 16);
            } catch (RuntimeException e) {
                return inherited;
            }
        }
        if (f.equals("white")) return 0xFFFFFFFF;
        return inherited;
    }

    private static AffineTransform transform(String s) {
        AffineTransform t = new AffineTransform();
        Matcher m = TRANSFORM.matcher(s);
        while (m.find()) {
            float[] v = PathData.numbers(m.group(2));
            switch (m.group(1)) {
                case "matrix":
                    if (v.length >= 6) t.concatenate(new AffineTransform(v[0], v[1], v[2], v[3], v[4], v[5]));
                    break;
                case "translate":
                    if (v.length >= 1) t.translate(v[0], v.length > 1 ? v[1] : 0);
                    break;
                case "scale":
                    if (v.length >= 1) t.scale(v[0], v.length > 1 ? v[1] : v[0]);
                    break;
                case "rotate":
                    if (v.length >= 3) t.rotate(Math.toRadians(v[0]), v[1], v[2]);
                    else if (v.length >= 1) t.rotate(Math.toRadians(v[0]));
                    break;
                default:
                    break;
            }
        }
        return t;
    }

    private static float num(String s) {
        if (s == null) return 0;
        float[] v = PathData.numbers(s);
        return v.length > 0 ? v[0] : 0;
    }

    /** SVG path data ("d" attribute) parser. Arcs are approximated by straight lines. */
    static final class PathData {
        private final String s;
        private int i;

        private PathData(String s) { this.s = s; }

        static float[] numbers(String s) {
            PathData p = new PathData(s);
            List<Float> out = new ArrayList<Float>();
            while (p.hasNumber()) out.add(p.number());
            float[] v = new float[out.size()];
            for (int k = 0; k < v.length; k++) v[k] = out.get(k);
            return v;
        }

        static Path2D parse(String d) {
            PathData p = new PathData(d);
            Path2D.Float path = new Path2D.Float(Path2D.WIND_NON_ZERO);
            float cx = 0, cy = 0, sx = 0, sy = 0, lcx = 0, lcy = 0;
            char cmd = 0, prev = 0;
            while (true) {
                p.skipSeparators();
                if (p.i >= p.s.length()) break;
                char ch = p.s.charAt(p.i);
                if (Character.isLetter(ch) && ch != 'e' && ch != 'E') {
                    cmd = ch;
                    p.i++;
                } else if (cmd == 0) {
                    break;
                }
                boolean rel = Character.isLowerCase(cmd);
                float ox = rel ? cx : 0, oy = rel ? cy : 0;
                switch (Character.toUpperCase(cmd)) {
                    case 'M':
                        cx = ox + p.number();
                        cy = oy + p.number();
                        path.moveTo(cx, cy);
                        sx = cx;
                        sy = cy;
                        // Extra coordinate pairs after a moveto are implicit linetos.
                        cmd = rel ? 'l' : 'L';
                        break;
                    case 'L':
                        cx = ox + p.number();
                        cy = oy + p.number();
                        path.lineTo(cx, cy);
                        break;
                    case 'H':
                        cx = ox + p.number();
                        path.lineTo(cx, cy);
                        break;
                    case 'V':
                        cy = oy + p.number();
                        path.lineTo(cx, cy);
                        break;
                    case 'C': {
                        float x1 = ox + p.number(), y1 = oy + p.number(), x2 = ox + p.number(), y2 = oy + p.number();
                        cx = ox + p.number();
                        cy = oy + p.number();
                        path.curveTo(x1, y1, x2, y2, cx, cy);
                        lcx = x2;
                        lcy = y2;
                        break;
                    }
                    case 'S': {
                        boolean smooth = "CcSs".indexOf(prev) >= 0;
                        float x1 = smooth ? 2 * cx - lcx : cx, y1 = smooth ? 2 * cy - lcy : cy;
                        float x2 = ox + p.number(), y2 = oy + p.number();
                        cx = ox + p.number();
                        cy = oy + p.number();
                        path.curveTo(x1, y1, x2, y2, cx, cy);
                        lcx = x2;
                        lcy = y2;
                        break;
                    }
                    case 'Q': {
                        float x1 = ox + p.number(), y1 = oy + p.number();
                        cx = ox + p.number();
                        cy = oy + p.number();
                        path.quadTo(x1, y1, cx, cy);
                        lcx = x1;
                        lcy = y1;
                        break;
                    }
                    case 'T': {
                        boolean smooth = "QqTt".indexOf(prev) >= 0;
                        float x1 = smooth ? 2 * cx - lcx : cx, y1 = smooth ? 2 * cy - lcy : cy;
                        cx = ox + p.number();
                        cy = oy + p.number();
                        path.quadTo(x1, y1, cx, cy);
                        lcx = x1;
                        lcy = y1;
                        break;
                    }
                    case 'A':
                        for (int k = 0; k < 5; k++) p.number();
                        cx = ox + p.number();
                        cy = oy + p.number();
                        path.lineTo(cx, cy);
                        break;
                    case 'Z':
                        path.closePath();
                        cx = sx;
                        cy = sy;
                        break;
                    default:
                        return path;
                }
                prev = cmd;
            }
            return path;
        }

        private void skipSeparators() {
            while (i < s.length() && (Character.isWhitespace(s.charAt(i)) || s.charAt(i) == ',')) i++;
        }

        private boolean hasNumber() {
            skipSeparators();
            if (i >= s.length()) return false;
            char c = s.charAt(i);
            return (c >= '0' && c <= '9') || c == '-' || c == '+' || c == '.';
        }

        private float number() {
            skipSeparators();
            int start = i;
            if (i < s.length() && (s.charAt(i) == '-' || s.charAt(i) == '+')) i++;
            boolean dot = false, digits = false;
            while (i < s.length()) {
                char c = s.charAt(i);
                if (c >= '0' && c <= '9') {
                    digits = true;
                    i++;
                } else if (c == '.' && !dot) {
                    dot = true;
                    i++;
                } else {
                    break;
                }
            }
            if (digits && i < s.length() && (s.charAt(i) == 'e' || s.charAt(i) == 'E')) {
                int save = i++;
                if (i < s.length() && (s.charAt(i) == '-' || s.charAt(i) == '+')) i++;
                int expStart = i;
                while (i < s.length() && Character.isDigit(s.charAt(i))) i++;
                if (i == expStart) i = save;
            }
            if (!digits) {
                // Malformed data: skip a character so parsing always makes progress.
                i = Math.max(i, start + 1);
                return 0;
            }
            return Float.parseFloat(s.substring(start, i));
        }
    }
}
