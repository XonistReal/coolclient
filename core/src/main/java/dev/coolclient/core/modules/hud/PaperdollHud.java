package dev.coolclient.core.modules.hud;

import dev.coolclient.core.api.GuiRenderer;
import dev.coolclient.core.hud.HudElement;
import dev.coolclient.core.render.Draw;
import dev.coolclient.core.setting.BoolSetting;
import dev.coolclient.core.setting.NumberSetting;
import dev.coolclient.core.util.Time;

/** A small model of your own player on the HUD. */
public final class PaperdollHud extends HudElement {
    private final NumberSetting size = add(new NumberSetting("Size", 28, 16, 60, 1));
    private final NumberSetting angle = add(new NumberSetting("Angle", -25, -180, 180, 5).suffix("°"));
    private final BoolSetting followPitch = add(new BoolSetting("Follow Head Pitch", true));
    private final BoolSetting onlyMoving = add(new BoolSetting("Only When Moving", false)
            .describe("Show it while sprinting, sneaking, gliding or riding"));
    private final BoolSetting background = add(new BoolSetting("Background", false));

    public PaperdollHud() {
        super("Paperdoll", "A little model of you on the HUD, like Bedrock's paper doll.");
        defaultPosition(Anchor.TOP_LEFT, 10, 30);
    }

    @Override
    public boolean shouldRender() {
        if (!game().inWorld()) return false;
        return !onlyMoving.on() || game().sprinting() || game().sneaking() || game().gliding() || game().riding();
    }

    @Override
    protected void draw(GuiRenderer r, boolean editing) {
        float s = size.f();
        float w = s * 1.25f, h = s * 2.15f;
        size(w, h);
        if (background.on() || (editing && !game().inWorld())) Draw.rounded(r, 0, 0, w, h, 4, 0x60000000);
        if (!game().inWorld()) return;
        float yaw = angle.f() + (editing ? (float) Math.sin(Time.seconds()) * 10f : 0f);
        float pitch = followPitch.on() ? game().pitch() * 0.5f : 0f;
        r.playerPreview(w / 2f, h - s * 0.08f, s, yaw, pitch);
    }
}
