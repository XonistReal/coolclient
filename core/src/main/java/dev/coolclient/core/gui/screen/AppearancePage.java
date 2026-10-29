package dev.coolclient.core.gui.screen;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.api.GuiRenderer;
import dev.coolclient.core.font.Fonts;
import dev.coolclient.core.gui.widget.ScrollArea;
import dev.coolclient.core.gui.widget.SettingWidgets;
import dev.coolclient.core.gui.widget.Widget;
import dev.coolclient.core.modules.client.ClientSettings;
import dev.coolclient.core.modules.visual.Skybox;
import dev.coolclient.core.render.Animation;
import dev.coolclient.core.render.Colors;
import dev.coolclient.core.render.Draw;
import dev.coolclient.core.render.Icons;
import dev.coolclient.core.render.Scissor;
import dev.coolclient.core.render.Theme;
import dev.coolclient.core.render.gpu.Backgrounds;
import dev.coolclient.core.render.gpu.GpuDraw;
import dev.coolclient.core.render.gpu.SkyRenderer;
import dev.coolclient.core.render.gpu.SkyStyle;
import dev.coolclient.core.setting.BoolSetting;
import dev.coolclient.core.util.MathUtil;
import dev.coolclient.core.util.Time;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The mod menu's Themes page: pick a theme, a menu background and a skybox from cards that
 * render the real shaders live, plus the glass/shader switches.
 */
final class AppearancePage {
    private static final float GAP = 7;

    private final ScrollArea scroll = new ScrollArea();
    private final Map<Object, Animation> hovers = new HashMap<Object, Animation>();
    private final List<float[]> hitRects = new ArrayList<float[]>();
    private final List<Runnable> hitActions = new ArrayList<Runnable>();
    private final List<Widget> toggles = new ArrayList<Widget>();
    private final List<BoolSetting> toggleSettings = new ArrayList<BoolSetting>();

    AppearancePage() {
        ClientSettings c = CoolClient.get().client();
        for (BoolSetting s : new BoolSetting[]{c.glassPanels, c.shaderUi, c.animatedAccent}) {
            toggleSettings.add(s);
            Widget w = SettingWidgets.create(s);
            w.setBounds(0, 0, 22, 11);
            toggles.add(w);
        }
    }

    void reset() { scroll.reset(); }

    private Animation hover(Object key, boolean hot) {
        Animation a = hovers.get(key);
        if (a == null) {
            a = new Animation(140, 0f);
            hovers.put(key, a);
        }
        a.target(hot);
        return a;
    }

    private void hit(float x, float y, float w, float h, Runnable action) {
        hitRects.add(new float[]{x, y, w, h});
        hitActions.add(action);
    }

    // ================================================================== drawing

