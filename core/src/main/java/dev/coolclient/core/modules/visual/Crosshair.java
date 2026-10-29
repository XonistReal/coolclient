package dev.coolclient.core.modules.visual;

import dev.coolclient.core.api.GuiRenderer;
import dev.coolclient.core.event.Events;
import dev.coolclient.core.module.Category;
import dev.coolclient.core.module.Module;
import dev.coolclient.core.render.Draw;
import dev.coolclient.core.setting.BoolSetting;
import dev.coolclient.core.setting.ColorSetting;
import dev.coolclient.core.setting.GridSetting;
import dev.coolclient.core.setting.ModeSetting;
import dev.coolclient.core.setting.NumberSetting;

/** Pixel-editable crosshair with presets, replacing the vanilla one. */
public final class Crosshair extends Module {
    private static final int N = 15;

    public final ModeSetting preset = add(new ModeSetting("Preset", "Cross", "Cross", "Gap Cross", "Dot", "Plus", "X", "Circle", "Custom"));
    public final GridSetting grid = add(new GridSetting("Pixels", N, build("Cross")));
    public final ColorSetting color = add(new ColorSetting("Color", 0xFFFFFFFF));
    public final BoolSetting outline = add(new BoolSetting("Outline", true));
    public final ColorSetting outlineColor = add(new ColorSetting("Outline Color", 0xC0000000).visibleWhen(outline::on));
    public final NumberSetting scale = add(new NumberSetting("Scale", 1, 0.5, 3, 0.25));
    public final BoolSetting hideThirdPerson = add(new BoolSetting("Hide In Third Person", true));
    public final BoolSetting hideInDebug = add(new BoolSetting("Hide With F3", false));

    public Crosshair() {
        super("Crosshair", "Design your own crosshair pixel by pixel.", Category.VISUAL);
        preset.onChange(() -> {
            if (!preset.is("Custom")) grid.set(build(preset.get()));
        });
        grid.onChange(() -> {
            if (!grid.get().equals(build(preset.get())) && !preset.is("Custom")) preset.set("Custom");
        });
        on(Events.RenderHud.class, e -> render(e.renderer));
    }

    /** True when the vanilla crosshair should be suppressed. */
    public boolean replacesVanilla() { return isEnabled(); }

    private void render(GuiRenderer r) {
        if (!game().inWorld()) return;
        if (hideThirdPerson.on() && game().perspective() != 0) return;
        if (hideInDebug.on() && game().debugOpen()) return;
        float s = scale.f();
        float ox = Math.round(r.width() / 2f - N * s / 2f), oy = Math.round(r.height() / 2f - N * s / 2f);
        if (outline.on()) {
            int oc = outlineColor.color();
            for (int i = 0; i < N; i++)
                for (int j = 0; j < N; j++)
                    if (grid.pixel(i, j)) Draw.rect(r, ox + (i - 0.5f) * s, oy + (j - 0.5f) * s, s * 2, s * 2, oc);
        }
        int c = color.color();
        for (int i = 0; i < N; i++)
            for (int j = 0; j < N; j++)
                if (grid.pixel(i, j)) Draw.rect(r, ox + i * s, oy + j * s, s, s, c);
    }

    private static String build(String preset) {
        boolean[][] g = new boolean[N][N];
        int c = N / 2;
        switch (preset) {
            case "Gap Cross":
                for (int i = 0; i < N; i++) {
                    if (Math.abs(i - c) >= 2 && Math.abs(i - c) <= 5) {
                        g[i][c] = true;
                        g[c][i] = true;
                    }
                }
                break;
            case "Dot":
                g[c][c] = true;
                g[c - 1][c] = g[c + 1][c] = g[c][c - 1] = g[c][c + 1] = true;
                break;
            case "Plus":
                for (int i = c - 3; i <= c + 3; i++) {
                    g[i][c] = true;
                    g[c][i] = true;
                    g[i][c - 1] = true;
                    g[c - 1][i] = true;
                }
                break;
            case "X":
                for (int i = -4; i <= 4; i++) {
                    if (i == 0) continue;
                    g[c + i][c + i] = true;
                    g[c + i][c - i] = true;
                }
                break;
            case "Circle":
                for (int a = 0; a < 360; a += 10) {
                    int x = c + (int) Math.round(Math.cos(Math.toRadians(a)) * 4);
                    int y = c + (int) Math.round(Math.sin(Math.toRadians(a)) * 4);
                    g[x][y] = true;
                }
                g[c][c] = true;
                break;
            default: // Cross
                for (int i = c - 4; i <= c + 4; i++) {
                    g[i][c] = true;
                    g[c][i] = true;
                }
                break;
        }
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < N; y++)
            for (int x = 0; x < N; x++) sb.append(g[x][y] ? '1' : '0');
        return sb.toString();
    }
}
