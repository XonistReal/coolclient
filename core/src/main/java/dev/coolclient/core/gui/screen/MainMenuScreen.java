package dev.coolclient.core.gui.screen;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.api.GuiRenderer;
import dev.coolclient.core.api.VanillaScreen;
import dev.coolclient.core.cosmetic.Cosmetic;
import dev.coolclient.core.cosmetic.CosmeticCatalog;
import dev.coolclient.core.cosmetic.CosmeticType;
import dev.coolclient.core.cosmetic.CosmeticsManager;
import dev.coolclient.core.cosmetic.Season;
import dev.coolclient.core.cosmetic.Unlock;
import dev.coolclient.core.cosmetic.cape.CapeCosmetic;
import dev.coolclient.core.font.FontRenderer;
import dev.coolclient.core.font.Fonts;
import dev.coolclient.core.gui.UIScreen;
import dev.coolclient.core.gui.widget.Button;
import dev.coolclient.core.json.Json;
import dev.coolclient.core.render.Colors;
import dev.coolclient.core.render.Draw;
import dev.coolclient.core.render.Icons;
import dev.coolclient.core.render.Theme;
import dev.coolclient.core.render.gpu.Backgrounds;
import dev.coolclient.core.util.MathUtil;
import dev.coolclient.core.util.Scheduler;
import dev.coolclient.core.util.Time;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Cool Client's title screen, in one of two layouts (Client settings, "Main Menu Layout"):
 * Centered (logo and buttons in the middle) or Showcase (buttons on the left, the background's
 * name at the bottom and the current cosmetic drop in a spotlight on the right).
 */
public final class MainMenuScreen extends UIScreen {
    private static boolean welcomeChecked;
    private final List<Button> buttons = new ArrayList<Button>();
    private Button cosmetics;
    private Button settings;
    private Button themes;
    private Button support;
    private Button spotlightButton;
    private boolean showcase;
    /** Spotlight panel (Showcase layout); zero width when the window is too narrow for it. */
    private float sx, sy, sw, sh;
    private float buttonsBottom;

    public MainMenuScreen() {
        super(null);
    }

    @Override
    protected void layout() {
        buttons.clear();
        showcase = CoolClient.get().client().mainMenuLayout.is("Showcase");
        final UIScreen self = this;
        float bh = 21, gap = 5;
        Runnable single = () -> CoolClient.get().platform().openVanilla(VanillaScreen.SINGLEPLAYER, self);
        Runnable multi = () -> CoolClient.get().platform().openVanilla(VanillaScreen.MULTIPLAYER, self);
        Runnable mods = () -> CoolClient.get().platform().openScreen(new ModMenuScreen(self));
        Runnable options = () -> CoolClient.get().platform().openVanilla(VanillaScreen.OPTIONS, self);
        Runnable locker = () -> CoolClient.get().platform().openScreen(new CosmeticsScreen(self));
        Runnable quit = () -> CoolClient.get().platform().quit();
        if (showcase) {
            float bw = 172;
            float x = left();
            float y = Math.max(height * 0.34f, logoY() + Fonts.huge().height() + 22);
            buttons.add(btn("Singleplayer", Icons.Icon.CUBE, single, x, y, bw, bh).primary());
            y += bh + gap;
            buttons.add(btn("Multiplayer", Icons.Icon.MULTIPLAYER, multi, x, y, bw, bh));
            y += bh + gap;
            float half = (bw - gap) / 2f;
            buttons.add(btn("Cosmetics", Icons.Icon.HANGER, locker, x, y, half, bh));
            buttons.add(btn("Cool Mods", Icons.Icon.GRID, mods, x + half + gap, y, half, bh));
            y += bh + gap;
            buttons.add(btn("Options", Icons.Icon.GEAR, options, x, y, half, bh));
            buttons.add(btn("Quit", Icons.Icon.EXIT, quit, x + half + gap, y, half, bh).danger());
            buttonsBottom = y + bh;
            sw = Math.min(210, width * 0.36f);
            sx = width - sw - Math.max(20, width * 0.06f);
            sy = Math.max(38, height * 0.13f);
            sh = Math.min(height - sy - 30, 260);
            if (sx < x + bw + 30 || sh < 150) sw = 0;
            spotlightButton = btn("Open Cosmetics", Icons.Icon.HANGER, locker, sx + 12, sy + sh - 32, sw - 24, 20).primary();
        } else {
            float bw = 184;
            float x = (width - bw) / 2f;
            float y = height * 0.48f;
            buttons.add(btn("Singleplayer", Icons.Icon.CUBE, single, x, y, bw, bh).primary());
            y += bh + gap;
            buttons.add(btn("Multiplayer", Icons.Icon.MULTIPLAYER, multi, x, y, bw, bh));
            y += bh + gap;
            float half = (bw - gap) / 2f;
            buttons.add(btn("Cool Mods", Icons.Icon.GRID, mods, x, y, half, bh));
            buttons.add(btn("Options", Icons.Icon.GEAR, options, x + half + gap, y, half, bh));
            y += bh + gap;
            buttons.add(btn("Quit Game", Icons.Icon.EXIT, quit, x, y, bw, bh).danger());
            buttonsBottom = y + bh;
            sw = 0;
        }
        cosmetics = btn("", Icons.Icon.HANGER, locker, width - 30, 8, 22, 22).radius(11);
        settings = btn("", Icons.Icon.GLOBE, () -> CoolClient.get().platform().openVanilla(VanillaScreen.LANGUAGE, self), width - 56, 8, 22, 22).radius(11);
        themes = btn("", Icons.Icon.PALETTE, () -> CoolClient.get().platform().openScreen(ModMenuScreen.themes(self)), width - 82, 8, 22, 22).radius(11);
        support = btn("Support", Icons.Icon.STAR, () -> {
            String url = CoolClient.get().cosmetics().entitlements().storeUrl();
            if (!url.isEmpty()) CoolClient.get().platform().openUrl(url);
        }, width - 162, 8, 74, 22).radius(11).primary();
    }

