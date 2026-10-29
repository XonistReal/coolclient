package dev.coolclient.core.gui.widget;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.setting.BoolSetting;
import dev.coolclient.core.setting.ColorSetting;
import dev.coolclient.core.setting.GridSetting;
import dev.coolclient.core.setting.KeySetting;
import dev.coolclient.core.setting.ModeSetting;
import dev.coolclient.core.setting.NumberSetting;
import dev.coolclient.core.setting.Setting;
import dev.coolclient.core.setting.TextSetting;

/** Creates the right widget for a setting. */
public final class SettingWidgets {
    private SettingWidgets() {}

    public static Widget create(Setting<?> s) {
        if (s instanceof BoolSetting) {
            final BoolSetting b = (BoolSetting) s;
            return new Toggle(b::on, v -> {
                b.set(v);
                CoolClient.get().config().markDirty();
            });
        }
        if (s instanceof NumberSetting) return new Slider((NumberSetting) s);
        if (s instanceof ModeSetting) return new ModeSelector((ModeSetting) s);
        if (s instanceof ColorSetting) return new ColorPicker((ColorSetting) s);
        if (s instanceof KeySetting) return new KeyBinder((KeySetting) s);
        if (s instanceof GridSetting) return new GridEditor((GridSetting) s);
        if (s instanceof TextSetting) {
            final TextSetting t = (TextSetting) s;
            TextField f = new TextField().maxLength(t.maxLength()).onChange(v -> {
                t.set(v);
                CoolClient.get().config().markDirty();
            });
            f.setText(t.get());
            f.setBounds(0, 0, 120, 13);
            return f;
        }
        return null;
    }

    /** True for widgets that sit below their label instead of beside it. */
    public static boolean fullWidth(Setting<?> s) {
        return s instanceof ColorSetting || s instanceof GridSetting;
    }
}
