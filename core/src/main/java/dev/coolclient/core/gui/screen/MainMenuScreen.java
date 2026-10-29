package dev.coolclient.core.gui.screen;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.api.GuiRenderer;
import dev.coolclient.core.api.VanillaScreen;
import dev.coolclient.core.font.FontRenderer;
import dev.coolclient.core.font.Fonts;
import dev.coolclient.core.gui.UIScreen;
import dev.coolclient.core.gui.widget.Button;
import dev.coolclient.core.render.Colors;
import dev.coolclient.core.render.Draw;
import dev.coolclient.core.render.Icons;
import dev.coolclient.core.render.Theme;
import dev.coolclient.core.util.Time;

import java.util.ArrayList;
import java.util.List;

/** Cool Client's title screen. */
public final class MainMenuScreen extends UIScreen {
    private final List<Button> buttons = new ArrayList<Button>();
    private Button cosmetics;
    private Button settings;
    private Button themes;

    public MainMenuScreen() {
        super(null);
    }

    @Override
    protected void layout() {
        buttons.clear();
        final UIScreen self = this;
        float bw = 184, bh = 21, gap = 5;
        float x = (width - bw) / 2f;
        float y = height * 0.48f;
        buttons.add(btn("Singleplayer", Icons.Icon.CUBE, () -> CoolClient.get().platform().openVanilla(VanillaScreen.SINGLEPLAYER, self), x, y, bw, bh).primary());
        y += bh + gap;
        buttons.add(btn("Multiplayer", Icons.Icon.MULTIPLAYER, () -> CoolClient.get().platform().openVanilla(VanillaScreen.MULTIPLAYER, self), x, y, bw, bh));
        y += bh + gap;
        float half = (bw - gap) / 2f;
        buttons.add(btn("Cool Mods", Icons.Icon.GRID, () -> CoolClient.get().platform().openScreen(new ModMenuScreen(self)), x, y, half, bh));
        buttons.add(btn("Options", Icons.Icon.GEAR, () -> CoolClient.get().platform().openVanilla(VanillaScreen.OPTIONS, self), x + half + gap, y, half, bh));
        y += bh + gap;
        buttons.add(btn("Quit Game", Icons.Icon.EXIT, () -> CoolClient.get().platform().quit(), x, y, bw, bh).danger());
        cosmetics = btn("", Icons.Icon.HANGER, () -> CoolClient.get().platform().openScreen(new CosmeticsScreen(self)), width - 30, 8, 22, 22).radius(11);
        settings = btn("", Icons.Icon.GLOBE, () -> CoolClient.get().platform().openVanilla(VanillaScreen.LANGUAGE, self), width - 56, 8, 22, 22).radius(11);
        themes = btn("", Icons.Icon.PALETTE, () -> CoolClient.get().platform().openScreen(ModMenuScreen.themes(self)), width - 82, 8, 22, 22).radius(11);
    }

    private static Button btn(String label, Icons.Icon icon, Runnable action, float x, float y, float w, float h) {
        Button b = new Button(label, action).icon(icon).radius(6).iconSize(label.isEmpty() ? 9 : 12);
        b.setBounds(x, y, w, h);
        return b;
    }

    @Override
    public void render(GuiRenderer r, float mx, float my, float delta) {
        float t = (float) Time.seconds();
        MenuBackground.draw(r, width, height, t);

        float open = openProgress();
        FontRenderer huge = Fonts.huge();
        String logo = "COOL";
        float spacing = 3f;
        float lw = 0;
        for (int i = 0; i < logo.length(); i++) lw += huge.width(String.valueOf(logo.charAt(i))) + spacing;
        lw -= spacing;
        float lx = (width - lw) / 2f, ly = height * 0.24f - (1f - open) * 10f;
        // Soft filled glow (an outline-only glow leaves a dark hole behind the letters).
        Draw.shadow(r, lx + 4, ly + 10, lw - 8, huge.height() - 16, 14, 30, Colors.withAlpha(Theme.accent(0.5f), (int) (22 * open)));
        float cx = lx;
        for (int i = 0; i < logo.length(); i++) {
            String ch = String.valueOf(logo.charAt(i));
            float bob = (float) Math.sin(t * 1.6f + i * 0.5f) * 1.2f;
            huge.draw(r, ch, cx, ly + bob, Colors.fade(Theme.accent(i / (float) (logo.length() - 1)), open), false);
            cx += huge.width(ch) + spacing;
        }
        String sub = "C L I E N T";
        Fonts.body().drawCentered(r, sub, width / 2f, ly + huge.height() + 4, Colors.fade(Theme.TEXT_DIM, open), false);

        for (Button b : buttons) b.render(r, mx, my);
        cosmetics.render(r, mx, my);
        settings.render(r, mx, my);
        themes.render(r, mx, my);

        FontRenderer sf = Fonts.small();
        sf.draw(r, CoolClient.NAME + " " + CoolClient.VERSION + "  ·  Minecraft " + CoolClient.get().platform().minecraftVersion(),
                6, height - 12, Theme.TEXT_FAINT, false);
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

    @Override
    public boolean mouseClicked(float mx, float my, int button) {
        for (Button b : buttons) if (b.mouseClicked(mx, my, button)) return true;
        if (cosmetics.mouseClicked(mx, my, button)) return true;
        if (themes.mouseClicked(mx, my, button)) return true;
        return settings.mouseClicked(mx, my, button);
    }

    @Override
    public boolean closeOnEscape() { return false; }
}