    private float left() { return Math.max(24, width * 0.07f); }

    private float logoY() { return Math.max(34, height * 0.12f); }

    private static Button btn(String label, Icons.Icon icon, Runnable action, float x, float y, float w, float h) {
        Button b = new Button(label, action).icon(icon).radius(6).iconSize(label.isEmpty() ? 9 : 12);
        b.setBounds(x, y, w, h);
        return b;
    }

    @Override
    public void render(GuiRenderer r, float mx, float my, float delta) {
        if (!welcomeChecked) {
            welcomeChecked = true;
            if (WelcomeScreen.needed()) {
                final UIScreen self = this;
                Scheduler.main(() -> CoolClient.get().platform().openScreen(new WelcomeScreen(self)));
            }
        }
        if (showcase != CoolClient.get().client().mainMenuLayout.is("Showcase")) layout();
        float t = (float) Time.seconds();
        MenuBackground.draw(r, width, height, t);
        float open = openProgress();

        if (showcase) drawShowcase(r, mx, my, t, open);
        else drawCenteredLogo(r, t, open);

        for (Button b : buttons) b.render(r, mx, my);
        cosmetics.render(r, mx, my);
        settings.render(r, mx, my);
        themes.render(r, mx, my);
        // Only once the API server has a store link (STORE_URL) configured.
        boolean store = !CoolClient.get().cosmetics().entitlements().storeUrl().isEmpty();
        if (store) support.render(r, mx, my);

        FontRenderer sf = Fonts.small();
        String version = CoolClient.NAME + " " + CoolClient.VERSION + "  ·  Minecraft " + CoolClient.get().platform().minecraftVersion();
        if (showcase) {
            sf.drawRight(r, version, width - 6, height - 22, Theme.TEXT_FAINT, false);
        } else {
            sf.draw(r, version, 6, height - 12, Theme.TEXT_FAINT, false);
        }
        sf.drawRight(r, "Copyright Mojang AB. Do not distribute!", width - 6, height - 12, Theme.TEXT_FAINT, false);

        String name = CoolClient.get().platform().game().selfName();
        if (name != null) {
            float nw = sf.width(name) + 26;
            Draw.rounded(r, 8, 8, nw, 18, 9, 0x50000000);
            Draw.circle(r, 17, 17, 5, Theme.accent(0.2f));
            Icons.draw(r, Icons.Icon.USER, 13, 13, 8, Theme.ON_ACCENT);
            sf.draw(r, name, 26, 13.5f, Theme.TEXT, false);
        }
    }

    private void drawCenteredLogo(GuiRenderer r, float t, float open) {
        FontRenderer huge = Fonts.huge();
        String logo = "COOL";
        float spacing = 3f;
        float lw = logoWidth(huge, logo, spacing);
        float lx = (width - lw) / 2f, ly = height * 0.24f - (1f - open) * 10f;
        drawLogo(r, huge, logo, lx, ly, spacing, t, open);
        Fonts.body().drawCentered(r, "C L I E N T", width / 2f, ly + huge.height() + 4, Colors.fade(Theme.TEXT_DIM, open), false);
    }

