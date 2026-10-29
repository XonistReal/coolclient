package dev.coolclient.core.cosmetic;

import dev.coolclient.core.json.Json;
import dev.coolclient.core.render.Colors;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** What a player has equipped, plus per-cosmetic colour overrides. */
public final class Loadout {
    private final EnumMap<CosmeticType, String> equipped = new EnumMap<CosmeticType, String>(CosmeticType.class);
    private final Map<String, Integer> colors = new HashMap<String, Integer>();
    private final Map<String, Boolean> chroma = new HashMap<String, Boolean>();

    public String equipped(CosmeticType type) { return equipped.get(type); }

    public Cosmetic cosmetic(CosmeticType type) {
        String id = equipped.get(type);
        return id == null ? null : CosmeticCatalog.get(id);
    }

    public boolean isEquipped(Cosmetic c) { return c.id.equals(equipped.get(c.type)); }

    public void equip(Cosmetic c) { equipped.put(c.type, c.id); }

    public void unequip(CosmeticType type) { equipped.remove(type); }

    public boolean isEmpty() { return equipped.isEmpty(); }

    public int baseColor(Cosmetic c) {
        Integer v = colors.get(c.id);
        return v == null ? c.defaultColor : v;
    }

    /** Colour to render with (animated if chroma is on). */
    public int color(Cosmetic c) {
        if (Boolean.TRUE.equals(chroma.get(c.id))) {
            return (baseColor(c) & 0xFF000000) | (Colors.chroma(0.6, 0) & 0xFFFFFF);
        }
        return baseColor(c);
    }

    public boolean chroma(Cosmetic c) { return Boolean.TRUE.equals(chroma.get(c.id)); }

    public void setColor(Cosmetic c, int argb) { colors.put(c.id, argb); }

    public void setChroma(Cosmetic c, boolean on) { chroma.put(c.id, on); }

    public Map<String, Object> toJson() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        Map<String, Object> eq = new LinkedHashMap<String, Object>();
        for (Map.Entry<CosmeticType, String> e : equipped.entrySet()) eq.put(e.getKey().name(), e.getValue());
        m.put("equipped", eq);
        Map<String, Object> col = new LinkedHashMap<String, Object>();
        for (Map.Entry<String, Integer> e : colors.entrySet()) col.put(e.getKey(), String.format("%08X", e.getValue()));
        m.put("colors", col);
        Map<String, Object> ch = new LinkedHashMap<String, Object>();
        for (Map.Entry<String, Boolean> e : chroma.entrySet()) if (e.getValue()) ch.put(e.getKey(), true);
        m.put("chroma", ch);
        return m;
    }

    public static Loadout fromJson(Map<String, Object> m) {
        Loadout l = new Loadout();
        if (m == null) return l;
        Map<String, Object> eq = Json.obj(m, "equipped");
        if (eq != null) {
            for (Map.Entry<String, Object> e : eq.entrySet()) {
                try {
                    CosmeticType t = CosmeticType.valueOf(e.getKey());
                    Cosmetic c = e.getValue() instanceof String ? CosmeticCatalog.get((String) e.getValue()) : null;
                    if (c != null && c.type == t) l.equipped.put(t, c.id);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        Map<String, Object> col = Json.obj(m, "colors");
        if (col != null) {
            for (Map.Entry<String, Object> e : col.entrySet()) {
                if (e.getValue() instanceof String) {
                    try {
                        l.colors.put(e.getKey(), (int) Long.parseLong((String) e.getValue(), 16));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        Map<String, Object> ch = Json.obj(m, "chroma");
        if (ch != null) {
            for (Map.Entry<String, Object> e : ch.entrySet()) {
                if (Boolean.TRUE.equals(e.getValue())) l.chroma.put(e.getKey(), true);
            }
        }
        return l;
    }
}
