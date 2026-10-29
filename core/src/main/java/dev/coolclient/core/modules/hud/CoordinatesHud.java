package dev.coolclient.core.modules.hud;

import dev.coolclient.core.api.GuiRenderer;
import dev.coolclient.core.font.FontRenderer;
import dev.coolclient.core.hud.HudElement;
import dev.coolclient.core.hud.HudStyle;
import dev.coolclient.core.setting.BoolSetting;
import dev.coolclient.core.setting.ModeSetting;
import dev.coolclient.core.setting.NumberSetting;
import dev.coolclient.core.setting.Setting;
import dev.coolclient.core.util.MathUtil;

import java.util.ArrayList;
import java.util.List;

public final class CoordinatesHud extends HudElement {
    private final ModeSetting layout = add(new ModeSetting("Layout", "Vertical", "Vertical", "Horizontal"));
    private final NumberSetting decimals = add(new NumberSetting("Decimals", 0, 0, 2, 1));
    private final BoolSetting direction = add(new BoolSetting("Show Direction", true));
    private final BoolSetting biome = add(new BoolSetting("Show Biome", false));
    private final BoolSetting otherDimension = add(new BoolSetting("Show Nether/Overworld", false));
    private final HudStyle style;

    private static final String[] DIRS = {"South", "South West", "West", "North West", "North", "North East", "East", "South East"};
    private static final String[] AXIS = {"+Z", "-X +Z", "-X", "-X -Z", "-Z", "+X -Z", "+X", "+X +Z"};

    public CoordinatesHud() {
        super("Coordinates", "Your position, facing and biome.");
        defaultPosition(Anchor.TOP_LEFT, 4, 58);
        enabledByDefault();
        style = new HudStyle(this, new HudStyle.Adder() {
            @Override
            public <S extends Setting<?>> S add(S setting) {
                return CoordinatesHud.this.add(setting);
            }
        }, true);
    }

    @Override
    protected void draw(GuiRenderer r, boolean editing) {
        boolean live = game().inWorld();
        double x = live ? game().x() : 128.5, y = live ? game().y() : 64, z = live ? game().z() : -256.5;
        int d = decimals.i();
        List<String[]> rows = new ArrayList<String[]>();
        rows.add(new String[]{"X", MathUtil.format(x, d)});
        rows.add(new String[]{"Y", MathUtil.format(y, d)});
        rows.add(new String[]{"Z", MathUtil.format(z, d)});
        if (direction.on()) {
            float yaw = live ? game().yaw() : 180f;
            int idx = Math.round(MathUtil.wrapDegrees(yaw) / 45f) & 7;
            rows.add(new String[]{"F", DIRS[idx] + " (" + AXIS[idx] + ")"});
        }
        if (otherDimension.on() && live) {
            String dim = game().dimension();
            if ("the_nether".equals(dim)) rows.add(new String[]{"OW", MathUtil.format(x * 8, 0) + ", " + MathUtil.format(z * 8, 0)});
            else if ("overworld".equals(dim)) rows.add(new String[]{"Nether", MathUtil.format(x / 8, 0) + ", " + MathUtil.format(z / 8, 0)});
        }
        if (biome.on()) rows.add(new String[]{"Biome", live ? game().biome() : "Plains"});

        FontRenderer f = style.fontRenderer();
        float pad = style.background.on() ? 4 : 0;
        float lh = f.height() + 1;
        if (layout.is("Horizontal")) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(3, rows.size()); i++) {
                if (i > 0) sb.append("  ");
                sb.append("§7").append(rows.get(i)[0]).append(": §r").append(rows.get(i)[1]);
            }
            float w = f.width(sb.toString()) + pad * 2, h = f.height() + pad * 2;
            size(w, h);
            style.drawBox(r, w, h);
            f.draw(r, sb.toString(), pad, pad, style.text(0), style.shadow.on());
            return;
        }
        float w = 0;
        for (String[] row : rows) w = Math.max(w, f.width(row[0] + ": " + row[1]));
        w += pad * 2;
        float h = rows.size() * lh - 1 + pad * 2;
        size(w, h);
        style.drawBox(r, w, h);
        float ty = pad;
        for (String[] row : rows) {
            float lx = pad + f.draw(r, row[0] + ": ", pad, ty, style.dim(), style.shadow.on());
            f.draw(r, row[1], lx, ty, style.text(ty / 40f), style.shadow.on());
            ty += lh;
        }
    }
}
