package dev.coolclient.core.gui.screen;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.api.GuiRenderer;
import dev.coolclient.core.font.FontRenderer;
import dev.coolclient.core.font.Fonts;
import dev.coolclient.core.gui.UIScreen;
import dev.coolclient.core.gui.widget.Button;
import dev.coolclient.core.gui.widget.KeyBinder;
import dev.coolclient.core.gui.widget.ScrollArea;
import dev.coolclient.core.gui.widget.SettingWidgets;
import dev.coolclient.core.gui.widget.TextField;
import dev.coolclient.core.gui.widget.Toggle;
import dev.coolclient.core.gui.widget.Widget;
import dev.coolclient.core.module.Category;
import dev.coolclient.core.module.Module;
import dev.coolclient.core.modules.client.ClientSettings;
import dev.coolclient.core.render.Animation;
import dev.coolclient.core.render.Colors;
import dev.coolclient.core.render.Draw;
import dev.coolclient.core.render.Easing;
import dev.coolclient.core.render.Icons;
import dev.coolclient.core.render.Scissor;
import dev.coolclient.core.render.Theme;
import dev.coolclient.core.setting.Setting;
import dev.coolclient.core.util.MathUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The CoolClient mod menu: categories, searchable module cards and per-module settings, in one of
 * three layouts (Client settings, "Mod Menu Layout"): Sidebar (categories down the side, cards),
 * Tabs (a tab bar, category chips, big tiles with favourites) and Compact (a dense list).
 */
public final class ModMenuScreen extends UIScreen {
    private static final float SIDEBAR = 116;
    private static final float HEADER = 34;
    /** Tabs layout: where the content starts below the tab bar and the chip row. */
    private static final float TABS_TOP = 64;

    private enum Layout { SIDEBAR, TABS, COMPACT }

    /** A clickable area recorded while drawing (left and right click actions). */
    private static final class Hit {
        final float x, y, w, h;
        final Runnable left, right;

