package dev.coolclient.core.gui.screen;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.api.GuiRenderer;
import dev.coolclient.core.font.FontRenderer;
import dev.coolclient.core.font.Fonts;
import dev.coolclient.core.gui.UIScreen;
import dev.coolclient.core.gui.widget.Button;
import dev.coolclient.core.gui.widget.Toggle;
import dev.coolclient.core.json.Json;
import dev.coolclient.core.render.Colors;
import dev.coolclient.core.render.Draw;
import dev.coolclient.core.render.Icons;
import dev.coolclient.core.render.Theme;
import dev.coolclient.core.util.Keys;
import dev.coolclient.core.util.Time;

/**
 * First-run screen: says plainly what Cool Client is and isn't, and asks before the online
 * cosmetics feature starts sending the player's name and UUID to the API server.
 */
public final class WelcomeScreen extends UIScreen {
    private static final String[][] POINTS = {
            {"Legit only", "Visual and quality-of-life mods. No reach, aim assist, ESP or autoclickers."},
            {"Open source", "Every line of code is public, and releases are built from it in the open."},
            {"Your account stays yours", "Your Microsoft password and tokens never reach Cool Client's servers."},
    };
    private static final Icons.Icon[] ICONS = {Icons.Icon.CHECK, Icons.Icon.EYE, Icons.Icon.USER};

    private boolean online = true;
    private final Toggle onlineToggle = new Toggle(() -> online, v -> online = v);
    private final Button start = new Button("Let's go", this::accept).primary().icon(Icons.Icon.CHECK);
    private final Button source = new Button("Read the code", () -> CoolClient.get().platform().openUrl(CoolClient.SOURCE_URL))
            .icon(Icons.Icon.TERMINAL);
    private float px, py, pw, ph;

    public WelcomeScreen(UIScreen parent) {
        super(parent);
    }

    /** True until the player has been through this screen once. */
    public static boolean needed() {
        return !Json.bool(CoolClient.get().config().clientData(), "welcomed", false);
    }

    /** Saves the choice and returns to the main menu. */
    public void accept() {
        CoolClient.get().client().onlineFeatures.set(online);
        CoolClient.get().config().clientData().put("welcomed", true);
        CoolClient.get().config().markDirty();
        close();
    }

    @Override
    protected void layout() {
        pw = Math.min(width - 24, 330);
        ph = Math.min(height - 20, 222);
        px = (width - pw) / 2f;
        py = (height - ph) / 2f;
        float by = py + ph - 28;
        source.setBounds(px + 16, by, (pw - 40) / 2f, 18);
        start.setBounds(px + 24 + (pw - 40) / 2f, by, (pw - 40) / 2f, 18);
        onlineToggle.setBounds(px + pw - 38, py + 130.5f, 22, 11);
    }

    @Override
    public void render(GuiRenderer r, float mx, float my, float delta) {
        MenuBackground.draw(r, width, height, (float) Time.seconds());
        float open = openProgress();
        layout();
        float x = px + 16, y = py + 14 + (1f - open) * 10f;
        Draw.beginPanels(r);
        Draw.shadow(r, px, py, pw, ph, 12, 26, Colors.fade(0x90000000, open));
        Draw.panel(r, px, py, pw, ph, 12, open);
        Draw.roundedOutline(r, px, py, pw, ph, 12, 0.75f, Colors.fade(Theme.BORDER, open));

        Fonts.title().draw(r, "Welcome to Cool Client", x, y, Colors.fade(Theme.TEXT, open), false);
        Fonts.small().draw(r, "Here's what you're installing, in plain words.", x, y + 14, Colors.fade(Theme.TEXT_DIM, open), false);
        y += 28;
        FontRenderer body = Fonts.body(), small = Fonts.small();
        for (int i = 0; i < POINTS.length; i++) {
            Draw.circle(r, x + 7, y + 7, 7, Colors.fade(Colors.withAlpha(Theme.accent(i / 2f), 50), open));
            Icons.draw(r, ICONS[i], x + 3, y + 3, 8, Colors.fade(Theme.accent(i / 2f), open));
            body.draw(r, POINTS[i][0], x + 20, y, Colors.fade(Theme.TEXT, open), false);
            small.draw(r, small.trim(POINTS[i][1], pw - 52), x + 20, y + 11, Colors.fade(Theme.TEXT_DIM, open), false);
            y += 25;
        }
        y = py + 124;
        Draw.rect(r, x, y, pw - 32, 0.5f, Colors.fade(Theme.BORDER, open));
        body.draw(r, "Show players' cosmetics", x, y + 8, Colors.fade(Theme.TEXT, open), false);
        onlineToggle.render(r, mx, my);
        String[] note = {"Sends your Minecraft name and UUID to the Cool Client server so",
                "other players see your cosmetics and you see theirs. Nothing else.",
                "You can change this any time under Settings."};
        for (int i = 0; i < note.length; i++) {
            small.draw(r, small.trim(note[i], pw - 32), x, y + 20 + i * 9, Colors.fade(Theme.TEXT_FAINT, open), false);
        }
        source.render(r, mx, my);
        start.render(r, mx, my);
    }

    @Override
    public boolean mouseClicked(float mx, float my, int button) {
        return onlineToggle.mouseClicked(mx, my, button) || source.mouseClicked(mx, my, button) || start.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int mods) {
        if (key == Keys.ENTER) {
            accept();
            return true;
        }
        return super.keyPressed(key, mods);
    }

    @Override
    public boolean closeOnEscape() { return false; }
}
