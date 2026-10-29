package dev.coolclient.core.modules.client;

import dev.coolclient.core.font.Fonts;
import dev.coolclient.core.module.Category;
import dev.coolclient.core.module.Module;
import dev.coolclient.core.render.Theme;
import dev.coolclient.core.render.gpu.Backgrounds;
import dev.coolclient.core.render.gpu.GpuDraw;
import dev.coolclient.core.setting.BoolSetting;
import dev.coolclient.core.setting.ColorSetting;
import dev.coolclient.core.setting.KeySetting;
import dev.coolclient.core.setting.ModeSetting;
import dev.coolclient.core.setting.NumberSetting;
import dev.coolclient.core.setting.TextSetting;
import dev.coolclient.core.util.Keys;

/** Global client preferences (always on). */
public final class ClientSettings extends Module {
    public final KeySetting menuKey = add(new KeySetting("Menu Key", Keys.RIGHT_SHIFT));
    public final ModeSetting theme = add(new ModeSetting("Theme", "Aurora", Theme.presetNames())
            .describe("Colours for every menu; also sets the accents below"));
    public final ModeSetting menuBackground = add(new ModeSetting("Menu Background", THEME_DEFAULT, backgroundModes())
            .describe("Animated shader behind the main menu and launcher"));
    public final BoolSetting glassPanels = add(new BoolSetting("Glass Panels", true)
            .describe("Frosted glass that blurs whatever is behind menus"));
    public final NumberSetting glassBlur = add(new NumberSetting("Glass Blur", 60, 0, 100, 5).suffix("%")
            .visibleWhen(glassPanels::on));
    public final BoolSetting shaderUi = add(new BoolSetting("Shader Rendering", true)
            .describe("Draw the UI with GPU shaders; turn off if menus look broken"));
    public final ColorSetting accentA = add(new ColorSetting("Accent Primary", 0xFF2DE2C1, false));
    public final ColorSetting accentB = add(new ColorSetting("Accent Secondary", 0xFF8A5CFF, false));
    public final BoolSetting animatedAccent = add(new BoolSetting("Animated Accent", true));
    public final NumberSetting uiOpacity = add(new NumberSetting("Menu Opacity", 92, 50, 100, 1).suffix("%"));
    public final ModeSetting uiFont = add(new ModeSetting("Interface Font", "Segoe UI",
            "Segoe UI", "Inter", "Arial", "Verdana", "Tahoma", "Trebuchet MS", "Calibri", "SansSerif"));
    public final BoolSetting customMainMenu = add(new BoolSetting("Custom Main Menu", true));
    public final BoolSetting hudSnapping = add(new BoolSetting("HUD Snapping", true));
    public final BoolSetting showBadges = add(new BoolSetting("Client User Badges", true)
            .describe("Show the Cool Client icon next to other Cool Client users"));
    public final BoolSetting notifications = add(new BoolSetting("Notifications", true));
    public final BoolSetting onlineFeatures = add(new BoolSetting("Online Features", false)
            .describe("Share cosmetics with other players through a Cool Client API server"));
    public final TextSetting apiUrl = add(new TextSetting("API Server", dev.coolclient.core.CoolClient.DEFAULT_API_URL));
    public final BoolSetting windowTitle = add(new BoolSetting("Branded Window Title", true));

    private static final String THEME_DEFAULT = "Theme Default";

    private static String[] backgroundModes() {
        java.util.List<String> l = new java.util.ArrayList<String>();
        l.add(THEME_DEFAULT);
        l.addAll(Backgrounds.names());
        return l.toArray(new String[0]);
    }

    public ClientSettings() {
        super("Client", "Cool Client's global settings: theme, menu key, online features.", Category.CLIENT);
        Runnable apply = this::apply;
        theme.onChange(() -> {
            // A new theme brings its own accents; they stay editable afterwards.
            Theme.Preset p = Theme.preset(theme.get());
            accentA.set(p.accentA);
            accentB.set(p.accentB);
            apply();
        });
        menuBackground.onChange(apply);
        glassPanels.onChange(apply);
        glassBlur.onChange(apply);
        shaderUi.onChange(apply);
        accentA.onChange(apply);
        accentB.onChange(apply);
        animatedAccent.onChange(apply);
        uiOpacity.onChange(apply);
        uiFont.onChange(apply);
    }

    @Override
    public boolean toggleable() { return false; }

    @Override
    public boolean bindable() { return false; }

    public void apply() {
        Theme.Preset preset = Theme.preset(theme.get());
        Theme.apply(preset);
        Backgrounds.Style bg = Backgrounds.byName(menuBackground.get());
        Theme.background = bg != null ? bg : preset.background;
        Theme.glass = glassPanels.on();
        Theme.blur = 2f + glassBlur.f() / 100f * 3f;
        GpuDraw.setEnabled(shaderUi.on());
        Theme.accentA = accentA.get();
        Theme.accentB = accentB.get();
        Theme.animatedAccent = animatedAccent.on();
        Theme.uiOpacity = uiOpacity.f() / 100f;
        Fonts.setFamily(uiFont.get());
    }
}
