package dev.coolclient.core.modules.hud;

import dev.coolclient.core.api.GuiRenderer;
import dev.coolclient.core.api.ItemRef;
import dev.coolclient.core.event.Events;
import dev.coolclient.core.font.FontRenderer;
import dev.coolclient.core.font.Fonts;
import dev.coolclient.core.hud.HudElement;
import dev.coolclient.core.render.Colors;
import dev.coolclient.core.render.Draw;
import dev.coolclient.core.setting.BoolSetting;
import dev.coolclient.core.setting.NumberSetting;
import dev.coolclient.core.util.Time;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** A short log of items entering (and leaving) your inventory: "+3 Cobblestone". */
public final class PickupLogHud extends HudElement {
    private static final class Entry {
        final String key, name;
        final ItemRef icon;
        int delta;
        long at;

        Entry(String key, String name, ItemRef icon, int delta) {
            this.key = key;
            this.name = name;
            this.icon = icon;
            this.delta = delta;
            this.at = Time.millis();
        }
    }

    private final BoolSetting removed = add(new BoolSetting("Show Removed Items", true));
    private final NumberSetting duration = add(new NumberSetting("Show For", 4, 1, 15, 1).suffix("s"));
    private final NumberSetting maxRows = add(new NumberSetting("Max Rows", 6, 1, 12, 1));
    private final BoolSetting background = add(new BoolSetting("Background", true));

    private final List<Entry> entries = new ArrayList<Entry>();
    private Map<String, Integer> last;
    private final Map<String, Object[]> seen = new HashMap<String, Object[]>();

    public PickupLogHud() {
        super("Pickup Log", "Lists items you pick up or lose, for a few seconds each.");
        defaultPosition(Anchor.MIDDLE_RIGHT, -4, 30);
        on(Events.Tick.class, e -> tick());
        on(Events.WorldJoin.class, e -> last = null);
        on(Events.WorldLeave.class, e -> {
            last = null;
            entries.clear();
        });
    }

    private void tick() {
        if (!game().inWorld() || game().health() <= 0) {
            last = null; // dying empties the inventory; don't log it
            return;
        }
        Map<String, Integer> now = new HashMap<String, Integer>();
        for (ItemRef s : game().inventory()) {
            String key = s.id() + "|" + s.displayName();
            Integer c = now.get(key);
            now.put(key, (c == null ? 0 : c) + s.count());
            if (!seen.containsKey(key)) seen.put(key, new Object[] {s.displayName(), game().itemOf(s.id())});
        }
        if (last != null) {
            for (Map.Entry<String, Integer> e : now.entrySet()) {
                Integer before = last.get(e.getKey());
                int d = e.getValue() - (before == null ? 0 : before);
                if (d != 0) log(e.getKey(), d);
            }
            for (Map.Entry<String, Integer> e : last.entrySet()) {
                if (!now.containsKey(e.getKey())) log(e.getKey(), -e.getValue());
            }
        }
        last = now;
        long cutoff = Time.millis() - (long) (duration.d() * 1000);
        for (Iterator<Entry> it = entries.iterator(); it.hasNext(); ) if (it.next().at < cutoff) it.remove();
    }

    private void log(String key, int delta) {
        if (delta < 0 && !removed.on()) return;
        for (Entry e : entries) {
            // Merge with a recent line for the same item going the same way.
            if (e.key.equals(key) && Integer.signum(e.delta) == Integer.signum(delta)) {
                e.delta += delta;
                e.at = Time.millis();
                return;
            }
        }
        Object[] info = seen.get(key);
        String name = info == null ? key : (String) info[0];
        ItemRef icon = info == null ? ItemRef.EMPTY : (ItemRef) info[1];
        entries.add(0, new Entry(key, name, icon, delta));
        while (entries.size() > 24) entries.remove(entries.size() - 1);
    }

    @Override
    public boolean shouldRender() { return !entries.isEmpty(); }

    @Override
    protected void draw(GuiRenderer r, boolean editing) {
        List<Entry> rows = entries;
        if (rows.isEmpty() && editing) {
            rows = new ArrayList<Entry>();
            rows.add(new Entry("a", "Cobblestone", game().itemOf("minecraft:cobblestone"), 12));
            rows.add(new Entry("b", "Ender Pearl", game().itemOf("minecraft:ender_pearl"), 2));
            rows.add(new Entry("c", "Arrow", game().itemOf("minecraft:arrow"), -5));
        }
        FontRenderer f = Fonts.vanilla();
        int n = Math.min(rows.size(), maxRows.i());
        float w = 60;
        for (int i = 0; i < n; i++) w = Math.max(w, 22 + f.width(label(rows.get(i))) + 4);
        float h = Math.max(18, n * 18);
        size(w, h);
        long now = Time.millis();
        float life = (float) (duration.d() * 1000);
        for (int i = 0; i < n; i++) {
            Entry e = rows.get(i);
            float left = editing && entries.isEmpty() ? 1f : 1f - (now - e.at) / life;
            float a = Math.max(0f, Math.min(1f, left * 4f));
            float y = i * 18;
            if (background.on()) Draw.rounded(r, 0, y, w, 17, 3, Colors.fade(0x70000000, a));
            if (a > 0.5f) r.item(e.icon, 2, y + 0.5f, false);
            int c = e.delta > 0 ? 0xFF7CFF8A : 0xFFFF6B6B;
            f.draw(r, label(e), 21, y + 4.5f, Colors.fade(c, a), true);
        }
    }

    private static String label(Entry e) {
        return (e.delta > 0 ? "+" : "") + e.delta + " " + e.name;
    }
}
