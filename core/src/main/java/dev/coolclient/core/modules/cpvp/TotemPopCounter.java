package dev.coolclient.core.modules.cpvp;

import dev.coolclient.core.api.Feature;
import dev.coolclient.core.event.Events;
import dev.coolclient.core.module.Category;
import dev.coolclient.core.module.Module;
import dev.coolclient.core.setting.BoolSetting;

import java.util.HashMap;
import java.util.Map;

/** Tracks how many totems each player has popped. */
public final class TotemPopCounter extends Module {
    public final BoolSetting chat = add(new BoolSetting("Chat Messages", true));
    public final BoolSetting nametags = add(new BoolSetting("Show On Nametags", true));
    public final BoolSetting countSelf = add(new BoolSetting("Count Your Own Pops", true));
    private final Map<String, Integer> pops = new HashMap<String, Integer>();

    public TotemPopCounter() {
        super("Totem Pop Counter", "Counts totem pops per player for crystal PvP.", Category.COMBAT, Feature.TOTEMS);
        versionTag("1.21");
        enabledByDefault();
        on(Events.TotemPop.class, e -> {
            if (e.self && !countSelf.on()) return;
            Integer n = pops.get(e.name);
            int count = n == null ? 1 : n + 1;
            pops.put(e.name, count);
            if (chat.on()) {
                String who = e.self ? "You" : e.name;
                platform().showChat("§8[§bCool§8] §f" + who + " §7popped §c" + count
                        + " §7totem" + (count == 1 ? "" : "s"));
            }
        });
        on(Events.PlayerDeath.class, e -> {
            Integer n = pops.remove(e.name);
            if (n != null && chat.on()) {
                platform().showChat("§8[§bCool§8] §f" + e.name + " §7died after popping §c" + n
                        + " §7totem" + (n == 1 ? "" : "s"));
            }
        });
        on(Events.WorldLeave.class, e -> pops.clear());
        on(Events.WorldJoin.class, e -> pops.clear());
    }

    public int pops(String name) {
        Integer n = pops.get(name);
        return n == null ? 0 : n;
    }

    public boolean showOnNametags() { return isEnabled() && nametags.on(); }
}