    private static float logoWidth(FontRenderer huge, String logo, float spacing) {
        float lw = 0;
        for (int i = 0; i < logo.length(); i++) lw += huge.width(String.valueOf(logo.charAt(i))) + spacing;
        return lw - spacing;
    }

    private static void drawLogo(GuiRenderer r, FontRenderer huge, String logo, float lx, float ly, float spacing, float t, float open) {
        float lw = logoWidth(huge, logo, spacing);
        // Soft filled glow (an outline-only glow leaves a dark hole behind the letters).
        Draw.shadow(r, lx + 4, ly + 10, lw - 8, huge.height() - 16, 14, 30, Colors.withAlpha(Theme.accent(0.5f), (int) (22 * open)));
        float cx = lx;
        for (int i = 0; i < logo.length(); i++) {
            String ch = String.valueOf(logo.charAt(i));
            float bob = (float) Math.sin(t * 1.6f + i * 0.5f) * 1.2f;
            huge.draw(r, ch, cx, ly + bob, Colors.fade(Theme.accent(i / (float) (logo.length() - 1)), open), false);
            cx += huge.width(ch) + spacing;
        }
    }

    // ------------------------------------------------------------------ Showcase layout

    private void drawShowcase(GuiRenderer r, float mx, float my, float t, float open) {
        // A soft dark wash behind the left column keeps the buttons readable on bright backgrounds.
        Draw.gradientH(r, 0, 0, width * 0.55f, height, Colors.withAlpha(0x000000, (int) (110 * open)), 0x00000000);

        FontRenderer huge = Fonts.huge();
        float x = left(), ly = logoY() - (1f - open) * 10f;
        drawLogo(r, huge, "COOL", x, ly, 3f, t, open);
        float lw = logoWidth(huge, "COOL", 3f);
        FontRenderer body = Fonts.body();
        String sub = "C L I E N T";
        float subY = ly + huge.height() + 3;
        float subX = x + (lw - body.width(sub)) / 2f;
        body.draw(r, sub, subX, subY, Colors.fade(Theme.accent(0.5f), open), false);
        Draw.rect(r, x, subY + body.height() / 2f, subX - x - 6, 0.75f, Colors.fade(Theme.accent(0f), open * 0.8f));
        Draw.rect(r, subX + body.width(sub) + 6, subY + body.height() / 2f, x + lw - (subX + body.width(sub) + 6), 0.75f,
                Colors.fade(Theme.accent(1f), open * 0.8f));

        // The background's name and a line about it, bottom left.
        FontRenderer titleFont = Fonts.get(20f);
        float by = height - 48;
        if (by > buttonsBottom + 12) {
            Backgrounds.Style bg = Theme.background;
            Draw.rounded(r, x, by, 3, 34, 1.5f, Colors.fade(Theme.accent(0.2f), open));
            titleFont.draw(r, bg.displayName.toUpperCase(java.util.Locale.ROOT), x + 10, by - 1, Colors.fade(Theme.TEXT, open), false);
            body.draw(r, tagline(bg), x + 10, by + titleFont.height() + 2, Colors.fade(Theme.TEXT_DIM, open), false);
        }

        if (sw > 0) {
            if (skinMode()) drawSkin(r, mx, my, t, open);
            else drawSpotlight(r, mx, my, t, open);
            drawModeChips(r, mx, my, open);
        }
    }

    private static boolean skinMode() {
        return CoolClient.get().client().showcaseSpotlight.is("Your Skin");
    }

    private final dev.coolclient.core.cosmetic.PlayerFigure figure = new dev.coolclient.core.cosmetic.PlayerFigure();

    /** Your skin, wearing your cosmetics, idling; its head follows the mouse. */
    private void drawSkin(GuiRenderer r, float mx, float my, float t, float open) {
        dev.coolclient.core.cosmetic.PlayerFigure f = figure;
        f.wear(CoolClient.get().cosmetics().local());
        f.skin = dev.coolclient.core.cosmetic.SkinLoader.texture();
        f.slim = dev.coolclient.core.cosmetic.SkinLoader.slim();
        f.legacySkin = dev.coolclient.core.cosmetic.SkinLoader.legacy();
        f.showBody = true;
        f.idle = true;
        f.time = t;
        float cx = sx + sw / 2f;
        float feet = sy + sh - 52 + (1f - open) * 12f;
        float s = (sh - 80) / 36f;
        float neckY = feet - 24 * s, headY = neckY - 4 * s;
        float bodyYaw = 22f + (float) Math.sin(t * 0.35f) * 8f;
        f.headYaw = Math.max(-45f, Math.min(45f, -(mx - cx) / (width * 0.35f) * 45f)) - (bodyYaw - 22f);
        f.headPitch = Math.max(-25f, Math.min(25f, (my - headY) / (height * 0.5f) * 25f));
        Draw.shadow(r, cx - 6 * s, feet - 3, 12 * s, 5, 5, 16, Colors.fade(Colors.withAlpha(Theme.accent(0.3f), 90), open));
        r.drawModel(cx, neckY, s, bodyYaw, 3, f::draw);
        String name = CoolClient.get().platform().game().selfName();
        FontRenderer tf = Fonts.title();
        if (name != null) tf.drawCentered(r, name, cx, feet + 8, Colors.fade(Theme.TEXT, open), false);
        spotlightButton.setBounds(sx + 12, sy + sh - 30, sw - 24, 20);
        spotlightButton.render(r, mx, my);
    }

