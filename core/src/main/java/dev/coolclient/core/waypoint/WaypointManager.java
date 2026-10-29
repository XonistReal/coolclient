package dev.coolclient.core.waypoint;

import dev.coolclient.core.config.ConfigManager;
import dev.coolclient.core.json.Json;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Personal waypoints, stored per server (or "singleplayer") and per dimension. */
public final class WaypointManager {
    public static final class Waypoint {
        public String name;
        public int x, y, z;
        public int color;
        public boolean visible = true;
        public String dimension;

        public Waypoint(String name, int x, int y, int z, int color, String dimension) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
            this.color = color;
            this.dimension = dimension;
        }
    }

    private static final int[] PALETTE = {0xFF2DE2C1, 0xFF8A5CFF, 0xFFFF5470, 0xFFFFB547, 0xFF4DA3FF, 0xFF5BE37A};

    private final File file;
    private final Map<String, List<Waypoint>> byServer = new LinkedHashMap<String, List<Waypoint>>();
    private String server = "singleplayer";
    private String dimension = "overworld";
    private boolean dirty;

    public WaypointManager(File dir) {
        this.file = new File(dir, "waypoints.json");
    }

    public void setContext(String serverAddress, String dim) {
        this.server = serverAddress == null ? "singleplayer" : serverAddress.toLowerCase();
        this.dimension = dim == null ? "overworld" : dim;
    }

    public void setDimension(String dim) {
        if (dim != null) this.dimension = dim;
    }

    public List<Waypoint> current() {
        List<Waypoint> out = new ArrayList<Waypoint>();
        List<Waypoint> all = byServer.get(server);
        if (all == null) return out;
        for (Waypoint w : all) if (w.visible && dimension.equals(w.dimension)) out.add(w);
        return out;
    }

    public List<Waypoint> allForServer() {
        List<Waypoint> l = byServer.get(server);
        return l == null ? new ArrayList<Waypoint>() : l;
    }

    public Waypoint add(String name, int x, int y, int z) {
        List<Waypoint> l = byServer.get(server);
        if (l == null) {
            l = new ArrayList<Waypoint>();
            byServer.put(server, l);
        }
        Waypoint w = new Waypoint(name, x, y, z, PALETTE[l.size() % PALETTE.length], dimension);
        l.add(w);
        save();
        return w;
    }

    public void remove(Waypoint w) {
        List<Waypoint> l = byServer.get(server);
        if (l != null && l.remove(w)) save();
    }

    public void clear() {
        byServer.remove(server);
        save();
    }

    @SuppressWarnings("unchecked")
    public void load() {
        Map<String, Object> root = ConfigManager.readJson(file);
        if (root == null) return;
        for (Map.Entry<String, Object> e : root.entrySet()) {
            if (!(e.getValue() instanceof List)) continue;
            List<Waypoint> list = new ArrayList<Waypoint>();
            for (Object o : (List<Object>) e.getValue()) {
                if (!(o instanceof Map)) continue;
                Map<String, Object> m = (Map<String, Object>) o;
                Waypoint w = new Waypoint(Json.str(m, "name", "Waypoint"), (int) Json.num(m, "x", 0),
                        (int) Json.num(m, "y", 64), (int) Json.num(m, "z", 0),
                        (int) Long.parseLong(Json.str(m, "color", "FF2DE2C1"), 16), Json.str(m, "dimension", "overworld"));
                w.visible = Json.bool(m, "visible", true);
                list.add(w);
            }
            byServer.put(e.getKey(), list);
        }
    }

    public void save() {
        dirty = true;
        saveNow();
    }

    public void saveNow() {
        if (!dirty) return;
        Map<String, Object> root = new LinkedHashMap<String, Object>();
        for (Map.Entry<String, List<Waypoint>> e : byServer.entrySet()) {
            List<Object> list = new ArrayList<Object>();
            for (Waypoint w : e.getValue()) {
                Map<String, Object> m = new LinkedHashMap<String, Object>();
                m.put("name", w.name);
                m.put("x", w.x);
                m.put("y", w.y);
                m.put("z", w.z);
                m.put("color", String.format("%08X", w.color));
                m.put("dimension", w.dimension);
                m.put("visible", w.visible);
                list.add(m);
            }
            root.put(e.getKey(), list);
        }
        ConfigManager.write(file, Json.pretty(root));
        dirty = false;
    }
}
