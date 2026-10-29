package dev.coolclient.launcher;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.api.GuiRenderer;
import dev.coolclient.core.font.FontRenderer;
import dev.coolclient.core.font.Fonts;
import dev.coolclient.core.gui.UIScreen;
import dev.coolclient.core.gui.screen.CosmeticsScreen;
import dev.coolclient.core.gui.screen.MenuBackground;
import dev.coolclient.core.gui.widget.Button;
import dev.coolclient.core.gui.widget.ModeSelector;
import dev.coolclient.core.gui.widget.ScrollArea;
import dev.coolclient.core.gui.widget.Slider;
import dev.coolclient.core.gui.widget.TextField;
import dev.coolclient.core.gui.widget.Toggle;
import dev.coolclient.core.gui.widget.Widget;
import dev.coolclient.core.render.Animation;
import dev.coolclient.core.render.Colors;
import dev.coolclient.core.render.Draw;
import dev.coolclient.core.render.Easing;
import dev.coolclient.core.render.Icons;
import dev.coolclient.core.render.Scissor;
import dev.coolclient.core.render.Theme;
import dev.coolclient.core.setting.ModeSetting;
import dev.coolclient.core.setting.NumberSetting;
import dev.coolclient.core.util.Keys;
import dev.coolclient.core.util.MathUtil;
import dev.coolclient.core.util.Time;
import dev.coolclient.launcher.core.Accounts;
import dev.coolclient.launcher.core.LauncherInfo;
import dev.coolclient.launcher.core.LauncherPaths;
import dev.coolclient.launcher.core.LauncherSettings;

import java.util.ArrayList;
import java.util.List;

/** The launcher: title bar, sidebar, and the Play / Settings / Console views plus the account dialog. */
final class LauncherScreen extends UIScreen {
    static final float TITLE_H = 20;
    private static final float SIDEBAR_W = 118;

    enum Tab { PLAY, SETTINGS, CONSOLE }

    private final LauncherController c;
    private final LauncherApp.WindowControls window;
    private Tab tab = Tab.PLAY;
    private final Animation tabAnim = new Animation(220, Easing.OUT_CUBIC, 1f);

    private final Animation accountsAnim = new Animation(200, Easing.OUT_CUBIC, 0f);
    private boolean accountsOpen;
    private final TextField offlineName = new TextField().placeholder("Offline username").maxLength(16);

    // Settings widgets
    private final List<Widget> settingsWidgets = new ArrayList<Widget>();
    private final NumberSetting memory = new NumberSetting("Memory", 3072, 1024, 16384, 512).suffix(" MB");
    private final ModeSetting afterLaunch = new ModeSetting("After launching", "Minimize launcher", "Minimize launcher", "Keep open", "Hide launcher");
    private final Slider memorySlider = new Slider(memory);
    private final TextField widthField = new TextField().maxLength(5);
    private final TextField heightField = new TextField().maxLength(5);
    private final TextField jvmField = new TextField().placeholder("-XX:+AlwaysPreTouch").maxLength(300);
    private final TextField javaField = new TextField().placeholder("Bundled Java (recommended)").maxLength(300);
    private final TextField clientIdField = new TextField().placeholder("00000000-0000-0000-0000-000000000000").maxLength(64);
    private final Toggle fullscreen;
    private final ModeSelector afterSelector = new ModeSelector(afterLaunch);
    private final ModeSelector themeSelector = new ModeSelector(CoolClient.get().client().theme);
    private final ModeSelector backgroundSelector = new ModeSelector(CoolClient.get().client().menuBackground);
    private final Toggle glassToggle = new Toggle(CoolClient.get().client().glassPanels::on, v -> {
        CoolClient.get().client().glassPanels.set(v);
        CoolClient.get().config().markDirty();
    });
    private final List<Button> folderButtons = new ArrayList<Button>();

    // Console
    private final ScrollArea logScroll = new ScrollArea();
    private int seenLogVersion = -1;
    private boolean followLog = true;

