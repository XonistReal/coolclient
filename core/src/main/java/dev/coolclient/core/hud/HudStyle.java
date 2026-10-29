package dev.coolclient.core.hud;

import dev.coolclient.core.api.GuiRenderer;
import dev.coolclient.core.font.FontRenderer;
import dev.coolclient.core.font.Fonts;
import dev.coolclient.core.module.Module;
import dev.coolclient.core.render.Colors;
import dev.coolclient.core.render.Draw;
import dev.coolclient.core.setting.BoolSetting;
import dev.coolclient.core.setting.ColorSetting;
import dev.coolclient.core.setting.ModeSetting;
import dev.coolclient.core.setting.NumberSetting;

/** Shared look settings for text-based HUD elements (background, border, font, colours). */
public final class HudStyle {
    public final BoolSetting background;
    public final ColorSetting backgroundColor;
    public final BoolSetting blur;
    public final BoolSetting border;
    public final ColorSetting borderColor;
    public final NumberSetting radius;
    public final ColorSetting textColor;
    public final BoolSetting shadow;
    public final ModeSetting font;

    public HudStyle(Module owner, Adder adder, boolean defaultBackground) {
        background = adder.add(new BoolSetting("Background", defaultBackground));
        backgroundColor = adder.add(new ColorSetting("Background Color", 0x80000000)
                .visibleWhen(background::on));
        blur = adder.add(new BoolSetting("Blur Behind", false)
                .describe("Frosted glass: blurs the world behind the element")
                .visibleWhen(background::on));
        border = adder.add(new BoolSetting("Border", false).visibleWhen(background::on));
        borderColor = adder.add(new ColorSetting("Border Color", 0xFF2DE2C1).visibleWhen(() -> background.on() && border.on()));
        radius = adder.add(new NumberSetting("Corner Radius", 2, 0, 8, 0.5).visibleWhen(background::on));
        textColor = adder.add(new ColorSetting("Text Color", 0xFFFFFFFF));
        shadow = adder.add(new BoolSetting("Text Shadow", true));
        font = adder.add(new ModeSetting("Font", "Minecraft", "Minecraft", "Smooth"));
    }

    /** Lets HudStyle register settings through the owning module's protected add(). */
    public interface Adder {
        <S extends dev.coolclient.core.setting.Setting<?>> S add(S setting);
    }

    public FontRenderer fontRenderer() {
        return font.is("Smooth") ? Fonts.get(8.5f) : Fonts.vanilla();
    }

    public void drawBox(GuiRenderer r, float w, float h) {
        if (!background.on()) return;
        float rad = radius.f();
        if (blur.on()) Draw.glass(r, 0, 0, w, h, rad, backgroundColor.color(), 1f);
        else Draw.rounded(r, 0, 0, w, h, rad, backgroundColor.color());
        if (border.on()) Draw.roundedOutline(r, 0, 0, w, h, rad, 1f, borderColor.color());
    }

    public int text(float offset) { return textColor.color(offset); }

    /** Text colour for a sub-part (e.g. label vs value) mixed toward dim grey. */
    public int dim() { return Colors.lerp(textColor.color(), 0xFF8D97B0, 0.45f); }
}
