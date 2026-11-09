package dev.coolclient.core.modules.hud;

import dev.coolclient.core.api.BlockInfo;
import dev.coolclient.core.api.GuiRenderer;
import dev.coolclient.core.api.ItemRef;
import dev.coolclient.core.font.FontRenderer;
import dev.coolclient.core.hud.HudElement;
import dev.coolclient.core.hud.HudStyle;
import dev.coolclient.core.setting.BoolSetting;
import dev.coolclient.core.setting.Setting;

/** The block you're looking at: its icon, name and where it's from. */
public final class BlockInfoHud extends HudElement {
    public final HudStyle style;
    private final BoolSetting icon = add(new BoolSetting("Icon", true));
    private final BoolSetting id = add(new BoolSetting("Show ID", false));
    private final BoolSetting source = add(new BoolSetting("Show Mod", true).describe("Which mod adds the block"));
    private final BoolSetting position = add(new BoolSetting("Show Position", false));

    public BlockInfoHud() {
        super("Block Info", "Name and icon of the block under your crosshair.");
        defaultPosition(Anchor.TOP_CENTER, 0, 4);
        style = new HudStyle(this, new HudStyle.Adder() {
            @Override
            public <S extends Setting<?>> S add(S setting) {
                return BlockInfoHud.this.add(setting);
            }
        }, true);
    }

    private BlockInfo target() {
        return game().inWorld() ? game().targetBlock() : null;
    }

    @Override
    public boolean shouldRender() { return target() != null; }

    @Override
    protected void draw(GuiRenderer r, boolean editing) {
        BlockInfo b = target();
        if (b == null) {
            if (!editing) {
                size(90, 24);
                return;
            }
            ItemRef log = game().itemOf("minecraft:oak_log");
            if (log.isEmpty()) log = game().itemOf("minecraft:log"); // 1.8 name
            b = new BlockInfo("minecraft:oak_log", "Oak Log", log, 12, 64, -30);
        }
        FontRenderer f = style.fontRenderer();
        String second = "";
        if (id.on()) second = b.id;
        else if (source.on()) second = "§9§o" + b.source;
        if (position.on()) second = (second.isEmpty() ? "" : second + "  §7") + b.x + " " + b.y + " " + b.z;
        boolean showIcon = icon.on() && !b.item.isEmpty();
        float textX = showIcon ? 24 : 6;
        float tw = Math.max(f.width(b.name), second.isEmpty() ? 0 : f.width(second) * 0.8f);
        float w = textX + tw + 6, h = second.isEmpty() ? 22 : 26;
        size(w, h);
        style.drawBox(r, w, h);
        if (showIcon) r.item(b.item, 4, (h - 16) / 2f, false);
        float ty = second.isEmpty() ? (h - f.height()) / 2f : 4;
        f.draw(r, b.name, textX, ty, style.text(0), style.shadow.on());
        if (!second.isEmpty()) {
            r.push();
            r.translate(textX, ty + f.height() + 2);
            r.scale(0.8f, 0.8f);
            f.draw(r, second, 0, 0, style.dim(), style.shadow.on());
            r.pop();
        }
    }
}
