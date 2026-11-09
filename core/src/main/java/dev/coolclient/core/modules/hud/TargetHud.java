package dev.coolclient.core.modules.hud;

import dev.coolclient.core.api.GuiRenderer;
import dev.coolclient.core.api.LivingInfo;
import dev.coolclient.core.event.Events;
import dev.coolclient.core.font.FontRenderer;
import dev.coolclient.core.font.Fonts;
import dev.coolclient.core.hud.HudElement;
import dev.coolclient.core.render.Colors;
import dev.coolclient.core.render.Draw;
import dev.coolclient.core.setting.BoolSetting;
import dev.coolclient.core.setting.ColorSetting;
import dev.coolclient.core.setting.NumberSetting;
import dev.coolclient.core.util.MathUtil;
import dev.coolclient.core.util.Time;

/** Name, health and distance of whatever you hit last. */
public final class TargetHud extends HudElement {
    private final BoolSetting mobs = add(new BoolSetting("Mobs Too", true).describe("Off: only players"));
    private final BoolSetting distance = add(new BoolSetting("Show Distance", true));
    private final BoolSetting armor = add(new BoolSetting("Show Armor", true));
    private final NumberSetting hideAfter = add(new NumberSetting("Hide After", 4, 1, 15, 1).suffix("s"));
    private final BoolSetting blur = add(new BoolSetting("Blur Behind", false));
    private final ColorSetting background = add(new ColorSetting("Background Color", 0x90000000));
    private final ColorSetting accent = add(new ColorSetting("Accent", 0xFF2DE2C1));

    private int target = -1;
    private boolean targetIsPlayer;
    private long hitAt;
    private float shown = -1;

    public TargetHud() {
        super("Target HUD", "Health and distance of the player or mob you're fighting.");
        defaultPosition(Anchor.CENTER, 60, 20);
        on(Events.Attack.class, e -> {
            if (!e.targetIsPlayer && !mobs.on()) return;
            if (e.targetId != target) shown = -1;
            target = e.targetId;
            targetIsPlayer = e.targetIsPlayer;
            hitAt = Time.millis();
        });
        on(Events.WorldLeave.class, e -> target = -1);
    }

    private LivingInfo info() {
        if (target < 0 || !game().inWorld() || Time.millis() - hitAt > hideAfter.d() * 1000) return null;
        return game().living(target);
    }

    @Override
    public boolean shouldRender() { return info() != null; }

    @Override
    protected void draw(GuiRenderer r, boolean editing) {
        LivingInfo t = info();
        if (t == null) {
            if (!editing) {
                size(120, 36);
                return;
            }
            t = new LivingInfo("Opponent", 13.5f, 20f, 4f, 3.1, true, 16);
        }
        FontRenderer f = Fonts.vanilla();
        float w = Math.max(120, f.width(t.name) + 16), h = 36;
        size(w, h);
        if (blur.on()) Draw.glass(r, 0, 0, w, h, 4, background.color(), 1f);
        else Draw.rounded(r, 0, 0, w, h, 4, background.color());
        f.draw(r, t.name, 6, 5, 0xFFFFFFFF, true);

        // Health bar, eased toward the real value so hits read as a drop.
        float max = Math.max(1f, t.maxHealth);
        float frac = MathUtil.clamp(t.health / max, 0f, 1f);
        shown = shown < 0 ? frac : shown + (frac - shown) * Math.min(1f, Time.delta() * 8f);
        float bx = 6, by = 18, bw = w - 12, bh = 5;
        Draw.rounded(r, bx, by, bw, bh, 2, 0x60000000);
        int hc = Colors.lerp(0xFFFF4F4F, 0xFF55FF7A, frac);
        if (shown > 0) Draw.rounded(r, bx, by, Math.max(bh, bw * shown), bh, 2, hc);
        if (t.absorption > 0) {
            float af = MathUtil.clamp(t.absorption / max, 0f, 1f);
            Draw.rounded(r, bx, by + bh + 1, Math.max(3, bw * af), 2, 1, 0xFFFFD23F);
        }

        String hp = MathUtil.format(t.health, 1) + " HP";
        if (t.absorption > 0) hp += " §e+" + MathUtil.format(t.absorption, 1);
        Fonts.get(7f).draw(r, hp, 6, 26, 0xFFE8ECF5, false);
        StringBuilder right = new StringBuilder();
        if (armor.on() && t.armor > 0) right.append("§b").append(t.armor).append(" armor");
        if (distance.on()) {
            if (right.length() > 0) right.append("  ");
            right.append("§7").append(MathUtil.format(t.distance, 1)).append("m");
        }
        if (right.length() > 0) Fonts.get(7f).drawRight(r, right.toString(), w - 6, 26, 0xFFE8ECF5, false);
        Draw.rounded(r, 0, 0, 2, h, 1, accent.color());
    }
}
