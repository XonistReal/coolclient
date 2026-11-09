package dev.coolclient.core.modules.utility;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.event.Events;
import dev.coolclient.core.module.Category;
import dev.coolclient.core.module.Module;
import dev.coolclient.core.setting.BoolSetting;
import dev.coolclient.core.waypoint.WaypointManager;

import java.util.ArrayList;

/** Where you died: a chat line with the coordinates, and a waypoint to walk back to. */
public final class DeathInfo extends Module {
    private static final String WAYPOINT = "Death";
    private final BoolSetting chat = add(new BoolSetting("Chat Message", true));
    private final BoolSetting waypoint = add(new BoolSetting("Death Waypoint", true).describe("Replaces the last one; needs Waypoints on to show"));
    private boolean dead;

    public DeathInfo() {
        super("Death Info", "Tells you where you died and marks the spot with a waypoint.", Category.UTILITY);
        on(Events.Tick.class, e -> {
            if (!game().inWorld()) {
                dead = false;
                return;
            }
            boolean now = game().health() <= 0f;
            if (now && !dead) died();
            dead = now;
        });
    }

    private void died() {
        int x = (int) Math.floor(game().x()), y = (int) Math.floor(game().y()), z = (int) Math.floor(game().z());
        String dim = game().dimension().replace("the_", "").replace('_', ' ');
        if (chat.on()) {
            platform().showChat("§8[§bCool Client§8] §7You died at §f" + x + " " + y + " " + z
                    + " §7in the " + dim + ".");
        }
        if (waypoint.on()) {
            WaypointManager wm = CoolClient.get().waypoints();
            wm.setDimension(game().dimension());
            for (WaypointManager.Waypoint w : new ArrayList<WaypointManager.Waypoint>(wm.allForServer())) {
                if (WAYPOINT.equals(w.name)) wm.remove(w);
            }
            WaypointManager.Waypoint w = wm.add(WAYPOINT, x, y, z);
            if (w != null) {
                w.color = 0xFFFF5555;
                wm.save();
            }
        }
    }
}