    /** "Drops" / "Skin" switch at the top right of the spotlight. */
    private void drawModeChips(GuiRenderer r, float mx, float my, float open) {
        String[] labels = {"Drops", "Skin"};
        float x = sx + sw - 12, y = sy + 9;
        for (int i = labels.length - 1; i >= 0; i--) {
            FontRenderer f = Fonts.small();
            float w = f.width(labels[i]) + 14;
            x -= w;
            boolean active = (i == 1) == skinMode();
            boolean hot = MathUtil.inside(mx, my, x, y, w, 13);
            if (active) Draw.roundedGradient(r, x, y, w, 13, 6.5f, Colors.fade(Theme.accent(0f), open), Colors.fade(Theme.accent(1f), open), true);
            else Draw.rounded(r, x, y, w, 13, 6.5f, Colors.fade(hot ? Theme.SURFACE_HOVER : Theme.SURFACE, open));
            f.draw(r, labels[i], x + 7, y + 3, Colors.fade(active ? Theme.ON_ACCENT : Theme.TEXT_DIM, open), false);
            chipRects[i] = new float[]{x, y, w, 13};
            x -= 4;
        }
    }

    private final float[][] chipRects = new float[2][];

    private static String tagline(Backgrounds.Style s) {
        switch (s) {
            case AURORA: return "Northern lights over a quiet night";
            case NEBULA: return "Drifting through a violet nebula";
            case SYNTHWAVE: return "A neon sun over an endless grid";
            case LIQUID: return "Soft colours folding into each other";
            case WAVES: return "Slow waves rolling in";
            case WARP: return "Stars streaking past at warp speed";
            case LAVA: return "Warm blobs rising and falling";
            default: return "Nothing but your colours";
        }
    }

    /** What the spotlight shows: the server's featured drop, else the current season's, else your cape. */
    private static final class Spotlight {
        final String kicker, title, subtitle;
        final Cosmetic cosmetic;

        Spotlight(String kicker, String title, String subtitle, Cosmetic cosmetic) {
            this.kicker = kicker;
            this.title = title;
            this.subtitle = subtitle;
            this.cosmetic = cosmetic;
        }
    }

    private static Spotlight spotlight() {
        CosmeticsManager cm = CoolClient.get().cosmetics();
        Map<String, Object> f = cm.catalog().featured();
        if (f != null) {
            Cosmetic c = CosmeticCatalog.get(Json.str(f, "cosmetic", ""));
            if (c != null) return new Spotlight(Json.str(f, "kicker", "FEATURED"), Json.str(f, "title", c.name), Json.str(f, "subtitle", c.description), c);
        }
        Cosmetic best = null;
        for (Cosmetic c : CosmeticCatalog.all()) {
            if (c.season() == null || !c.season().active() || !cm.entitlements().listed(c)) continue;
            if (best == null || (c instanceof CapeCosmetic && !(best instanceof CapeCosmetic))) best = c;
            if (best instanceof CapeCosmetic) break;
        }
        if (best != null) {
            Season s = best.season();
            String when = best.unlock == Unlock.FREE ? "Free until " + s.lastDay() + ". Equip one to keep it." : "Out until " + s.lastDay() + ".";
            return new Spotlight("NOW DROPPING", s.displayName + " Drop", when, best);
        }
        Cosmetic cape = cm.local().cosmetic(CosmeticType.CAPE);
        if (cape == null) cape = CosmeticCatalog.get("cape_aurora");
        return new Spotlight("YOUR CAPE", cape.name, cape.description, cape);
    }