    void draw(GuiRenderer r, float x, float y, float w, float h, float mx, float my, float alpha) {
        hitRects.clear();
        hitActions.clear();
        scroll.setBounds(x, y, w, h);
        Scissor.push(r, x - 2, y - 2, w + 4, h + 4);
        float top = y - scroll.offset();
        float yy = top;
        float t = (float) Time.seconds();
        ClientSettings client = CoolClient.get().client();
        boolean inView = scroll.contains(mx, my);

        // Themes
        yy = section(r, "Theme", "Colours for every menu, the HUD editor and the launcher", x, yy, alpha);
        List<Theme.Preset> presets = Theme.presets();
        int cols = Math.max(2, (int) ((w + GAP) / (112 + GAP)));
        float cw = (w - GAP * (cols - 1)) / cols, ch = 70;
        for (int i = 0; i < presets.size(); i++) {
            final Theme.Preset p = presets.get(i);
            float cx = x + (i % cols) * (cw + GAP), cy = yy + (i / cols) * (ch + GAP);
            themeCard(r, p, cx, cy, cw, ch, mx, my, inView, alpha, t);
            hit(cx, cy, cw, ch, () -> {
                client.theme.set(p.name);
                CoolClient.get().config().markDirty();
            });
        }
        yy += rows(presets.size(), cols) * (ch + GAP) + 10;

        // Menu backgrounds
        yy = section(r, "Menu Background", "Animated shaders behind the main menu and the launcher", x, yy, alpha);
        Backgrounds.Style[] styles = Backgrounds.Style.values();
        int bcols = Math.max(2, (int) ((w + GAP) / (96 + GAP)));
        float bw = (w - GAP * (bcols - 1)) / bcols, bh = 54;
        for (int i = 0; i <= styles.length; i++) {
            final Backgrounds.Style s = i == 0 ? null : styles[i - 1];
            final String name = s == null ? "Theme Default" : s.displayName;
            float cx = x + (i % bcols) * (bw + GAP), cy = yy + (i / bcols) * (bh + GAP);
            boolean active = client.menuBackground.is(name);
            backgroundCard(r, s == null ? Theme.current().background : s, name, active, cx, cy, bw, bh, mx, my, inView, alpha, t);
            hit(cx, cy, bw, bh, () -> {
                client.menuBackground.set(name);
                CoolClient.get().config().markDirty();
            });
        }
        yy += rows(styles.length + 1, bcols) * (bh + GAP) + 10;

        // Skyboxes
        final Skybox sky = CoolClient.get().modules().get(Skybox.class);
        if (sky != null) {
            yy = section(r, "Skybox", "Replaces the overworld sky in game", x, yy, alpha);
            SkyStyle[] skies = SkyStyle.values();
            float sh = 60;
            for (int i = 0; i <= skies.length; i++) {
                final SkyStyle s = i == 0 ? null : skies[i - 1];
                float cx = x + (i % bcols) * (bw + GAP), cy = yy + (i / bcols) * (sh + GAP);
                boolean active = s == null ? !sky.isEnabledRaw() : sky.isEnabledRaw() && sky.sky() == s;
                skyCard(r, sky, s, active, i, cx, cy, bw, sh, mx, my, inView, alpha, t);
                hit(cx, cy, bw, sh, () -> {
                    if (s == null) {
                        sky.setEnabled(false);
                    } else {
                        sky.style.set(s.displayName);
                        sky.setEnabled(true);
                        if (s == SkyStyle.IMAGE && SkyRenderer.imageTexture(sky.image.get()) == 0) {
                            SkyRenderer.folder().mkdirs();
                            CoolClient.get().platform().openFolder(SkyRenderer.folder());
                            CoolClient.get().notifications().info("Custom skybox",
                                    "Drop an equirectangular image named " + sky.image.get() + " in this folder");
                        }
                    }
                    CoolClient.get().config().markDirty();
                });
            }
            yy += rows(skies.length + 1, bcols) * (sh + GAP) + 10;
        }

        // Switches
        yy = section(r, "Effects", "Accent colours, fonts and opacity are under Settings", x, yy, alpha);
        for (int i = 0; i < toggles.size(); i++) {
            BoolSetting s = toggleSettings.get(i);
            boolean hot = inView && MathUtil.inside(mx, my, x, yy, w, 24);
            float hv = hover(s, hot).get();
            Draw.rounded(r, x, yy, w, 22, 5, Colors.fade(Colors.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, hv), alpha));
            Fonts.body().draw(r, s.name(), x + 8, yy + 3.5f, Colors.fade(Theme.TEXT, alpha), false);
            Fonts.small().draw(r, Fonts.small().trim(s.description(), w - 50), x + 8, yy + 13, Colors.fade(Theme.TEXT_FAINT, alpha), false);
            Widget tw = toggles.get(i);
            tw.setBounds(x + w - 30, yy + 5.5f, 22, 11);
            tw.render(r, mx, my);
            yy += 26;
        }