    LauncherScreen(LauncherController controller, LauncherApp.WindowControls window) {
        super(null);
        this.c = controller;
        this.window = window;
        LauncherSettings s = c.settings;
        memory.set((double) s.memoryMb);
        memory.onChange(() -> {
            s.memoryMb = memory.i();
            s.save();
        });
        widthField.setText(String.valueOf(s.width));
        heightField.setText(String.valueOf(s.height));
        jvmField.setText(s.jvmArgs);
        javaField.setText(s.javaPath);
        clientIdField.setText(s.msClientId);
        widthField.onChange(t -> {
            s.width = parse(t, s.width);
            s.save();
        });
        heightField.onChange(t -> {
            s.height = parse(t, s.height);
            s.save();
        });
        jvmField.onChange(t -> {
            s.jvmArgs = t;
            s.save();
        });
        javaField.onChange(t -> {
            s.javaPath = t.trim();
            s.save();
        });
        clientIdField.onChange(t -> {
            s.msClientId = t.trim();
            s.save();
        });
        fullscreen = new Toggle(() -> s.fullscreen, v -> {
            s.fullscreen = v;
            s.save();
        });
        afterLaunch.set(s.afterLaunch.equals("keep") ? "Keep open" : s.afterLaunch.equals("close") ? "Hide launcher" : "Minimize launcher");
        afterLaunch.onChange(() -> {
            s.afterLaunch = afterLaunch.is("Keep open") ? "keep" : afterLaunch.is("Hide launcher") ? "close" : "minimize";
            s.save();
        });
        offlineName.onSubmit(this::submitOffline);
        folderButtons.add(new Button("Game files", () -> CoolClient.get().platform().openFolder(LauncherPaths.BASE)).icon(Icons.Icon.FOLDER));
        folderButtons.add(new Button("1.8.9", () -> CoolClient.get().platform().openFolder(LauncherPaths.instance("1.8.9"))).icon(Icons.Icon.FOLDER));
        folderButtons.add(new Button("1.21.4", () -> CoolClient.get().platform().openFolder(LauncherPaths.instance("1.21.4"))).icon(Icons.Icon.FOLDER));
        settingsWidgets.add(memorySlider);
        settingsWidgets.add(widthField);
        settingsWidgets.add(heightField);
        settingsWidgets.add(fullscreen);
        settingsWidgets.add(jvmField);
        settingsWidgets.add(javaField);
        settingsWidgets.add(afterSelector);
        settingsWidgets.add(clientIdField);
        settingsWidgets.addAll(folderButtons);
        settingsWidgets.add(themeSelector);
        settingsWidgets.add(backgroundSelector);
        settingsWidgets.add(glassToggle);
    }

