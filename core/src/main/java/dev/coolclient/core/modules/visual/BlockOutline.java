package dev.coolclient.core.modules.visual;

import dev.coolclient.core.module.Category;
import dev.coolclient.core.module.Module;
import dev.coolclient.core.setting.BoolSetting;
import dev.coolclient.core.setting.ColorSetting;
import dev.coolclient.core.setting.NumberSetting;

public final class BlockOutline extends Module {
    public final ColorSetting color = add(new ColorSetting("Outline Color", 0xCC2DE2C1));
    public final NumberSetting width = add(new NumberSetting("Line Width", 2, 1, 6, 0.5));
    public final BoolSetting fill = add(new BoolSetting("Fill", false));
    public final ColorSetting fillColor = add(new ColorSetting("Fill Color", 0x222DE2C1).visibleWhen(fill::on));

    public BlockOutline() {
        super("Block Outline", "Customise the outline of the block you're looking at.", Category.VISUAL);
    }
}