        Hit(float x, float y, float w, float h, Runnable left, Runnable right) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.left = left;
            this.right = right;
        }
    }
    private static final String THEMES = "themes";
    private static Category rememberedCategory;

    private Category category = rememberedCategory;
    private Module selected;
    private final TextField search = new TextField().placeholder("Search mods...").icon(Icons.Icon.SEARCH).maxLength(32);
    private final ScrollArea gridScroll = new ScrollArea();
    private final ScrollArea settingsScroll = new ScrollArea();
    private final Animation page = new Animation(280, Easing.OUT_CUBIC, 0f);
    private final Map<Module, Animation> toggleAnims = new HashMap<Module, Animation>();
    private final Map<Object, Animation> hoverAnims = new HashMap<Object, Animation>();
    private final List<Row> rows = new ArrayList<Row>();
    private Toggle headerToggle;
    private Button resetButton;
    private final Button editHud;
    private AppearancePage appearance;
    private boolean themesPage;

    private float px, py, pw, ph;
    private float navH = 17;
    private boolean showFooter = true;
    private Layout style = Layout.SIDEBAR;
    private boolean favouritesOnly;
    /** Tabs layout on a narrow window: tabs show only their icons. */
    private boolean iconTabs;
    private final List<Hit> navHits = new ArrayList<Hit>();
    private final List<Hit> gridHits = new ArrayList<Hit>();

    private static final class Row {
        final Setting<?> setting;
        final String label;
        final String description;
        final Widget widget;

        Row(Setting<?> setting, String label, String description, Widget widget) {
            this.setting = setting;
            this.label = label;
            this.description = description;
            this.widget = widget;
        }
    }

    public ModMenuScreen(UIScreen parent) {
        super(parent);
        editHud = new Button("Edit HUD", () -> {
            if (parent instanceof HudEditorScreen) close();
            else CoolClient.get().platform().openScreen(new HudEditorScreen(null));
        }).icon(Icons.Icon.LAYOUT);
        search.onChange(s -> {
            gridScroll.reset();
            if (!s.isEmpty()) themesPage = false;
        });
    }

    public static ModMenuScreen forModule(UIScreen parent, Module m) {
        ModMenuScreen s = new ModMenuScreen(parent);
        s.category = m.category();
        s.openSettings(m);
        s.page.snap(1f);
        return s;
    }

    /** Opens on the module grid filtered by a search. */
    public static ModMenuScreen searching(UIScreen parent, String query) {
        ModMenuScreen s = new ModMenuScreen(parent);
        s.search.setText(query);
        return s;
    }

    public static ModMenuScreen clientSettings(UIScreen parent) {
        return forModule(parent, CoolClient.get().client());
    }

    /** Opens straight on the Themes page. */
    public static ModMenuScreen themes(UIScreen parent) {
        ModMenuScreen s = new ModMenuScreen(parent);
        s.openThemes();
        return s;
    }

    private void openThemes() {
        if (appearance == null) appearance = new AppearancePage();
        appearance.reset();
        themesPage = true;
        category = null;
        selected = null;
        search.setText("");
        page.snap(0f);
        page.target(0f);
    }

    // ================================================================== layout

    private static Layout chosenLayout() {
        String s = CoolClient.get().client().menuLayout.get();
        return "Tabs".equalsIgnoreCase(s) ? Layout.TABS : "Compact".equalsIgnoreCase(s) ? Layout.COMPACT : Layout.SIDEBAR;
    }

    private boolean tabs() { return style == Layout.TABS; }

    @Override
    protected void layout() {
        style = chosenLayout();
        pw = Math.min(width - 24, tabs() ? 680 : style == Layout.COMPACT ? 560 : 600);
        ph = Math.min(height - 24, tabs() ? 380 : 360);
        px = (width - pw) / 2f;
        py = (height - ph) / 2f;
        // 10 nav entries + a divider; shrink them (and drop the footer) on short screens.
        float avail = ph - 52 - 8;
        float step = (avail - 8 - 30) / 10f;
        showFooter = step >= 19;
        if (!showFooter) step = (avail - 8) / 10f;
        navH = MathUtil.clamp(step - 2, 11, 17);
    }

    private float contentX() { return tabs() ? px + 12 : px + SIDEBAR + 12; }

    private float contentW() { return tabs() ? pw - 24 : pw - SIDEBAR - 24; }

    private float contentY() { return tabs() ? py + TABS_TOP : py + HEADER + 10; }

    private float contentH() { return tabs() ? ph - TABS_TOP - 10 : ph - HEADER - 18; }

    // ================================================================== rendering

    @Override
    public void render(GuiRenderer r, float mx, float my, float delta) {
        float open = openProgress();
        layout();
        navHits.clear();
        gridHits.clear();
        py += (1f - open) * 14f;
        Draw.rect(r, 0, 0, width, height, Colors.fade(Theme.BACKDROP, open));
        Draw.beginPanels(r);
        Draw.shadow(r, px, py, pw, ph, 10, 24, Colors.fade(0x90000000, open));
        Draw.panel(r, px, py, pw, ph, 10, open);
        if (!tabs()) Draw.rounded(r, px, py, SIDEBAR, ph, 10, 0, 0, 10, Colors.fade(Theme.light ? 0x0C000000 : 0x40000000, open));
        Draw.roundedOutline(r, px, py, pw, ph, 10, 0.75f, Colors.fade(Theme.BORDER, open));

        if (tabs()) {
            drawTopBar(r, mx, my);
        } else {
            drawSidebar(r, mx, my);
            drawHeader(r, mx, my);
        }

        if (themesPage) {
            appearance.draw(r, contentX(), contentY(), contentW(), contentH(), mx, my, open);
            return;
        }
        float p = page.get();
        if (p < 0.999f) drawGrid(r, mx, my, p);
        if (p > 0.001f && selected != null) drawSettings(r, mx, my, p);
    }

    private Animation hover(Object key, boolean hot) {
        Animation a = hoverAnims.get(key);
        if (a == null) {
            a = new Animation(140, 0f);
            hoverAnims.put(key, a);
        }
        a.target(hot);
        return a;
    }

    private void drawSidebar(GuiRenderer r, float mx, float my) {
        FontRenderer big = Fonts.get(14f);
        String logo = "COOL";
        float lx = px + 14, ly = py + 14;
        for (int i = 0; i < logo.length(); i++) {
            String ch = String.valueOf(logo.charAt(i));
            big.draw(r, ch, lx, ly, Theme.accent(i / (float) (logo.length() - 1)), false);
            lx += big.width(ch) + 0.6f;
        }
        Fonts.small().draw(r, "CLIENT  " + "·" + "  " + CoolClient.get().platform().minecraftVersion(), px + 14, ly + 16, Theme.TEXT_FAINT, false);

        float y = py + 52;
        y = navItem(r, mx, my, "All Mods", Icons.Icon.GRID, null, y);
        for (Category c : Category.values()) {
            if (c == Category.CLIENT) continue;
            y = navItem(r, mx, my, c.displayName, iconFor(c), c, y);
        }
        y += 6;
        Draw.rect(r, px + 12, y - 3, SIDEBAR - 24, 0.5f, Theme.BORDER);
        y += 2;
        y = navItem(r, mx, my, "Cosmetics", Icons.Icon.HANGER, "cosmetics", y);
        y = navItem(r, mx, my, "Themes", Icons.Icon.PALETTE, THEMES, y);
        navItem(r, mx, my, "Settings", Icons.Icon.GEAR, Category.CLIENT, y);

        if (!showFooter) return;
        String prof = "Profile: " + CoolClient.get().config().activeProfile();
        Fonts.small().draw(r, Fonts.small().trim(prof, SIDEBAR - 24), px + 14, py + ph - 26, Theme.TEXT_DIM, false);
        Fonts.small().draw(r, "v" + CoolClient.VERSION, px + 14, py + ph - 15, Theme.TEXT_FAINT, false);
    }

    private static Icons.Icon iconFor(Category c) {
        switch (c) {
            case HUD: return Icons.Icon.LAYOUT;
            case COMBAT: return Icons.Icon.STAR;
            case VISUAL: return Icons.Icon.EYE;
            case MECHANIC: return Icons.Icon.KEYBOARD;
            case UTILITY: return Icons.Icon.SLIDERS;
            case PERFORMANCE: return Icons.Icon.SPARKLE;
            default: return Icons.Icon.GEAR;
        }
    }

    private float navItem(GuiRenderer r, float mx, float my, String label, Icons.Icon icon, Object key, float y) {
        float x = px + 8, w = SIDEBAR - 16, h = navH;
        boolean active = THEMES.equals(key) ? themesPage
                : !themesPage && (key == null ? category == null : key.equals(category));
        boolean hot = MathUtil.inside(mx, my, x, y, w, h);
        float hv = hover("nav:" + label, hot || active).get();
        if (hv > 0.01f) Draw.rounded(r, x, y, w, h, 5, Theme.highlight((int) ((active ? 22 : 12) * hv)));
        if (active) Draw.rounded(r, x, y + 3, 2.5f, h - 6, 1.25f, Theme.accent(0.3f));
        int c = active ? Theme.TEXT : Colors.lerp(Theme.TEXT_DIM, Theme.TEXT, hv);
        Icons.draw(r, icon, x + 8, y + (h - 9) / 2f, 9, active ? Theme.accent(0.2f) : c);
        Fonts.body().draw(r, label, x + 22, y + (h - Fonts.body().height()) / 2f, c, false);
        return y + h + 2;
    }

    private void drawHeader(GuiRenderer r, float mx, float my) {
        float x = contentX(), w = contentW();
        String title = themesPage ? "Themes" : selected != null && page.target() == 1f ? selected.name()
                : (search.text().isEmpty() ? (category == null ? "All Mods" : category.displayName) : "Search");
        float searchW = MathUtil.clamp(w - 70 - 4 - 90, 70, 150);
        float searchX = x + w - 70 - searchW;
        Fonts.title().draw(r, Fonts.title().trim(title, searchX - x - 8), x, py + 15, Theme.TEXT, false);
        search.setBounds(searchX, py + 11, searchW, 16);
        search.render(r, mx, my);
        editHud.setBounds(x + w - 64, py + 11, 64, 16);
        editHud.render(r, mx, my);
        Draw.rect(r, x, py + HEADER + 2, w, 0.5f, Theme.BORDER);
    }

    // ------------------------------------------------------------------ Tabs layout: tab bar and chips

    private void drawTopBar(GuiRenderer r, float mx, float my) {
        FontRenderer big = Fonts.get(13f);
        String logo = "COOL";
        float lx = px + 14, ly = py + 12;
        for (int i = 0; i < logo.length(); i++) {
            String ch = String.valueOf(logo.charAt(i));
            big.draw(r, ch, lx, ly, Theme.accent(i / (float) (logo.length() - 1)), false);
            lx += big.width(ch) + 0.6f;
        }
        float x = lx + 14, y = py + 9, h = 20;
        // On narrow windows the tabs drop their labels so they don't run into Edit HUD.
        FontRenderer bf = Fonts.body();
        float ew = bf.width("Edit HUD") + 30;
        float labelled = 4 * 35 + bf.width("Mods") + bf.width("Cosmetics") + bf.width("Themes") + bf.width("Settings");
        iconTabs = x + labelled > px + pw - 12 - h - 6 - ew - 8;
        boolean settingsOpen = !themesPage && page.target() == 1f && selected instanceof ClientSettings;
        x = tab(r, mx, my, "Mods", Icons.Icon.GRID, !themesPage && !settingsOpen, x, y, h, () -> {
            themesPage = false;
            if (page.target() == 1f) page.target(0f);
            if (category == Category.CLIENT) category = rememberedCategory;
        });
        x = tab(r, mx, my, "Cosmetics", Icons.Icon.HANGER, false, x, y, h,
                () -> CoolClient.get().platform().openScreen(new CosmeticsScreen(this)));
        x = tab(r, mx, my, "Themes", Icons.Icon.PALETTE, themesPage, x, y, h, this::openThemes);
        tab(r, mx, my, "Settings", Icons.Icon.GEAR, settingsOpen, x, y, h, () -> {
            themesPage = false;
            category = Category.CLIENT;
            openSettings(CoolClient.get().client());
        });

        // Right: Edit HUD and close.
        float cx = px + pw - 12 - h;
        boolean closeHot = MathUtil.inside(mx, my, cx, y, h, h);
        Draw.rounded(r, cx, y, h, h, 6, closeHot ? Colors.withAlpha(Theme.DANGER, 200) : Theme.SURFACE);
        Icons.draw(r, Icons.Icon.CLOSE, cx + 5, y + 5, 10, closeHot ? 0xFFFFFFFF : Theme.TEXT_DIM);
        navHits.add(new Hit(cx, y, h, h, this::close, null));
        tab(r, mx, my, "Edit HUD", Icons.Icon.LAYOUT, false, cx - 6 - ew, y, h, () -> {
            if (parent instanceof HudEditorScreen) close();
            else CoolClient.get().platform().openScreen(new HudEditorScreen(null));
        });
        Draw.rect(r, px + 12, py + 35, pw - 24, 0.5f, Theme.BORDER);

        // Second row: category chips and search on the grid; a title elsewhere.
        float ry = py + 41;
        if (themesPage || (selected != null && page.target() == 1f)) {
            Fonts.title().draw(r, themesPage ? "Themes" : selected.name(), contentX(), ry + 2, Theme.TEXT, false);
            return;
        }
        float searchW = Math.min(140, contentW() * 0.3f);
        search.setBounds(contentX() + contentW() - searchW, ry, searchW, 16);
        search.render(r, mx, my);
        float maxX = contentX() + contentW() - searchW - 8;
        boolean searching = !search.text().isEmpty();
        float cxx = chip(r, mx, my, "All", null, !searching && !favouritesOnly && category == null, contentX(), ry, maxX, () -> selectCategory(null));
        cxx = chip(r, mx, my, "Favourites", Icons.Icon.HEART, !searching && favouritesOnly, cxx, ry, maxX, () -> {
            selectCategory(null);
            favouritesOnly = true;
        });
        for (final Category c : Category.values()) {
            if (c == Category.CLIENT) continue;
            cxx = chip(r, mx, my, c.displayName, null, !searching && !favouritesOnly && category == c, cxx, ry, maxX, () -> selectCategory(c));
        }
    }

    private float tab(GuiRenderer r, float mx, float my, String label, Icons.Icon icon, boolean active,
                      float x, float y, float h, Runnable action) {
        FontRenderer f = Fonts.body();
        boolean iconOnly = iconTabs && !"Edit HUD".equals(label);
        float w = iconOnly ? 28 : f.width(label) + 30;
        boolean hot = MathUtil.inside(mx, my, x, y, w, h);
        float hv = hover("tab:" + label, hot).get();
        if (active) Draw.roundedGradient(r, x, y, w, h, 6, Theme.accent(0f), Theme.accent(1f), true);
        else Draw.rounded(r, x, y, w, h, 6, Colors.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, hv));
        int c = active ? Theme.ON_ACCENT : Colors.lerp(Theme.TEXT_DIM, Theme.TEXT, hv);
        Icons.draw(r, icon, x + 9, y + (h - 10) / 2f, 10, c);
        if (!iconOnly) f.draw(r, label, x + 23, y + (h - f.height()) / 2f, c, false);
        navHits.add(new Hit(x, y, w, h, action, null));
        return x + w + 5;
    }

    private float chip(GuiRenderer r, float mx, float my, String label, Icons.Icon icon, boolean active,
                       float x, float y, float maxX, Runnable action) {
        FontRenderer f = Fonts.small();
        float w = f.width(label) + 18 + (icon != null ? 11 : 0), h = 16;
        if (x + w > maxX) return x;
        boolean hot = MathUtil.inside(mx, my, x, y, w, h);
        float hv = hover("chip:" + label, hot).get();
        if (active) Draw.roundedGradient(r, x, y, w, h, 5, Theme.accent(0f), Theme.accent(1f), true);
        else Draw.rounded(r, x, y, w, h, 5, Colors.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, hv));
        int c = active ? Theme.ON_ACCENT : Colors.lerp(Theme.TEXT_DIM, Theme.TEXT, hv);
        float tx = x + 9;
        if (icon != null) {
            Icons.draw(r, icon, tx - 1, y + 4, 8, active ? Theme.ON_ACCENT : 0xFFFF4F7A);
            tx += 11;
        }
        f.draw(r, label, tx, y + (h - f.height()) / 2f + 0.5f, c, false);
        navHits.add(new Hit(x, y, w, h, action, null));
        return x + w + 4;
    }

    // ------------------------------------------------------------------ favourites (Tabs layout)

    private static List<String> favourites() {
        List<String> out = new ArrayList<String>();
        for (String s : CoolClient.get().client().favouriteMods.get().split(",")) if (!s.trim().isEmpty()) out.add(s.trim());
        return out;
    }

    private static boolean favourite(Module m) {
        return favourites().contains(m.name());
    }

    private static void toggleFavourite(Module m) {
        List<String> f = favourites();
        if (!f.remove(m.name())) f.add(m.name());
        StringBuilder sb = new StringBuilder();
        for (String s : f) sb.append(sb.length() == 0 ? "" : ",").append(s);
        CoolClient.get().client().favouriteMods.set(sb.toString());
        CoolClient.get().config().markDirty();
    }

    // ------------------------------------------------------------------ module grid

    private List<Module> visibleModules() {
        List<Module> out = new ArrayList<Module>();
        String q = search.text().trim().toLowerCase(Locale.ROOT);
        for (Module m : CoolClient.get().modules().all()) {
            if (m instanceof ClientSettings) continue;
            if (!q.isEmpty()) {
                if (m.name().toLowerCase(Locale.ROOT).contains(q) || m.description().toLowerCase(Locale.ROOT).contains(q)) out.add(m);
            } else if (favouritesOnly ? favourite(m) : category == null || m.category() == category) {
                out.add(m);
            }
        }
        if (tabs()) {
            // Favourites first.
            List<Module> fav = new ArrayList<Module>(), rest = new ArrayList<Module>();
            List<String> names = favourites();
            for (Module m : out) (names.contains(m.name()) ? fav : rest).add(m);
            fav.addAll(rest);
            return fav;
        }
        return out;
    }

    private int columns() {
        if (style == Layout.COMPACT) return 1;
        if (tabs()) return Math.max(3, (int) (contentW() / 122f));
        return Math.max(2, (int) (contentW() / 128f));
    }

    private float itemH() { return tabs() ? 96 : style == Layout.COMPACT ? 24 : 74; }

    private float itemGap() { return style == Layout.COMPACT ? 3 : 7; }

    /** A card's main click: its settings if it has any, otherwise on/off. */
    private void activate(Module m) {
        if (!m.settings().isEmpty() || m.bindable()) openSettings(m);
        else if (m.toggleable()) m.toggle();
    }

    private static Runnable toggler(final Module m) {
        return m.toggleable() ? m::toggle : null;
    }

    private float toggleAmount(Module m) {
        Animation ta = toggleAnims.get(m);
        if (ta == null) {
            ta = new Animation(220, m.isEnabled() ? 1f : 0f);
            toggleAnims.put(m, ta);
        }
        ta.target(m.isEnabled());
        return ta.get();
    }

    private void drawGrid(GuiRenderer r, float mx, float my, float p) {
        float alpha = 1f - p;
        float shift = -p * 40f;
        float x0 = contentX() + shift, y0 = contentY(), w = contentW(), h = contentH();
        List<Module> mods = visibleModules();
        int cols = columns();
        float gap = itemGap();
        float cw = (w - gap * (cols - 1)) / cols, ch = itemH();
        int rowsN = (mods.size() + cols - 1) / cols;
        gridScroll.setBounds(contentX(), y0, w, h);
        gridScroll.setContentHeight(rowsN * (ch + gap) - gap);
        Scissor.push(r, contentX() - 2, y0 - 2, w + 4, h + 4);
        float off = gridScroll.offset();
        for (int i = 0; i < mods.size(); i++) {
            Module m = mods.get(i);
            float cx = x0 + (i % cols) * (cw + gap);
            float cy = y0 + (i / cols) * (ch + gap) - off;
            if (cy + ch < y0 - 4 || cy > y0 + h + 4) continue;
            if (tabs()) drawTile(r, m, cx, cy, cw, ch, mx, my, alpha);
            else if (style == Layout.COMPACT) drawRow(r, m, cx, cy, cw, ch, mx, my, alpha);
            else drawCard(r, m, cx, cy, cw, ch, mx, my, alpha);
        }
        if (mods.isEmpty()) {
            String empty = favouritesOnly && search.text().isEmpty() ? "No favourites yet: tap the heart on a mod"
                    : "No mods match \"" + search.text() + "\"";
            Fonts.body().drawCentered(r, empty, contentX() + w / 2f, y0 + 40, Theme.TEXT_DIM, false);
        }
        Scissor.pop(r);
        if (p < 0.01f) gridScroll.update(r, mx, my, CoolClient.get().platform().input().isMouseDown(0));
    }

    private void drawCard(GuiRenderer r, Module m, float x, float y, float w, float h, float mx, float my, float alpha) {
        boolean hot = MathUtil.inside(mx, my, x, y, w, h) && gridScroll.contains(mx, my) && page.target() == 0f;
        float hv = hover(m, hot).get();
        float on = toggleAmount(m);
        if (page.target() == 0f) gridHits.add(new Hit(x, y, w, h, () -> activate(m), toggler(m)));

        float lift = hv * 1.5f;
        y -= lift;
        if (hv > 0.01f) Draw.shadow(r, x, y, w, h, 7, 10, Colors.fade(0x50000000, hv * alpha));
        Draw.rounded(r, x, y, w, h, 7, Colors.fade(Colors.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, hv), alpha));
        int border = Colors.lerp(Theme.BORDER, Colors.withAlpha(Theme.accent(0.3f), 120), on * 0.8f);
        Draw.roundedOutline(r, x, y, w, h, 7, 0.75f, Colors.fade(border, alpha));

        // Icon tile
        int cc = m.category().color;
        Draw.roundedGradient(r, x + 8, y + 8, 22, 22, 5, Colors.fade(cc, alpha), Colors.fade(Colors.brighten(cc, 0.55f), alpha), false);
        String initials = initials(m.name());
        Fonts.get(9f).drawCentered(r, initials, x + 19, y + 14.5f, Colors.fade(0xFFFFFFFF, alpha), false);

        FontRenderer nf = Fonts.body();
        float nameMax = w - 44 - (m.versionTag() != null ? 22 : 0);
        nf.draw(r, nf.trim(m.name(), nameMax), x + 36, y + 9, Colors.fade(Theme.TEXT, alpha), false);
        if (m.versionTag() != null) {
            float bx = x + 36 + Math.min(nf.width(m.name()), nameMax) + 4;
            Draw.rounded(r, bx, y + 9, 20, 9, 3, Colors.fade(Colors.withAlpha(cc, 60), alpha));
            Fonts.get(6f).drawCentered(r, m.versionTag(), bx + 10, y + 10.5f, Colors.fade(cc, alpha), false);
        }
        String sub = m.isBlocked() ? "Blocked by server" : m.category().displayName;
        Fonts.small().draw(r, sub, x + 36, y + 20, Colors.fade(m.isBlocked() ? Theme.WARNING : Theme.TEXT_FAINT, alpha), false);

        List<String> desc = wrap(Fonts.small(), m.description(), w - 16);
        for (int i = 0; i < Math.min(2, desc.size()); i++) {
            Fonts.small().draw(r, desc.get(i), x + 8, y + 35 + i * 9, Colors.fade(Theme.TEXT_DIM, alpha), false);
        }

        // Bottom strip: toggle + gear
        float sy = y + h - 16;
        boolean hasSettings = !m.settings().isEmpty() || m.bindable();
        float gearW = hasSettings ? 18 : 0;
        boolean stripHot = hot && MathUtil.inside(mx, my, x, sy, w - gearW, 16);
        if (page.target() == 0f && m.toggleable()) gridHits.add(new Hit(x, sy, w - gearW, 16, m::toggle, m::toggle));
        int off = Colors.lerp(0xFF3A2230, 0xFF4A2A3A, stripHot ? 1 : 0);
        int onC = Colors.lerp(0xFF1D5A4A, 0xFF23705C, stripHot ? 1 : 0);
        int strip = m.toggleable() ? Colors.lerp(off, onC, on) : Theme.SURFACE_ACTIVE;
        Draw.rounded(r, x, sy, w, 16, 0, 0, 7, 7, Colors.fade(strip, alpha));
        String label = !m.toggleable() ? "OPEN" : (m.isEnabledRaw() ? "ENABLED" : "DISABLED");
        int tc = !m.toggleable() ? Theme.TEXT : Colors.lerp(0xFFFF8FA3, 0xFF7CF5D0, on);
        Fonts.get(7f).drawCentered(r, label, x + (w - gearW) / 2f, sy + 4.5f, Colors.fade(tc, alpha), false);
        if (hasSettings) {
            boolean gh = hot && MathUtil.inside(mx, my, x + w - gearW, sy, gearW, 16);
            Draw.rect(r, x + w - gearW, sy + 3, 0.5f, 10, Colors.fade(Theme.highlight(48), alpha));
            Icons.draw(r, Icons.Icon.GEAR, x + w - gearW + 4.5f, sy + 3.5f, 9, Colors.fade(gh ? Theme.TEXT : Theme.TEXT_DIM, alpha));
        }
    }

    /** Tabs layout: a big tile with a heart, a large badge, a gear and an on/off pill. */
    private void drawTile(GuiRenderer r, final Module m, float x, float y, float w, float h, float mx, float my, float alpha) {
        boolean live = page.target() == 0f;
        boolean hot = MathUtil.inside(mx, my, x, y, w, h) && gridScroll.contains(mx, my) && live;
        float hv = hover(m, hot).get();
        float on = toggleAmount(m);
        y -= hv * 1.5f;
        if (hv > 0.01f) Draw.shadow(r, x, y, w, h, 8, 10, Colors.fade(0x50000000, hv * alpha));
        Draw.rounded(r, x, y, w, h, 8, Colors.fade(Colors.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, hv), alpha));
        int border = Colors.lerp(Theme.BORDER, Colors.withAlpha(Theme.accent(0.3f), 110), on * 0.7f);
        Draw.roundedOutline(r, x, y, w, h, 8, 0.75f, Colors.fade(border, alpha));
        if (live) gridHits.add(new Hit(x, y, w, h, () -> activate(m), toggler(m)));

        FontRenderer nf = Fonts.body();
        nf.draw(r, nf.trim(m.name(), w - 34), x + 9, y + 8, Colors.fade(Theme.TEXT, alpha), false);
        boolean fav = favourite(m);
        float hx = x + w - 21;
        boolean heartHot = hot && MathUtil.inside(mx, my, hx - 2, y + 4, 16, 14);
        Icons.draw(r, Icons.Icon.HEART, hx, y + 6, 11, Colors.fade(fav ? 0xFFFF4F7A : heartHot ? Theme.TEXT_DIM : Theme.TEXT_FAINT, alpha));
        if (live) gridHits.add(new Hit(hx - 2, y + 4, 16, 14, () -> toggleFavourite(m), () -> toggleFavourite(m)));

        // Big badge in the middle.
        int cc = m.category().color;
        float bs = 30, bx = x + (w - bs) / 2f, by = y + 25;
        Draw.shadow(r, bx, by + 2, bs, bs, 8, 8, Colors.fade(Colors.withAlpha(cc, 60), alpha));
        Draw.roundedGradient(r, bx, by, bs, bs, 8, Colors.fade(cc, alpha), Colors.fade(Colors.brighten(cc, 0.55f), alpha), false);
        Fonts.get(11f).drawCentered(r, initials(m.name()), bx + bs / 2f, by + bs / 2f - 5.5f, Colors.fade(0xFFFFFFFF, alpha), false);
        if (m.isBlocked()) Fonts.small().drawCentered(r, "Blocked by server", x + w / 2f, by + bs + 3, Colors.fade(Theme.WARNING, alpha), false);

        // Bottom row: gear and the on/off pill.
        boolean hasSettings = !m.settings().isEmpty() || m.bindable();
        float rowY = y + h - 22, pillX = x + 7;
        if (hasSettings) {
            boolean gh = hot && MathUtil.inside(mx, my, x + 7, rowY, 18, 16);
            Draw.rounded(r, x + 7, rowY, 18, 16, 4, Colors.fade(gh ? Theme.SURFACE_ACTIVE : Theme.highlight(14), alpha));
            Icons.draw(r, Icons.Icon.GEAR, x + 11.5f, rowY + 3.5f, 9, Colors.fade(gh ? Theme.TEXT : Theme.TEXT_DIM, alpha));
            if (live) gridHits.add(new Hit(x + 7, rowY, 18, 16, () -> openSettings(m), null));
            pillX += 22;
        }
        float pw0 = x + w - 7 - pillX;
        boolean pillHot = hot && MathUtil.inside(mx, my, pillX, rowY, pw0, 16);
        int offC = Colors.lerp(Theme.SURFACE_ACTIVE, Theme.SURFACE_HOVER, pillHot ? 1 : 0);
        int onC = Colors.lerp(0xFF1E6A4A, 0xFF24805A, pillHot ? 1 : 0);
        Draw.rounded(r, pillX, rowY, pw0, 16, 4, Colors.fade(m.toggleable() ? Colors.lerp(offC, onC, on) : Theme.SURFACE_ACTIVE, alpha));
        String label = !m.toggleable() ? "Open" : m.isEnabledRaw() ? "Enabled" : "Disabled";
        int tc = !m.toggleable() ? Theme.TEXT : Colors.lerp(Theme.TEXT_DIM, 0xFFB9F6D8, on);
        Fonts.get(7.5f).drawCentered(r, label, pillX + pw0 / 2f, rowY + 4.5f, Colors.fade(tc, alpha), false);
        if (live) gridHits.add(new Hit(pillX, rowY, pw0, 16, m.toggleable() ? m::toggle : () -> activate(m), toggler(m)));
    }

    /** Compact layout: one row per mod with a switch. */
    private void drawRow(GuiRenderer r, final Module m, float x, float y, float w, float h, float mx, float my, float alpha) {
        boolean live = page.target() == 0f;
        boolean hot = MathUtil.inside(mx, my, x, y, w, h) && gridScroll.contains(mx, my) && live;
        float hv = hover(m, hot).get();
        float on = toggleAmount(m);
        Draw.rounded(r, x, y, w, h, 5, Colors.fade(Colors.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, hv), alpha));
        if (on > 0.01f) Draw.rounded(r, x + 1.5f, y + 5, 2.5f, h - 10, 1.25f, Colors.fade(Colors.withAlpha(Theme.accent(0.3f), (int) (255 * on)), alpha));
        if (live) gridHits.add(new Hit(x, y, w, h, () -> activate(m), toggler(m)));

        int cc = m.category().color;
        Draw.roundedGradient(r, x + 8, y + 4, 16, 16, 4, Colors.fade(cc, alpha), Colors.fade(Colors.brighten(cc, 0.55f), alpha), false);
        Fonts.get(6.5f).drawCentered(r, initials(m.name()), x + 16, y + 8.5f, Colors.fade(0xFFFFFFFF, alpha), false);
        FontRenderer nf = Fonts.body();
        float ty = y + (h - nf.height()) / 2f;
        float nx = x + 30;
        float rightEdge = x + w - 64;
        String name = nf.trim(m.name(), Math.max(40, (rightEdge - nx) * 0.4f));
        nf.draw(r, name, nx, ty, Colors.fade(Theme.TEXT, alpha), false);
        nx += nf.width(name) + 8;
        String cat = m.isBlocked() ? "Blocked by server" : m.category().displayName;
        Fonts.small().draw(r, cat, nx, ty + 1.5f, Colors.fade(m.isBlocked() ? Theme.WARNING : Theme.TEXT_FAINT, alpha), false);
        nx += Fonts.small().width(cat) + 10;
        if (rightEdge - nx > 30) {
            Fonts.small().draw(r, Fonts.small().trim(m.description(), rightEdge - nx), nx, ty + 1.5f, Colors.fade(Theme.TEXT_DIM, alpha), false);
        }

        boolean hasSettings = !m.settings().isEmpty() || m.bindable();
        if (hasSettings) {
            boolean gh = hot && MathUtil.inside(mx, my, x + w - 22, y, 20, h);
            Icons.draw(r, Icons.Icon.GEAR, x + w - 17, y + (h - 9) / 2f, 9, Colors.fade(gh ? Theme.TEXT : Theme.TEXT_FAINT, alpha));
            if (live) gridHits.add(new Hit(x + w - 22, y, 20, h, () -> openSettings(m), null));
        }
        if (m.toggleable()) {
            float sx = x + w - 52, sy = y + (h - 11) / 2f;
            Draw.rounded(r, sx, sy, 22, 11, 5.5f, Colors.fade(Colors.lerp(Theme.SURFACE_ACTIVE, Theme.accent(0.3f), on), alpha));
            Draw.circle(r, sx + 5.5f + on * 11f, sy + 5.5f, 4f, Colors.fade(on > 0.5f ? Theme.ON_ACCENT : Theme.TEXT_DIM, alpha));
            if (live) gridHits.add(new Hit(sx - 4, y, 30, h, m::toggle, m::toggle));
        }
    }

    private static String initials(String name) {
        String[] parts = name.replace("1.7", "Old").split("[ \\-]+");
        if (parts.length >= 2) return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase(Locale.ROOT);
        return name.length() <= 3 ? name.toUpperCase(Locale.ROOT) : name.substring(0, 2).toUpperCase(Locale.ROOT);
    }

    static List<String> wrap(FontRenderer f, String text, float maxW) {
        List<String> lines = new ArrayList<String>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String test = line.length() == 0 ? word : line + " " + word;
            if (f.width(test) > maxW && line.length() > 0) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(test);
            }
        }
        if (line.length() > 0) lines.add(line.toString());
        return lines;
    }

    // ------------------------------------------------------------------ settings page

    private void openSettings(final Module m) {
        selected = m;
        rows.clear();
        settingsScroll.reset();
        if (m.bindable() && m.toggleable()) {
            rows.add(new Row(null, "Keybind", "Press to toggle this mod", new KeyBinder(m.keybind())));
        }
        for (Setting<?> s : m.settings()) {
            Widget w = SettingWidgets.create(s);
            if (w != null) rows.add(new Row(s, s.name(), s.description(), w));
        }
        if (m instanceof ClientSettings) rows.add(new Row(null, "Profiles", "", new ProfilesWidget()));
        headerToggle = m.toggleable() ? new Toggle(m::isEnabledRaw, v -> m.setEnabled(v)) : null;
        resetButton = new Button("Reset", () -> {
            m.resetSettings();
            if (m instanceof dev.coolclient.core.hud.HudElement) ((dev.coolclient.core.hud.HudElement) m).resetPosition();
            CoolClient.get().config().markDirty();
            openSettings(m);
        }).icon(Icons.Icon.RESET);
        page.target(1f);
    }

    private void drawSettings(GuiRenderer r, float mx, float my, float p) {
        float x = contentX() + (1f - p) * 60f, y0 = contentY(), w = contentW(), h = contentH();
        float alpha = p;
        Scissor.push(r, contentX() - 2, y0 - 2, w + 4, h + 4);

        // Module header card
        boolean backHot = MathUtil.inside(mx, my, x, y0, 18, 18);
        Draw.rounded(r, x, y0, 18, 18, 5, Colors.fade(backHot ? Theme.SURFACE_HOVER : Theme.SURFACE, alpha));
        Icons.draw(r, Icons.Icon.CHEVRON_LEFT, x + 4, y0 + 4, 10, Colors.fade(Theme.TEXT, alpha));
        float descMax = w - 26 - (headerToggle != null ? 66 + 22 + 8 : 58 + 8);
        Fonts.small().draw(r, Fonts.small().trim(selected.description(), descMax), x + 26, y0 + 5,
                Colors.fade(Theme.TEXT_DIM, alpha), false);
        if (headerToggle != null) {
            headerToggle.setBounds(x + w - 22 - 66, y0 + 3.5f, 22, 11);
            headerToggle.render(r, mx, my);
        }
        resetButton.setBounds(x + w - 58, y0, 58, 18);
        resetButton.render(r, mx, my);

        float listY = y0 + 26;
        float listH = h - 26;
        settingsScroll.setBounds(contentX(), listY, w, listH);
        Scissor.push(r, contentX() - 2, listY, w + 4, listH);
        float yy = listY - settingsScroll.offset();
        float total = 0;
        for (Row row : rows) {
            if (row.setting != null && !row.setting.visible()) continue;
            Widget wd = row.widget;
            boolean wide = wd instanceof ProfilesWidget;
            float rowH = Math.max(22, wd.preferredHeight() + 9);
            if (wide) rowH = wd.preferredHeight() + 20;
            boolean hot = MathUtil.inside(mx, my, x, yy, w, rowH) && settingsScroll.contains(mx, my);
            float hv = hover(row, hot).get();
            Draw.rounded(r, x, yy, w - 6, rowH - 3, 5, Colors.fade(Theme.highlight((int) (12 * hv)), alpha));
            FontRenderer lf = Fonts.body();
            float labelY = yy + (Math.min(rowH, 22) - lf.height()) / 2f - 1.5f;
            lf.draw(r, row.label, x + 8, labelY, Colors.fade(Theme.TEXT, alpha), false);
            if (!row.description.isEmpty() && !wide) {
                Fonts.small().draw(r, Fonts.small().trim(row.description, w * 0.5f), x + 8, labelY + lf.height() + 1.5f,
                        Colors.fade(Theme.TEXT_FAINT, alpha), false);
                if (rowH < 30) rowH = 30;
            }
            if (wide) {
                wd.setBounds(x + 8, yy + 18, w - 22, wd.preferredHeight());
            } else {
                float ww = wd.w;
                wd.setBounds(x + w - 14 - ww, yy + 5.5f, ww, wd.h);
            }
            wd.render(r, mx, my);
            yy += rowH;
            total += rowH;
        }
        Scissor.pop(r);
        settingsScroll.setContentHeight(total + 4);
        settingsScroll.update(r, mx, my, CoolClient.get().platform().input().isMouseDown(0));
        Scissor.pop(r);
    }

    // ================================================================== input

    @Override
    public boolean mouseClicked(float mx, float my, int button) {
        boolean searchShown = !tabs() || !(themesPage || (selected != null && page.target() == 1f));
        if (searchShown && search.mouseClicked(mx, my, button)) return true;
        if (tabs()) {
            if (click(navHits, mx, my, button)) return true;
        } else if (editHud.mouseClicked(mx, my, button)) {
            return true;
        }

        // Sidebar
        if (!tabs() && MathUtil.inside(mx, my, px, py, SIDEBAR, ph)) {
            float y = py + 52;
            List<Object> keys = new ArrayList<Object>();
            keys.add(null);
            for (Category c : Category.values()) if (c != Category.CLIENT) keys.add(c);
            for (Object key : keys) {
                if (MathUtil.inside(mx, my, px + 8, y, SIDEBAR - 16, navH)) {
                    selectCategory((Category) key);
                    return true;
                }
                y += navH + 2;
            }
            y += 8;
            if (MathUtil.inside(mx, my, px + 8, y, SIDEBAR - 16, navH)) {
                CoolClient.get().platform().playClickSound();
                CoolClient.get().platform().openScreen(new CosmeticsScreen(this));
                return true;
            }
            y += navH + 2;
            if (MathUtil.inside(mx, my, px + 8, y, SIDEBAR - 16, navH)) {
                CoolClient.get().platform().playClickSound();
                openThemes();
                return true;
            }
            y += navH + 2;
            if (MathUtil.inside(mx, my, px + 8, y, SIDEBAR - 16, navH)) {
                CoolClient.get().platform().playClickSound();
                themesPage = false;
                category = Category.CLIENT;
                openSettings(CoolClient.get().client());
                return true;
            }
            return true;
        }

        if (themesPage) return appearance.mouseClicked(mx, my, button);

        if (page.target() == 1f && selected != null) {
            float x = contentX(), y0 = contentY();
            if (MathUtil.inside(mx, my, x, y0, 18, 18)) {
                CoolClient.get().platform().playClickSound();
                page.target(0f);
                if (selected instanceof ClientSettings) category = rememberedCategory;
                return true;
            }
            if (headerToggle != null && headerToggle.mouseClicked(mx, my, button)) return true;
            if (resetButton.mouseClicked(mx, my, button)) return true;
            if (settingsScroll.mouseClicked(mx, my)) return true;
            if (!settingsScroll.contains(mx, my)) return false;
            for (Row row : rows) {
                if (row.setting != null && !row.setting.visible()) continue;
                if (row.widget.mouseClicked(mx, my, button)) return true;
            }
            return false;
        }

        // Grid
        if (gridScroll.mouseClicked(mx, my)) return true;
        if (!gridScroll.contains(mx, my)) return false;
        return click(gridHits, mx, my, button);
    }

    /** Runs the topmost recorded area under the mouse (areas drawn later sit on top). */
    private static boolean click(List<Hit> hits, float mx, float my, int button) {
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (!MathUtil.inside(mx, my, h.x, h.y, h.w, h.h)) continue;
            Runnable action = button == 1 ? h.right : button == 0 ? h.left : null;
            if (action != null) {
                CoolClient.get().platform().playClickSound();
                action.run();
            }
            return true;
        }
        return false;
    }

    private void selectCategory(Category c) {
        CoolClient.get().platform().playClickSound();
        themesPage = false;
        favouritesOnly = false;
        category = c;
        rememberedCategory = c;
        search.setText("");
        gridScroll.reset();
        page.target(0f);
    }

    @Override
    public boolean mouseReleased(float mx, float my, int button) {
        if (page.target() == 1f) {
            for (Row row : rows) if (row.widget.mouseReleased(mx, my, button)) return true;
        }
        return false;
    }

    @Override
    public boolean blursWorld() { return true; }

    @Override
    public boolean mouseScrolled(float mx, float my, double amount) {
        if (themesPage) return appearance.mouseScrolled(mx, my, amount);
        if (page.target() == 1f) {
            for (Row row : rows) {
                if (row.setting != null && !row.setting.visible()) continue;
                if (row.widget.mouseScrolled(mx, my, amount)) return true;
            }
            return settingsScroll.scroll(mx, my, amount);
        }
        return gridScroll.scroll(mx, my, amount);
    }

    @Override
    public boolean keyPressed(int key, int mods) {
        if (page.target() == 1f) {
            for (Row row : rows) {
                if (row.widget.capturesKeyboard()) return row.widget.keyPressed(key, mods);
            }
        }
        if (search.capturesKeyboard()) return search.keyPressed(key, mods);
        if (key == dev.coolclient.core.util.Keys.ESCAPE && page.target() == 1f && !(parent instanceof HudEditorScreen && selected instanceof ClientSettings)) {
            page.target(0f);
            return true;
        }
        if (key == dev.coolclient.core.util.Keys.F && (mods & 2) != 0) {
            search.setFocused(true);
            return true;
        }
        return super.keyPressed(key, mods);
    }

    @Override
    public boolean charTyped(char c) {
        if (page.target() == 1f) {
            for (Row row : rows) if (row.widget.capturesKeyboard()) return row.widget.charTyped(c);
        }
        if (!search.focused() && page.target() == 0f && Character.isLetterOrDigit(c)) {
            search.setFocused(true);
            themesPage = false;
        }
        return search.charTyped(c);
    }

    @Override
    public void onClose() {
        CoolClient.get().config().markDirty();
    }

    // ================================================================== profiles

    /** Profile list with load/delete and a create field, shown on the Client page. */
    private static final class ProfilesWidget extends Widget {
        private final TextField name = new TextField().placeholder("New profile name").maxLength(24);
        private final Button create;

        ProfilesWidget() {
            create = new Button("Create", () -> {
                if (!name.text().trim().isEmpty()) {
                    CoolClient.get().config().createProfile(name.text());
                    CoolClient.get().notifications().info("Profile created", "Now using \"" + CoolClient.get().config().activeProfile() + "\"");
                    name.setText("");
                }
            }).icon(Icons.Icon.PLUS);
        }

        @Override
        public float preferredHeight() {
            return CoolClient.get().config().profiles().size() * 18 + 22;
        }

        @Override
        protected void draw(GuiRenderer r, float mx, float my) {
            List<String> profiles = CoolClient.get().config().profiles();
            String active = CoolClient.get().config().activeProfile();
            float yy = y;
            for (String p : profiles) {
                boolean isActive = p.equals(active);
                Draw.rounded(r, x, yy, w, 15, 4, isActive ? Colors.withAlpha(Theme.accent(0.3f), 40) : Theme.SURFACE);
                Fonts.body().draw(r, p, x + 7, yy + 3.5f, Theme.TEXT, false);
                if (isActive) {
                    Fonts.small().drawRight(r, "ACTIVE", x + w - 8, yy + 4.5f, Theme.accent(0.3f), false);
                } else {
                    boolean lh = MathUtil.inside(mx, my, x + w - 60, yy, 34, 15);
                    Fonts.small().drawRight(r, "LOAD", x + w - 28, yy + 4.5f, lh ? Theme.TEXT : Theme.TEXT_DIM, false);
                    boolean dh = MathUtil.inside(mx, my, x + w - 20, yy, 18, 15);
                    Icons.draw(r, Icons.Icon.CLOSE, x + w - 15, yy + 3.5f, 8, dh ? Theme.DANGER : Theme.TEXT_FAINT);
                }
                yy += 18;
            }
            name.setBounds(x, yy + 2, w - 72, 16);
            name.render(r, mx, my);
            create.setBounds(x + w - 66, yy + 2, 66, 16);
            create.render(r, mx, my);
        }

        @Override
        public boolean mouseClicked(float mx, float my, int button) {
            if (name.mouseClicked(mx, my, button)) return true;
            if (create.mouseClicked(mx, my, button)) return true;
            List<String> profiles = CoolClient.get().config().profiles();
            String active = CoolClient.get().config().activeProfile();
            float yy = y;
            for (String p : profiles) {
                if (!p.equals(active)) {
                    if (MathUtil.inside(mx, my, x + w - 60, yy, 34, 15)) {
                        CoolClient.get().config().switchProfile(p);
                        CoolClient.get().client().apply();
                        CoolClient.get().notifications().info("Profile loaded", p);
                        return true;
                    }
                    if (MathUtil.inside(mx, my, x + w - 20, yy, 18, 15)) {
                        CoolClient.get().config().deleteProfile(p);
                        return true;
                    }
                }
                yy += 18;
            }
            return false;
        }

        @Override
        public boolean capturesKeyboard() { return name.capturesKeyboard(); }

        @Override
        public boolean keyPressed(int key, int mods) { return name.keyPressed(key, mods); }

        @Override
        public boolean charTyped(char c) { return name.charTyped(c); }
    }
}