        Scissor.pop(r);
        scroll.setContentHeight(yy - top);
        scroll.update(r, mx, my, CoolClient.get().platform().input().isMouseDown(0));
    }

    private static int rows(int n, int cols) { return (n + cols - 1) / cols; }

    private float section(GuiRenderer r, String title, String hint, float x, float y, float alpha) {
        Fonts.title().draw(r, title, x, y, Colors.fade(Theme.TEXT, alpha), false);
        Fonts.small().draw(r, hint, x + Fonts.title().width(title) + 8, y + 3, Colors.fade(Theme.TEXT_FAINT, alpha), false);
        return y + Fonts.title().height() + 7;
    }

    /** Lifts a hovered card and draws its shadow; returns the hover amount. */
    private float lift(GuiRenderer r, Object key, float x, float y, float w, float h, float mx, float my, boolean inView, float alpha) {
        boolean hot = inView && MathUtil.inside(mx, my, x, y, w, h);
        float hv = hover(key, hot).get();
        if (hv > 0.01f) Draw.shadow(r, x, y - hv * 1.5f, w, h, 7, 10, Colors.fade(0x50000000, hv * alpha));
        return hv;
    }

    private static void activeRing(GuiRenderer r, float x, float y, float w, float h, float alpha) {
        Draw.roundedOutlineGradient(r, x - 1.5f, y - 1.5f, w + 3, h + 3, 8.5f, 1.4f,
                Colors.fade(Theme.accent(0f), alpha), Colors.fade(Theme.accent(1f), alpha));
        Draw.circle(r, x + w - 9, y + 9, 5.5f, Colors.fade(Theme.accent(0.4f), alpha));
        Icons.draw(r, Icons.Icon.CHECK, x + w - 13, y + 5, 8, Colors.fade(Theme.ON_ACCENT, alpha));
    }

    private void themeCard(GuiRenderer r, Theme.Preset p, float x, float y, float w, float h,
                           float mx, float my, boolean inView, float alpha, float t) {
        float hv = lift(r, p, x, y, w, h, mx, my, inView, alpha);
        y -= hv * 1.5f;
        Draw.rounded(r, x, y, w, h, 7, Colors.fade(Colors.withAlpha(p.panel, 255), alpha));
        float px = x + 3, py = y + 3, pw = w - 6, ph = h - 22;
        if (!GpuDraw.background(r, p.background, px, py, pw, ph, t, alpha, 5, p.accentA, p.accentB, p.panel)) {
            Draw.roundedGradient(r, px, py, pw, ph, 5, Colors.fade(p.accentA, alpha), Colors.fade(p.accentB, alpha), GpuDraw.GRAD_DIAGONAL);
        }
        // A tiny mock window in the theme's own colours.
        float mw = Math.min(pw * 0.62f, 70), mh = 24, mxx = px + 6, myy = py + ph - mh - 6;
        Draw.rounded(r, mxx, myy, mw, mh, 4, Colors.fade(Colors.withAlpha(p.panel, 225), alpha));
        Draw.rounded(r, mxx + 5, myy + 5, mw * 0.5f, 3, 1.5f, Colors.fade(p.text, alpha));
        Draw.rounded(r, mxx + 5, myy + 11, mw * 0.35f, 2.5f, 1.25f, Colors.fade(p.textDim, alpha));
        Draw.roundedGradient(r, mxx + mw - 25, myy + mh - 10, 20, 6, 3, Colors.fade(p.accentA, alpha), Colors.fade(p.accentB, alpha), true);
        Fonts.body().draw(r, p.name, x + 8, y + h - 15.5f, Colors.fade(p.text, alpha), false);
        Draw.circle(r, x + w - 20, y + h - 10.5f, 3.5f, Colors.fade(p.accentA, alpha));
        Draw.circle(r, x + w - 10, y + h - 10.5f, 3.5f, Colors.fade(p.accentB, alpha));
        if (Theme.current() == p) activeRing(r, x, y, w, h, alpha);
        else Draw.roundedOutline(r, x, y, w, h, 7, 0.75f, Colors.fade(Theme.BORDER, alpha));
    }

    private void backgroundCard(GuiRenderer r, Backgrounds.Style s, String name, boolean active, float x, float y, float w, float h,
                                float mx, float my, boolean inView, float alpha, float t) {
        float hv = lift(r, name, x, y, w, h, mx, my, inView, alpha);
        y -= hv * 1.5f;
        if (!GpuDraw.background(r, s, x, y, w, h, t, alpha, 7, Theme.accentA, Theme.accentB, Theme.PANEL)) {
            Draw.roundedGradient(r, x, y, w, h, 7, Colors.fade(Theme.accentA, alpha), Colors.fade(Theme.accentB, alpha), GpuDraw.GRAD_DIAGONAL);
        }
        label(r, name, x, y, w, h, alpha);
        if (active) activeRing(r, x, y, w, h, alpha);
        else Draw.roundedOutline(r, x, y, w, h, 7, 0.75f, Colors.fade(Theme.BORDER, alpha));
    }

    private void skyCard(GuiRenderer r, Skybox sky, SkyStyle s, boolean active, int index, float x, float y, float w, float h,
                         float mx, float my, boolean inView, float alpha, float t) {
        float hv = lift(r, s == null ? "sky:off" : s, x, y, w, h, mx, my, inView, alpha);
        y -= hv * 1.5f;
        String name = s == null ? "Vanilla" : s.displayName;
        boolean drawn = false;
        if (s != null) {
            int image = s == SkyStyle.IMAGE ? SkyRenderer.imageTexture(sky.image.get()) : 0;
            if (s != SkyStyle.IMAGE || image != 0) {
                float yaw = t * 0.12f + index * 0.9f;
                drawn = GpuDraw.skyPreview(r, s, x, y, w, h, 7, yaw, 0.22f, t, image);
            }
            if (!drawn && s == SkyStyle.IMAGE) {
                Draw.rounded(r, x, y, w, h, 7, Colors.fade(Theme.SURFACE, alpha));
                Icons.draw(r, Icons.Icon.FOLDER, x + w / 2f - 6, y + 12, 12, Colors.fade(Theme.TEXT_DIM, alpha));
                Fonts.small().drawCentered(r, "Add skybox.png", x + w / 2f, y + 28, Colors.fade(Theme.TEXT_FAINT, alpha), false);
                drawn = true;
            }
        }
        if (!drawn) {
            int top = s == null ? 0xFF78A7FF : s.fog;
            Draw.roundedGradient(r, x, y, w, h, 7, Colors.fade(Colors.withAlpha(top, 255), alpha),
                    Colors.fade(Colors.lerp(top | 0xFF000000, 0xFF000000, 0.6f), alpha), GpuDraw.GRAD_V);
        }
        label(r, name, x, y, w, h, alpha);
        if (active) activeRing(r, x, y, w, h, alpha);
        else Draw.roundedOutline(r, x, y, w, h, 7, 0.75f, Colors.fade(Theme.BORDER, alpha));
    }

    /** Name over a dark fade at the bottom of a preview card. */
    private static void label(GuiRenderer r, String name, float x, float y, float w, float h, float alpha) {
        Draw.rounded(r, x, y + h - 18, w, 18, 0, 0, 7, 7, Colors.fade(0x8C000000, alpha));
        Fonts.small().draw(r, Fonts.small().trim(name, w - 12), x + 7, y + h - 12.5f, Colors.fade(0xFFFFFFFF, alpha), false);
    }

    // ================================================================== input

    boolean mouseClicked(float mx, float my, int button) {
        if (scroll.mouseClicked(mx, my)) return true;
        if (!scroll.contains(mx, my)) return false;
        for (Widget w : toggles) if (w.mouseClicked(mx, my, button)) return true;
        if (button != 0) return false;
        for (int i = 0; i < hitRects.size(); i++) {
            float[] h = hitRects.get(i);
            if (MathUtil.inside(mx, my, h[0], h[1], h[2], h[3])) {
                CoolClient.get().platform().playClickSound();
                hitActions.get(i).run();
                return true;
            }
        }
        return false;
    }

    boolean mouseScrolled(float mx, float my, double amount) {
        return scroll.scroll(mx, my, amount);
    }
}
