package dev.coolclient.core.modules.visual;

import dev.coolclient.core.module.Category;
import dev.coolclient.core.module.Module;
import dev.coolclient.core.render.gpu.SkyStyle;
import dev.coolclient.core.setting.BoolSetting;
import dev.coolclient.core.setting.ModeSetting;
import dev.coolclient.core.setting.NumberSetting;
import dev.coolclient.core.setting.TextSetting;

/**
 * Replaces the overworld sky with a shader-drawn skybox (or your own panorama image). The
 * sky itself is drawn by {@link dev.coolclient.core.render.gpu.SkyRenderer}; the version
 * mixins only ask it whether to skip the vanilla sky.
 */
public final class Skybox extends Module {
    public final ModeSetting style = add(new ModeSetting("Sky", SkyStyle.AURORA.displayName, SkyStyle.names()));
    public final BoolSetting sunMoon = add(new BoolSetting("Sun & Moon", true)
            .describe("Draw the sun and moon on skies that don't have their own"));
    public final NumberSetting rotation = add(new NumberSetting("Rotation Speed", 1, 0, 10, 0.5));
    public final NumberSetting brightness = add(new NumberSetting("Brightness", 100, 20, 150, 5).suffix("%"));
    public final BoolSetting tintFog = add(new BoolSetting("Match Fog", true)
            .describe("Blend distant terrain into the new horizon colour"));
    public final TextSetting image = add(new TextSetting("Image File", "skybox.png", 64)
            .describe("Equirectangular PNG/JPG in .minecraft/coolclient/skyboxes")
            .visibleWhen(() -> style.is(SkyStyle.IMAGE.displayName)));

    public Skybox() {
        super("Skybox", "Animated shader skies, or your own panorama image.", Category.VISUAL);
    }

    public SkyStyle sky() { return SkyStyle.byName(style.get()); }
}