    private static int parse(String t, int def) {
        try {
            return Integer.parseInt(t.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    @Override public boolean closeOnEscape() { return false; }

    Tab tab() { return tab; }

    void setTab(Tab t) {
        if (t == tab) return;
        tab = t;
        tabAnim.snap(0f);
        tabAnim.target(1f);
    }

    void openAccounts(boolean open) {
        accountsOpen = open;
        accountsAnim.target(open ? 1f : 0f);
        if (!open) offlineName.setFocused(false);
    }

    // ================================================================== layout

    private float cx() { return SIDEBAR_W; }

    private float cy() { return TITLE_H; }

    private float cw() { return width - SIDEBAR_W; }

    private float ch() { return height - TITLE_H; }

    private float cardW() { return (cw() - 36) / 2f; }

    @Override
    protected void layout() {
        float colW = cardW(), lx = cx() + 12, rx = lx + colW + 12, top = cy() + 46;
        float fx = lx + 12, fw = colW - 24;
        memorySlider.setBounds(fx, top + 34, fw, 11);
        widthField.setBounds(fx, top + 70, (fw - 6) / 2f, 15);
        heightField.setBounds(fx + (fw + 6) / 2f, top + 70, (fw - 6) / 2f, 15);
        fullscreen.setBounds(fx + fw - 22, top + 94, 22, 11);
        jvmField.setBounds(fx, top + 130, fw, 15);
        javaField.setBounds(fx, top + 166, fw, 15);
        float gx = rx + 12;
        afterSelector.setBounds(gx, top + 34, fw, 14);
        clientIdField.setBounds(gx, top + 70, fw, 15);
        float bw = (fw - 8) / 3f;
        for (int i = 0; i < folderButtons.size(); i++) folderButtons.get(i).setBounds(gx + i * (bw + 4), top + 148, bw, 16);
        float ay = top + 208;
        themeSelector.setBounds(fx, ay + 34, fw, 14);
        backgroundSelector.setBounds(gx, ay + 34, fw - 70, 14);
        glassToggle.setBounds(gx + fw - 22, ay + 35.5f, 22, 11);
        logScroll.setBounds(cx() + 12, cy() + 40, cw() - 24, ch() - 52);
    }

    // ================================================================== render

    @Override
    public void render(GuiRenderer r, float mx, float my, float delta) {
        boolean modal = accountsOpen;
        float amx = modal ? -1 : mx, amy = modal ? -1 : my;
        Draw.rect(r, 0, 0, width, height, Colors.lerp(Theme.PANEL, 0xFF000000, Theme.light ? 0.05f : 0.45f) | 0xFF000000);
        renderContent(r, amx, amy);
        renderSidebar(r, amx, amy);
        renderTitleBar(r, mx, my);
        float a = accountsAnim.get();
        if (a > 0.01f) renderAccounts(r, mx, my, a);
    }

    private void renderTitleBar(GuiRenderer r, float mx, float my) {
        Draw.rect(r, 0, 0, width, TITLE_H, 0xFF05070D);
        Draw.rect(r, 0, TITLE_H - 0.5f, width, 0.5f, Theme.BORDER);
        FontRenderer f = Fonts.get(9f);
        String logo = "COOL";
        float x = 12;
        for (int i = 0; i < logo.length(); i++) {
            String ch = String.valueOf(logo.charAt(i));
            f.draw(r, ch, x, (TITLE_H - f.height()) / 2f, Theme.accent(i / 3f), false);
            x += f.width(ch) + 1.2f;
        }
        Fonts.small().draw(r, "LAUNCHER", x + 5, (TITLE_H - Fonts.small().height()) / 2f + 0.5f, Theme.TEXT_FAINT, false);
        // Window buttons
        float bw = 30;
        boolean closeHot = MathUtil.inside(mx, my, width - bw, 0, bw, TITLE_H);
        boolean minHot = MathUtil.inside(mx, my, width - bw * 2, 0, bw, TITLE_H);
        if (closeHot) Draw.rect(r, width - bw, 0, bw, TITLE_H, Theme.DANGER);
        if (minHot) Draw.rect(r, width - bw * 2, 0, bw, TITLE_H, Theme.SURFACE_HOVER);
        Icons.draw(r, Icons.Icon.CLOSE, width - bw / 2f - 4, TITLE_H / 2f - 4, 8, closeHot ? 0xFFFFFFFF : Theme.TEXT_DIM);
        Icons.draw(r, Icons.Icon.MINUS, width - bw * 1.5f - 4, TITLE_H / 2f - 4, 8, minHot ? 0xFFFFFFFF : Theme.TEXT_DIM);
    }

    /** Handles a press in the title bar (called before any screen gets the click). */
    boolean titleBarPress(float mx, float my, int button) {
        if (my >= TITLE_H || button != 0) return false;
        float bw = 30;
        if (mx >= width - bw) window.close();
        else if (mx >= width - bw * 2) window.minimize();
        else window.beginDrag();
        return true;
    }

    private void renderSidebar(GuiRenderer r, float mx, float my) {
        Draw.rect(r, 0, TITLE_H, SIDEBAR_W, height - TITLE_H, Colors.lerp(Theme.PANEL, 0xFF000000, Theme.light ? 0.04f : 0.3f));
        Draw.rect(r, SIDEBAR_W - 0.5f, TITLE_H, 0.5f, height - TITLE_H, Theme.BORDER);
        float y = TITLE_H + 12;
        y = navItem(r, mx, my, "Play", Icons.Icon.PLAY, tab == Tab.PLAY, y);
        y = navItem(r, mx, my, "Cosmetics", Icons.Icon.HANGER, false, y);
        y = navItem(r, mx, my, "Settings", Icons.Icon.GEAR, tab == Tab.SETTINGS, y);
        navItem(r, mx, my, "Console", Icons.Icon.TERMINAL, tab == Tab.CONSOLE, y);

        // Account chip
        float ax = 8, ay = height - 46, aw = SIDEBAR_W - 16, ah = 30;
        boolean hot = MathUtil.inside(mx, my, ax, ay, aw, ah);
        Draw.rounded(r, ax, ay, aw, ah, 6, hot ? Theme.SURFACE_HOVER : Theme.SURFACE);
        Draw.roundedOutline(r, ax, ay, aw, ah, 6, 0.75f, hot ? Theme.BORDER_STRONG : Theme.BORDER);
        Accounts.Account acc = c.accounts.selected();
        avatar(r, acc, ax + 6, ay + 6, 18);
        FontRenderer body = Fonts.body(), small = Fonts.small();
        String name = acc == null ? "Add account" : acc.name;
        String type = acc == null ? "Click to sign in" : acc.microsoft() ? "Microsoft" : "Offline";
        body.draw(r, body.trim(name, aw - 34), ax + 29, ay + 5, Theme.TEXT, false);
        small.draw(r, type, ax + 29, ay + 17, acc != null && acc.microsoft() ? Theme.SUCCESS : Theme.TEXT_DIM, false);
        small.draw(r, "v" + LauncherInfo.VERSION, ax + 2, height - 12, Theme.TEXT_FAINT, false);
    }

    private float navItem(GuiRenderer r, float mx, float my, String label, Icons.Icon icon, boolean active, float y) {
        float x = 8, w = SIDEBAR_W - 16, h = 24;
        boolean hot = MathUtil.inside(mx, my, x, y, w, h);
        if (active) {
            Draw.rounded(r, x, y, w, h, 5, Theme.SURFACE_ACTIVE);
            Draw.roundedGradient(r, x, y + 5, 2.5f, h - 10, 1.25f, Theme.accent(0f), Theme.accent(1f), false);
        } else if (hot) {
            Draw.rounded(r, x, y, w, h, 5, Theme.SURFACE);
        }
        int col = active ? Theme.TEXT : hot ? Theme.TEXT : Theme.TEXT_DIM;
        Icons.draw(r, icon, x + 10, y + (h - 10) / 2f, 10, active ? Theme.accent(0.2f) : col);
        Fonts.body().draw(r, label, x + 27, y + (h - Fonts.body().height()) / 2f, col, false);
        return y + h + 3;
    }

    private static void avatar(GuiRenderer r, Accounts.Account acc, float x, float y, float s) {
        Draw.circle(r, x + s / 2f, y + s / 2f, s / 2f, acc == null ? Theme.SURFACE_ACTIVE : Theme.accent(0.3f));
        if (acc == null) {
            Icons.draw(r, Icons.Icon.USER, x + s * 0.2f, y + s * 0.2f, s * 0.6f, Theme.TEXT_DIM);
        } else {
            FontRenderer f = Fonts.get(s * 0.5f);
            String ch = acc.name.substring(0, 1).toUpperCase();
            f.drawCentered(r, ch, x + s / 2f, y + (s - f.height()) / 2f, Theme.ON_ACCENT, false);
        }
    }

    private void renderContent(GuiRenderer r, float mx, float my) {
        float t = tabAnim.get();
        Scissor.push(r, cx(), cy(), cw(), ch());
        r.push();
        r.translate(0, (1 - t) * 6);
        switch (tab) {
            case PLAY: renderPlay(r, mx, my); break;
            case SETTINGS: renderSettings(r, mx, my); break;
            default: renderConsole(r, mx, my); break;
        }
        r.pop();
        Scissor.pop(r);
    }

    // ------------------------------------------------------------------ play

    private float playX() { return cx() + (cw() - 210) / 2f; }

    private float playY() { return cy() + ch() * 0.70f; }

    private float cardsY() { return cy() + ch() * 0.40f; }

    private float cardX(int i) {
        float w = 160, gap = 12;
        return cx() + (cw() - (w * 2 + gap)) / 2f + i * (w + gap);
    }

    private void renderPlay(GuiRenderer r, float mx, float my) {
        float time = (float) Time.seconds();
        r.push();
        r.translate(cx(), cy());
        MenuBackground.draw(r, cw(), ch(), time);
        r.pop();

        // Logo
        FontRenderer huge = Fonts.huge();
        String logo = "COOL CLIENT";
        float spacing = 3, lw = 0;
        for (int i = 0; i < logo.length(); i++) lw += huge.width(String.valueOf(logo.charAt(i))) + spacing;
        lw -= spacing;
        float lx = cx() + (cw() - lw) / 2f, ly = cy() + ch() * 0.12f;
        Draw.shadow(r, lx + 6, ly + 10, lw - 12, huge.height() - 16, 14, 30, Colors.withAlpha(Theme.accent(0.5f), 20));
        float x = lx;
        for (int i = 0; i < logo.length(); i++) {
            String ch = String.valueOf(logo.charAt(i));
            huge.draw(r, ch, x, ly, Theme.accent(i / (float) (logo.length() - 1)), false);
            x += huge.width(ch) + spacing;
        }
        Fonts.body().drawCentered(r, "Legit PvP. Animated cosmetics. Smooth performance.", cx() + cw() / 2f, ly + huge.height() + 2, Theme.TEXT_DIM, false);

        // Version cards
        String[][] info = {
                {"1.8.9", "CLASSIC PVP", "1.7 animations, motion blur, keystrokes."},
                {"1.21.4", "CRYSTAL PVP", "Totem counter, crystal and shield tweaks."}};
        for (int i = 0; i < 2; i++) {
            float x0 = cardX(i), y0 = cardsY(), w = 160, h = 58;
            boolean selected = c.settings.version.equals(info[i][0]);
            boolean hot = MathUtil.inside(mx, my, x0, y0, w, h) && !busy();
            if (selected) Draw.glow(r, x0, y0, w, h, 8, 6, Colors.withAlpha(Theme.accent(0.5f), 60));
            Draw.rounded(r, x0, y0, w, h, 8, selected ? 0xF0141B2C : hot ? 0xE0141A28 : 0xC80E121D);
            if (selected) Draw.roundedOutlineGradient(r, x0, y0, w, h, 8, 1f, Theme.accent(0f), Theme.accent(1f));
            else Draw.roundedOutline(r, x0, y0, w, h, 8, 0.75f, hot ? Theme.BORDER_STRONG : Theme.BORDER);
            Fonts.small().draw(r, info[i][1], x0 + 10, y0 + 8, selected ? Theme.accent(0.2f) : Theme.TEXT_FAINT, false);
            Fonts.title().draw(r, info[i][0], x0 + 10, y0 + 18, Theme.TEXT, false);
            Fonts.small().draw(r, Fonts.small().trim(info[i][2], w - 20), x0 + 10, y0 + 40, Theme.TEXT_DIM, false);
            if (selected) {
                Draw.circle(r, x0 + w - 14, y0 + 13, 5.5f, Theme.accent(0.4f));
                Icons.draw(r, Icons.Icon.CHECK, x0 + w - 18, y0 + 9, 8, Theme.ON_ACCENT);
            }
        }

        // Play button / progress
        float px = playX(), py = playY(), pw = 210, ph = 36;
        boolean hot = MathUtil.inside(mx, my, px, py, pw, ph);
        if (c.launching) {
            Draw.rounded(r, px, py, pw, ph, 9, 0xF0141B2C);
            float fw = Math.max(18, pw * MathUtil.clamp(c.fraction, 0, 1));
            Draw.roundedGradient(r, px, py, fw, ph, 9, Colors.withAlpha(Theme.accent(0f), 150), Colors.withAlpha(Theme.accent(1f), 150), true);
            Draw.roundedOutline(r, px, py, pw, ph, 9, 0.75f, Theme.BORDER_STRONG);
            String st = c.stage + (c.fraction > 0 && c.fraction < 1 ? "  " + Math.round(c.fraction * 100) + "%" : "");
            Fonts.body().drawCentered(r, Fonts.body().trim(st, pw - 16), px + pw / 2f, py + (ph - Fonts.body().height()) / 2f, Theme.TEXT, false);
        } else if (c.running()) {
            Draw.rounded(r, px, py, pw, ph, 9, 0xF0141B2C);
            Draw.roundedOutlineGradient(r, px, py, pw, ph, 9, 1f, Theme.accent(0f), Theme.accent(1f));
            Fonts.title().drawCentered(r, "PLAYING " + c.runningVersion, px + pw / 2f, py + 7, Theme.TEXT, false);
            Fonts.small().drawCentered(r, "Minecraft is running", px + pw / 2f, py + 23, Theme.TEXT_DIM, false);
            float kx = px + pw / 2f - 40, ky = py + ph + 8;
            boolean khot = MathUtil.inside(mx, my, kx, ky, 80, 14);
            Draw.rounded(r, kx, ky, 80, 14, 4, khot ? Colors.lerp(Theme.SURFACE, Theme.DANGER, 0.5f) : Theme.SURFACE);
            Fonts.small().drawCentered(r, "Force close game", kx + 40, ky + 3.5f, khot ? Theme.TEXT : Theme.TEXT_DIM, false);
        } else {
            Draw.glow(r, px, py, pw, ph, 9, 6 + 4 * (hot ? 1 : 0), Colors.withAlpha(Theme.accent(0.5f), hot ? 110 : 70));
            Draw.roundedGradient(r, px, py, pw, ph, 9, Theme.accent(0f), Theme.accent(1f), true);
            if (hot) Draw.rounded(r, px, py, pw, ph, 9, 0x22FFFFFF);
            FontRenderer tf = Fonts.get(13f);
            tf.drawCentered(r, "PLAY", px + pw / 2f, py + 5, Theme.ON_ACCENT, false);
            Accounts.Account acc = c.accounts.selected();
            String sub = "Minecraft " + c.settings.version + (acc == null ? "  ·  no account" : "  ·  " + acc.name);
            Fonts.small().drawCentered(r, sub, px + pw / 2f, py + 23, 0xC00A0E18, false);
        }
    }

    private boolean busy() { return c.launching || c.running(); }

    // ------------------------------------------------------------------ settings

    private void renderSettings(GuiRenderer r, float mx, float my) {
        Fonts.title().draw(r, "Settings", cx() + 12, cy() + 14, Theme.TEXT, false);
        float colW = cardW(), lx = cx() + 12, rx = lx + colW + 12, top = cy() + 46;
        card(r, lx, top, colW, 196, "Game");
        card(r, rx, top, colW, 196, "Launcher");
        FontRenderer s = Fonts.small();
        float fx = lx + 12, gx = rx + 12;
        s.draw(r, "Memory", fx, top + 24, Theme.TEXT_DIM, false);
        s.draw(r, "Window size", fx, top + 60, Theme.TEXT_DIM, false);
        s.draw(r, "Launch in fullscreen", fx, top + 96, Theme.TEXT_DIM, false);
        s.draw(r, "Extra JVM arguments", fx, top + 120, Theme.TEXT_DIM, false);
        s.draw(r, "Java executable", fx, top + 156, Theme.TEXT_DIM, false);
        s.draw(r, "After launching", gx, top + 24, Theme.TEXT_DIM, false);
        s.draw(r, "Microsoft app ID (for Microsoft sign-in)", gx, top + 60, Theme.TEXT_DIM, false);
        float hy = top + 92;
        for (String line : new String[]{"Microsoft sign-in needs an Azure app that Mojang",
                "has approved for Minecraft. Offline accounts work",
                "without one (singleplayer and offline servers)."}) {
            s.draw(r, line, gx, hy, Theme.TEXT_FAINT, false);
            hy += s.height() + 2;
        }
        s.draw(r, "Open folder", gx, top + 138, Theme.TEXT_DIM, false);
        float ay = top + 208;
        card(r, lx, ay, colW * 2 + 12, 62, "Appearance");
        s.draw(r, "Theme (shared with the game)", fx, ay + 24, Theme.TEXT_DIM, false);
        s.draw(r, "Background", gx, ay + 24, Theme.TEXT_DIM, false);
        s.drawRight(r, "Glass", gx + colW - 24 - 28, ay + 37, Theme.TEXT_DIM, false);
        for (Widget w : settingsWidgets) w.render(r, mx, my);
        s.draw(r, "Data: " + s.trim(LauncherPaths.BASE.getAbsolutePath(), colW - 40), gx, top + 176, Theme.TEXT_FAINT, false);
    }

    private static void card(GuiRenderer r, float x, float y, float w, float h, String title) {
        Draw.rounded(r, x, y, w, h, 8, Theme.PANEL_LIGHT);
        Draw.roundedOutline(r, x, y, w, h, 8, 0.75f, Theme.BORDER);
        Fonts.body().draw(r, title, x + 12, y + 8, Theme.TEXT, false);
    }

    // ------------------------------------------------------------------ console

    private float consoleButtonX(int i) { return cx() + cw() - 12 - (3 - i) * 64 + 4; }

    private void renderConsole(GuiRenderer r, float mx, float my) {
        Fonts.title().draw(r, "Console", cx() + 12, cy() + 14, Theme.TEXT, false);
        String[] labels = {"Copy", "Clear", "Force close"};
        for (int i = 0; i < 3; i++) {
            float bx = consoleButtonX(i), by = cy() + 12;
            boolean enabled = i < 2 || c.running();
            boolean hot = enabled && MathUtil.inside(mx, my, bx, by, 60, 16);
            int base = i == 2 && hot ? Colors.lerp(Theme.SURFACE, Theme.DANGER, 0.5f) : hot ? Theme.SURFACE_HOVER : Theme.SURFACE;
            Draw.rounded(r, bx, by, 60, 16, 4, base);
            Draw.roundedOutline(r, bx, by, 60, 16, 4, 0.75f, Theme.BORDER);
            Fonts.small().drawCentered(r, labels[i], bx + 30, by + 4.5f, enabled ? Theme.TEXT : Theme.TEXT_FAINT, false);
        }
        float x = logScroll.x, y = logScroll.y, w = logScroll.w, h = logScroll.h;
        Draw.rounded(r, x, y, w, h, 6, Colors.lerp(Theme.PANEL, 0xFF000000, Theme.light ? 0.03f : 0.35f) | 0xFF000000);
        Draw.roundedOutline(r, x, y, w, h, 6, 0.75f, Theme.BORDER);
        List<String> lines = c.logLines();
        FontRenderer f = Fonts.small();
        float lh = f.height() + 2;
        logScroll.setContentHeight(lines.size() * lh + 12);
        int v = c.logVersion();
        if (v != seenLogVersion) {
            seenLogVersion = v;
            if (followLog) logScroll.scrollTo(logScroll.maxScroll());
        }
        Scissor.push(r, x + 1, y + 1, w - 2, h - 2);
        if (lines.isEmpty()) {
            f.drawCentered(r, "Game output appears here when you press Play.", x + w / 2f, y + h / 2f - 4, Theme.TEXT_FAINT, false);
        }
        float off = logScroll.offset();
        int first = Math.max(0, (int) ((off - 6) / lh));
        for (int i = first; i < lines.size(); i++) {
            float ly = y + 6 + i * lh - off;
            if (ly > y + h) break;
            String line = lines.get(i);
            int col = line.contains("ERROR") || line.contains("Exception") ? Theme.DANGER
                    : line.contains("WARN") ? Theme.WARNING : line.startsWith("[Launcher]") ? Theme.accent(0.2f) : Theme.TEXT_DIM;
            f.draw(r, f.trim(line, w - 18), x + 8, ly, col, false);
        }
        Scissor.pop(r);
        logScroll.update(r, mx, my, CoolClient.get().platform().input().isMouseDown(0));
        followLog = logScroll.offset() >= logScroll.maxScroll() - 2;
    }

    // ------------------------------------------------------------------ accounts dialog

    private float dialogW() { return 280; }

    private float dialogH() { return 120 + Math.max(1, c.accounts.all().size()) * 26 + (c.signingIn ? 64 : 0); }

    private float dialogX() { return (width - dialogW()) / 2f; }

    private float dialogY() { return (height - dialogH()) / 2f; }

    private void renderAccounts(GuiRenderer r, float mx, float my, float a) {
        Draw.rect(r, 0, TITLE_H, width, height - TITLE_H, Colors.withAlpha(0xFF03050A, (int) (170 * a)));
        float w = dialogW(), h = dialogH(), x = dialogX(), y = dialogY() + (1 - a) * 10;
        r.push();
        Draw.shadow(r, x, y, w, h, 10, 24, Colors.withAlpha(0xFF000000, (int) (160 * a)));
        Draw.panel(r, x, y, w, h, 10, a);
        Draw.roundedOutline(r, x, y, w, h, 10, 0.75f, Theme.BORDER_STRONG);
        Fonts.title().draw(r, "Accounts", x + 14, y + 12, Theme.TEXT, false);
        boolean closeHot = MathUtil.inside(mx, my, x + w - 24, y + 10, 14, 14);
        Icons.draw(r, Icons.Icon.CLOSE, x + w - 22, y + 13, 9, closeHot ? Theme.TEXT : Theme.TEXT_DIM);

        float ry = y + 36;
        List<Accounts.Account> all = c.accounts.all();
        Accounts.Account sel = c.accounts.selected();
        if (all.isEmpty()) {
            Fonts.small().draw(r, "No accounts yet. Add one below.", x + 14, ry + 7, Theme.TEXT_FAINT, false);
            ry += 26;
        }
        for (Accounts.Account acc : all) {
            boolean selected = sel != null && sel.id.equals(acc.id);
            boolean hot = MathUtil.inside(mx, my, x + 10, ry, w - 20, 22);
            Draw.rounded(r, x + 10, ry, w - 20, 22, 5, selected ? Theme.SURFACE_ACTIVE : hot ? Theme.SURFACE_HOVER : Theme.SURFACE);
            avatar(r, acc, x + 15, ry + 3, 16);
            Fonts.body().draw(r, acc.name, x + 37, ry + 3, Theme.TEXT, false);
            Fonts.small().draw(r, acc.microsoft() ? "Microsoft" : "Offline", x + 37, ry + 13, acc.microsoft() ? Theme.SUCCESS : Theme.TEXT_DIM, false);
            if (selected) Icons.draw(r, Icons.Icon.CHECK, x + w - 50, ry + 7, 8, Theme.accent(0.3f));
            boolean rmHot = MathUtil.inside(mx, my, x + w - 32, ry + 4, 14, 14);
            Icons.draw(r, Icons.Icon.CLOSE, x + w - 29, ry + 7, 8, rmHot ? Theme.DANGER : Theme.TEXT_FAINT);
            ry += 26;
        }

        // Microsoft
        ry += 4;
        float bx = x + 14, bw = w - 28;
        if (c.signingIn) {
            Accounts.DeviceCode code = c.deviceCode;
            Draw.rounded(r, bx, ry, bw, 64, 6, Theme.SURFACE);
            if (code == null) {
                Fonts.small().drawCentered(r, "Contacting Microsoft...", bx + bw / 2f, ry + 28, Theme.TEXT_DIM, false);
            } else {
                Fonts.small().drawCentered(r, "Go to microsoft.com/link and enter this code:", bx + bw / 2f, ry + 6, Theme.TEXT_DIM, false);
                Fonts.title().drawCentered(r, code.userCode, bx + bw / 2f, ry + 18, Theme.accent(0.3f), false);
                Fonts.small().drawCentered(r, "(copied to your clipboard)", bx + bw / 2f, ry + 34, Theme.TEXT_FAINT, false);
            }
            smallButton(r, mx, my, bx + bw / 2f - 84, ry + 46, 80, "Open link", true);
            smallButton(r, mx, my, bx + bw / 2f + 4, ry + 46, 80, "Cancel", true);
            ry += 64;
        } else {
            boolean hot = MathUtil.inside(mx, my, bx, ry, bw, 20);
            Draw.roundedGradient(r, bx, ry, bw, 20, 5, Theme.accent(0f), Theme.accent(1f), true);
            if (hot) Draw.rounded(r, bx, ry, bw, 20, 5, 0x22FFFFFF);
            Fonts.body().drawCentered(r, "Sign in with Microsoft", bx + bw / 2f, ry + 5.5f, Theme.ON_ACCENT, false);
            if (c.settings.msClientId.isEmpty()) {
                Fonts.small().drawCentered(r, "Needs a Microsoft app ID in Settings first", bx + bw / 2f, ry + 24, Theme.TEXT_FAINT, false);
            }
        }
        ry += 38;
        // Offline
        offlineName.setBounds(bx, ry, bw - 58, 17);
        offlineName.render(r, mx, my);
        smallButton(r, mx, my, bx + bw - 54, ry, 54, "Add offline", true);
        Fonts.small().draw(r, "Offline names work in singleplayer and offline servers.", bx, ry + 22, Theme.TEXT_FAINT, false);
        r.pop();
    }

    private static void smallButton(GuiRenderer r, float mx, float my, float x, float y, float w, String label, boolean enabled) {
        boolean hot = enabled && MathUtil.inside(mx, my, x, y, w, 17);
        Draw.rounded(r, x, y, w, 17, 4, hot ? Theme.SURFACE_HOVER : Theme.SURFACE_ACTIVE);
        Draw.roundedOutline(r, x, y, w, 17, 4, 0.75f, hot ? Theme.BORDER_STRONG : Theme.BORDER);
        Fonts.small().drawCentered(r, label, x + w / 2f, y + 5, Theme.TEXT, false);
    }

    private void submitOffline(String name) {
        if (c.addOffline(name)) offlineName.setText("");
    }

    private boolean accountsClick(float mx, float my, int button) {
        float w = dialogW(), x = dialogX(), y = dialogY();
        if (!MathUtil.inside(mx, my, x, y, w, dialogH())) {
            openAccounts(false);
            return true;
        }
        if (MathUtil.inside(mx, my, x + w - 24, y + 10, 14, 14)) {
            openAccounts(false);
            return true;
        }
        float ry = y + 36;
        List<Accounts.Account> all = c.accounts.all();
        if (all.isEmpty()) ry += 26;
        for (Accounts.Account acc : all) {
            if (MathUtil.inside(mx, my, x + w - 32, ry + 4, 14, 14)) {
                c.accounts.remove(acc);
                return true;
            }
            if (MathUtil.inside(mx, my, x + 10, ry, w - 20, 22)) {
                c.accounts.select(acc);
                return true;
            }
            ry += 26;
        }
        ry += 4;
        float bx = x + 14, bw = w - 28;
        if (c.signingIn) {
            if (MathUtil.inside(mx, my, bx + bw / 2f - 84, ry + 46, 80, 17)) {
                Accounts.DeviceCode code = c.deviceCode;
                CoolClient.get().platform().openUrl(code != null ? code.verificationUri : "https://www.microsoft.com/link");
                return true;
            }
            if (MathUtil.inside(mx, my, bx + bw / 2f + 4, ry + 46, 80, 17)) {
                c.cancelMicrosoft();
                return true;
            }
            ry += 64;
        } else {
            if (MathUtil.inside(mx, my, bx, ry, bw, 20)) {
                c.startMicrosoft();
                return true;
            }
        }
        ry += 38;
        if (MathUtil.inside(mx, my, bx + bw - 54, ry, 54, 17)) {
            submitOffline(offlineName.text());
            return true;
        }
        offlineName.mouseClicked(mx, my, button);
        return true;
    }

    // ================================================================== input

    @Override
    public boolean mouseClicked(float mx, float my, int button) {
        if (accountsOpen) return accountsClick(mx, my, button);
        // Sidebar
        if (mx < SIDEBAR_W && my > TITLE_H) {
            float y = TITLE_H + 12;
            Object[] items = {Tab.PLAY, "cosmetics", Tab.SETTINGS, Tab.CONSOLE};
            for (Object it : items) {
                if (MathUtil.inside(mx, my, 8, y, SIDEBAR_W - 16, 24)) {
                    if (it instanceof Tab) setTab((Tab) it);
                    else CoolClient.get().platform().openScreen(new CosmeticsScreen(this));
                    return true;
                }
                y += 27;
            }
            if (MathUtil.inside(mx, my, 8, height - 46, SIDEBAR_W - 16, 30)) {
                openAccounts(true);
                return true;
            }
            return false;
        }
        switch (tab) {
            case PLAY: return playClick(mx, my);
            case SETTINGS:
                for (Widget w : settingsWidgets) {
                    if (w instanceof TextField) ((TextField) w).mouseClicked(mx, my, button);
                }
                for (Widget w : settingsWidgets) {
                    if (!(w instanceof TextField) && w.mouseClicked(mx, my, button)) {
                        if (w == themeSelector || w == backgroundSelector) CoolClient.get().config().markDirty();
                        return true;
                    }
                }
                return true;
            default:
                if (logScroll.mouseClicked(mx, my)) return true;
                for (int i = 0; i < 3; i++) {
                    if (MathUtil.inside(mx, my, consoleButtonX(i), cy() + 12, 60, 16)) {
                        if (i == 0) CoolClient.get().platform().setClipboard(String.join("\n", c.logLines()));
                        else if (i == 1) c.clearLog();
                        else c.kill();
                        return true;
                    }
                }
                return false;
        }
    }

    private boolean playClick(float mx, float my) {
        if (!busy()) {
            for (int i = 0; i < 2; i++) {
                if (MathUtil.inside(mx, my, cardX(i), cardsY(), 160, 58)) {
                    c.settings.version = LauncherSettings.VERSIONS[i];
                    c.settings.save();
                    return true;
                }
            }
        }
        float px = playX(), py = playY();
        if (MathUtil.inside(mx, my, px, py, 210, 36) && !busy()) {
            if (c.accounts.selected() == null) openAccounts(true);
            else c.play(null);
            return true;
        }
        if (c.running() && MathUtil.inside(mx, my, px + 210 / 2f - 40, py + 44, 80, 14)) {
            c.kill();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(float mx, float my, int button) {
        if (tab == Tab.SETTINGS && !accountsOpen) {
            for (Widget w : settingsWidgets) w.mouseReleased(mx, my, button);
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(float mx, float my, double amount) {
        if (accountsOpen) return true;
        if (tab == Tab.CONSOLE) return logScroll.scroll(mx, my, amount);
        if (tab == Tab.SETTINGS) {
            for (Widget w : settingsWidgets) if (w.mouseScrolled(mx, my, amount)) return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int key, int mods) {
        if (accountsOpen) {
            if (key == Keys.ESCAPE) {
                if (c.signingIn) c.cancelMicrosoft();
                openAccounts(false);
                return true;
            }
            return offlineName.keyPressed(key, mods);
        }
        if (tab == Tab.SETTINGS) {
            for (Widget w : settingsWidgets) if (w.keyPressed(key, mods)) return true;
        }
        if (tab == Tab.PLAY && (key == Keys.ENTER || key == Keys.KP_ENTER) && !busy()) {
            if (c.accounts.selected() == null) openAccounts(true);
            else c.play(null);
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(char ch) {
        if (accountsOpen) return offlineName.charTyped(ch);
        if (tab == Tab.SETTINGS) {
            for (Widget w : settingsWidgets) if (w.charTyped(ch)) return true;
        }
        return false;
    }
}
