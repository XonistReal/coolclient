package dev.coolclient.core.modules.hud;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.api.GuiRenderer;
import dev.coolclient.core.api.ItemRef;
import dev.coolclient.core.event.Events;
import dev.coolclient.core.font.FontRenderer;
import dev.coolclient.core.font.Fonts;
import dev.coolclient.core.gui.Notifications;
import dev.coolclient.core.hud.HudElement;
import dev.coolclient.core.render.Colors;
import dev.coolclient.core.setting.BoolSetting;
import dev.coolclient.core.setting.NumberSetting;
import dev.coolclient.core.util.Time;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Warns when your armour or held item is about to break. */
public final class DurabilityAlertsHud extends HudElement {
    private final NumberSetting threshold = add(new NumberSetting("Threshold", 10, 1, 50, 1).suffix("%"));
    private final BoolSetting includeHeld = add(new BoolSetting("Include Held Item", true));
    private final BoolSetting toast = add(new BoolSetting("Toast Notification", true));
    private static final String[] SLOT_NAMES = {"Boots", "Leggings", "Chestplate", "Helmet"};
    private final Set<String> warned = new HashSet<String>();
    private final List<String> lines = new ArrayList<String>();

    public DurabilityAlertsHud() {
        super("Durability Alerts", "Warns you before your gear breaks.");
        defaultPosition(Anchor.CENTER, 0, 30);
        enabledByDefault();
        on(Events.Tick.class, e -> refresh());
    }

    private void refresh() {
        lines.clear();
        if (!game().inWorld()) return;
        float t = threshold.f() / 100f;
        for (int i = 3; i >= 0; i--) check(game().armor(i), SLOT_NAMES[i], t);
        if (includeHeld.on()) check(game().mainHand(), "Held Item", t);
    }

    private void check(ItemRef it, String label, float t) {
        if (it.isEmpty() || !it.isDamageable()) return;
        String key = label + ":" + it.id();
        if (it.durabilityFraction() <= t) {
            lines.add(label + " " + Math.round(it.durabilityFraction() * 100) + "%");
            if (warned.add(key) && toast.on()) {
                CoolClient.get().notifications().post("Low durability", it.displayName() + " is about to break!", Notifications.Level.WARNING);
            }
        } else {
            warned.remove(key);
        }
    }

    @Override
    public boolean shouldRender() { return !lines.isEmpty(); }

    @Override
    protected void draw(GuiRenderer r, boolean editing) {
        List<String> show = lines.isEmpty() && editing ? java.util.Collections.singletonList("Helmet 8%") : lines;
        FontRenderer f = Fonts.vanilla();
        float w = 0;
        for (String s : show) w = Math.max(w, f.width("! " + s));
        float pulse = 0.6f + 0.4f * (float) Math.abs(Math.sin(Time.seconds() * 3));
        float y = 0;
        for (String s : show) {
            f.drawCentered(r, "! " + s, w / 2f, y, Colors.fade(0xFFFF5470, pulse), true);
            y += 10;
        }
        size(Math.max(w, 40), Math.max(y - 1, 9));
    }
}