    private void drawSpotlight(GuiRenderer r, float mx, float my, float t, float open) {
        Spotlight s = spotlight();
        float x = sx, y = sy + (1f - open) * 12f, w = sw, h = sh;
        Draw.shadow(r, x, y, w, h, 10, 20, Colors.fade(0x70000000, open));
        Draw.rounded(r, x, y, w, h, 10, Colors.fade(Colors.withAlpha(Theme.PANEL, 190), open));
        Draw.roundedOutline(r, x, y, w, h, 10, 0.75f, Colors.fade(Theme.BORDER, open));
        int accent = s.cosmetic.season() != null ? s.cosmetic.season().color : Theme.accent(0.3f);
        Fonts.get(7f).draw(r, s.kicker, x + 12, y + 11, Colors.fade(accent, open), false);
        FontRenderer tf = Fonts.title();
        tf.draw(r, tf.trim(s.title, w - 24), x + 12, y + 21, Colors.fade(Theme.TEXT, open), false);

        // The cosmetic, slowly turning.
        float areaTop = y + 40, areaBottom = y + h - 64;
        float cxm = x + w / 2f, cym = (areaTop + areaBottom) / 2f;
        Draw.shadow(r, cxm - 30, areaBottom - 6, 60, 6, 6, 14, Colors.fade(Colors.withAlpha(accent, 90), open));
        if (s.cosmetic instanceof CapeCosmetic) {
            CapeCosmetic cape = (CapeCosmetic) s.cosmetic;
            int tex = CoolClient.get().cosmetics().capeTexture(cape, CoolClient.get().cosmetics().local().color(cape));
            float ch = Math.min(areaBottom - areaTop, 120), cw = ch / 1.6f;
            float swing = (float) Math.cos(t * 0.8f);
            float vw = cw * Math.max(0.1f, Math.abs(swing));
            float u0 = swing >= 0 ? 1 / 64f : 12 / 64f, u1 = swing >= 0 ? 11 / 64f : 22 / 64f;
            Draw.texture(r, tex, cxm - vw / 2f, cym - ch / 2f, vw, ch, u0, 1 / 32f, u1, 17 / 32f, Colors.fade(0xFFFFFFFF, open), true);
        } else {
            float bob = (float) Math.sin(t * 1.8f) * 3f;
            Draw.glow(r, cxm - 22, cym - 22 + bob, 44, 44, 22, 18, Colors.fade(Colors.withAlpha(s.cosmetic.defaultColor, 70), open));
            Icons.draw(r, icon(s.cosmetic.type), cxm - 22, cym - 22 + bob, 44, Colors.fade(Colors.withAlpha(s.cosmetic.defaultColor, 255), open));
        }

        List<String> lines = ModMenuScreen.wrap(Fonts.small(), s.subtitle, w - 24);
        for (int i = 0; i < Math.min(2, lines.size()); i++) {
            Fonts.small().draw(r, lines.get(i), x + 12, y + h - 58 + i * 9, Colors.fade(Theme.TEXT_DIM, open), false);
        }
        Fonts.small().draw(r, Fonts.small().trim(s.cosmetic.name + "  ·  " + s.cosmetic.rarity.displayName, w - 24), x + 12, y + h - 42,
                Colors.fade(s.cosmetic.rarity.color, open), false);
        spotlightButton.setBounds(x + 12, y + h - 30, w - 24, 20);
        spotlightButton.render(r, mx, my);
    }

    private static Icons.Icon icon(CosmeticType t) {
        switch (t) {
            case HAT: return Icons.Icon.CROWN;
            case HALO: return Icons.Icon.STAR;
            case WINGS: return Icons.Icon.SPARKLE;
            case BANDANA: return Icons.Icon.USER;
            case AURA: return Icons.Icon.MOON;
            default: return Icons.Icon.HANGER;
        }
    }

    @Override
    public boolean mouseClicked(float mx, float my, int button) {
        for (Button b : buttons) if (b.mouseClicked(mx, my, button)) return true;
        if (showcase && sw > 0) {
            for (int i = 0; i < chipRects.length; i++) {
                float[] c = chipRects[i];
                if (c != null && button == 0 && MathUtil.inside(mx, my, c[0], c[1], c[2], c[3])) {
                    CoolClient.get().platform().playClickSound();
                    CoolClient.get().client().showcaseSpotlight.set(i == 1 ? "Your Skin" : "New Drops");
                    CoolClient.get().config().markDirty();
                    return true;
                }
            }
            if (spotlightButton.mouseClicked(mx, my, button)) return true;
        }
        if (cosmetics.mouseClicked(mx, my, button)) return true;
        if (themes.mouseClicked(mx, my, button)) return true;
        if (!CoolClient.get().cosmetics().entitlements().storeUrl().isEmpty() && support.mouseClicked(mx, my, button)) return true;
        return settings.mouseClicked(mx, my, button);
    }

    @Override
    public boolean closeOnEscape() { return false; }
}
