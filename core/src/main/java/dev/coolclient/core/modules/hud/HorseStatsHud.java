package dev.coolclient.core.modules.hud;

import dev.coolclient.core.hud.LinesElement;
import dev.coolclient.core.setting.BoolSetting;
import dev.coolclient.core.util.MathUtil;

import java.util.ArrayList;
import java.util.List;

/** Speed, jump height and health of the horse (or donkey, mule, camel ...) you're riding. */
public final class HorseStatsHud extends LinesElement {
    private final BoolSetting speed = add(new BoolSetting("Speed", true));
    private final BoolSetting jump = add(new BoolSetting("Jump Height", true));
    private final BoolSetting health = add(new BoolSetting("Health", true));

    public HorseStatsHud() {
        super("Horse Stats", "Speed, jump height and health of your horse while riding.");
        defaultPosition(Anchor.BOTTOM_LEFT, 4, -60);
    }

    @Override
    public boolean shouldRender() { return game().inWorld() && game().mountStats() != null; }

    @Override
    protected List<String> lines(boolean editing) {
        double[] s = game().inWorld() ? game().mountStats() : null;
        if (s == null) s = editing ? new double[] {13.2, 4.1, 22, 30} : null;
        List<String> out = new ArrayList<String>();
        if (s == null) return out;
        if (speed.on()) out.add("Speed: " + MathUtil.format(s[0], 1) + " m/s");
        if (jump.on()) out.add("Jump: " + MathUtil.format(s[1], 1) + " blocks");
        if (health.on()) out.add("Health: " + MathUtil.format(s[2], 0) + " / " + MathUtil.format(s[3], 0));
        return out;
    }
}
