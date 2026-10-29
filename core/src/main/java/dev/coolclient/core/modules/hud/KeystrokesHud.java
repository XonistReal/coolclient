package dev.coolclient.core.modules.hud;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.api.GuiRenderer;
import dev.coolclient.core.font.FontRenderer;
import dev.coolclient.core.font.Fonts;
import dev.coolclient.core.hud.HudElement;
import dev.coolclient.core.render.Colors;
import dev.coolclient.core.render.Draw;
import dev.coolclient.core.setting.BoolSetting;
import dev.coolclient.core.setting.ColorSetting;
import dev.coolclient.core.setting.ModeSetting;
import dev.coolclient.core.setting.NumberSetting;
import dev.coolclient.core.util.Keys;
import dev.coolclient.core.util.MathUtil;
import dev.coolclient.core.util.Time;

import java.util.HashMap;
import java.util.Map;

/** WASD + mouse + space keystrokes with smooth press fades. */
public final class KeystrokesHud extends HudElement {
    private final BoolSetting showMouse = add(new BoolSetting("Show Mouse Buttons", true));
    private final BoolSetting showCps = add(new BoolSetting("Show CPS", true).visibleWhen(showMouse::on));
    private final BoolSetting showSpace = add(new BoolSetting("Show Spacebar", true));
    private final NumberSetting keySize = add(new NumberSetting("Key Size", 22, 16, 32, 1));
    private final NumberSetting gap = add(new NumberSetting("Gap", 2, 0, 5, 0.5));
    private final NumberSetting radius = add(new NumberSetting("Corner Radius", 3, 0, 8, 0.5));
    private final NumberSetting fade = add(new NumberSetting("Fade Time", 90, 0, 400, 10).suffix("ms"));
    private final ColorSetting background = add(new ColorSetting("Background", 0x70000000));
    private final ColorSetting pressed = add(new ColorSetting("Pressed Background", 0xC8FFFFFF));
    private final ColorSetting text = add(new ColorSetting("Text Color", 0xFFFFFFFF));
    private final ColorSetting pressedText = add(new ColorSetting("Pressed Text Color", 0xFF101010));
    private final BoolSetting border = add(new BoolSetting("Accent Border", false));
    private final ModeSetting font = add(new ModeSetting("Font", "Minecraft", "Minecraft", "Smooth"));

    private final Map<String, Float> state = new HashMap<String, Float>();

    public KeystrokesHud() {
        super("Keystrokes", "Shows your movement keys and mouse clicks.");
        defaultPosition(Anchor.TOP_RIGHT, -4, 4);
        enabledByDefault();
    }

    @Override
    protected void draw(GuiRenderer r, boolean editing) {
        float k = keySize.f(), g = gap.f();
        float total = k * 3 + g * 2;
        float y = 0;
        key(r, "forward", "W", k + g, y, k, k);
        y += k + g;
        key(r, "left", "A", 0, y, k, k);
        key(r, "back", "S", k + g, y, k, k);
        key(r, "right", "D", (k + g) * 2, y, k, k);
        y += k + g;
        if (showMouse.on()) {
            float mw = (total - g) / 2f;
            float mh = showCps.on() ? k : k * 0.8f;
            mouse(r, "attack", "LMB", 0, y, mw, mh, CoolClient.get().leftCps().cps());
            mouse(r, "use", "RMB", mw + g, y, mw, mh, CoolClient.get().rightCps().cps());
            y += mh + g;
        }
        if (showSpace.on()) {
            float sh = k * 0.55f;
            float p = press("jump");
            box(r, 0, y, total, sh, p);
            float lw = total * 0.42f;
            Draw.rounded(r, (total - lw) / 2f, y + sh / 2f - 0.75f, lw, 1.5f, 0.75f,
                    Colors.lerp(text.color(), pressedText.color(), p));
            y += sh;
        } else {
            y -= g;
        }
        size(total, y);
    }

    private FontRenderer fontRenderer() {
        return font.is("Smooth") ? Fonts.get(8.5f) : Fonts.vanilla();
    }

    private float press(String binding) {
        int code = game().bindingKey(binding);
        boolean down = code != Keys.NONE && game().inWorld() && !platform().anyScreenOpen() && isDown(code);
        Float cur = state.get(binding);
        float v = cur == null ? 0f : cur;
        float speed = fade.f() <= 0 ? 1f : Time.delta() * 1000f / fade.f();
        v = MathUtil.clamp(v + (down ? speed : -speed), 0f, 1f);
        state.put(binding, v);
        return v;
    }

    private void box(GuiRenderer r, float x, float y, float w, float h, float p) {
        Draw.rounded(r, x, y, w, h, radius.f(), Colors.lerp(background.color(x), pressed.color(x), p));
        if (border.on()) Draw.roundedOutline(r, x, y, w, h, radius.f(), 0.75f, dev.coolclient.core.render.Theme.accent(x / 80f));
    }

    private void key(GuiRenderer r, String binding, String fallback, float x, float y, float w, float h) {
        float p = press(binding);
        box(r, x, y, w, h, p);
        int code = game().bindingKey(binding);
        String label = code == Keys.NONE ? fallback : Keys.name(code);
        if (label.length() > 3) label = label.substring(0, 3);
        FontRenderer f = fontRenderer();
        f.drawCentered(r, label, x + w / 2f, y + (h - f.height()) / 2f + 0.5f,
                Colors.lerp(text.color(x), pressedText.color(), p), p < 0.5f);
    }

    private void mouse(GuiRenderer r, String binding, String label, float x, float y, float w, float h, int cps) {
        float p = press(binding);
        box(r, x, y, w, h, p);
        FontRenderer f = fontRenderer();
        int c = Colors.lerp(text.color(x), pressedText.color(), p);
        if (showCps.on()) {
            float sf = 0.6f;
            f.drawCentered(r, label, x + w / 2f, y + h / 2f - f.height() + 1.5f, c, p < 0.5f);
            r.push();
            r.translate(x + w / 2f, y + h / 2f + 1.5f);
            r.scale(sf, sf);
            f.drawCentered(r, cps + " CPS", 0, 0, c, p < 0.5f);
            r.pop();
        } else {
            f.drawCentered(r, label, x + w / 2f, y + (h - f.height()) / 2f + 0.5f, c, p < 0.5f);
        }
    }
}
