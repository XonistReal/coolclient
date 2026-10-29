package dev.coolclient.core.setting;

import dev.coolclient.core.json.Json;
import dev.coolclient.core.render.Colors;

import java.util.LinkedHashMap;
import java.util.Map;

/** ARGB colour with optional animated chroma (rainbow) mode. */
public final class ColorSetting extends Setting<Integer> {
    private boolean chroma;
    private double chromaSpeed = 1.0;
    private final boolean allowAlpha;

    public ColorSetting(String name, int argb) {
        this(name, argb, true);
    }

    public ColorSetting(String name, int argb, boolean allowAlpha) {
        super(name, argb);
        this.allowAlpha = allowAlpha;
    }

    public boolean allowAlpha() { return allowAlpha; }

    public boolean chroma() { return chroma; }

    public void setChroma(boolean chroma) { this.chroma = chroma; }

    public double chromaSpeed() { return chromaSpeed; }

    public void setChromaSpeed(double speed) { this.chromaSpeed = Math.max(0.1, Math.min(5, speed)); }

    /** The colour to draw right now (chroma-animated if enabled). */
    public int color() { return color(0f); }

    /** Chroma colour with a phase offset, e.g. based on screen x for a moving rainbow. */
    public int color(float offset) {
        if (!chroma) return value;
        int rgb = Colors.chroma(chromaSpeed, offset);
        return (value & 0xFF000000) | (rgb & 0x00FFFFFF);
    }

    @Override
    public void reset() {
        super.reset();
        chroma = false;
        chromaSpeed = 1.0;
    }

    @Override
    public Object toJson() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("argb", String.format("%08X", value));
        m.put("chroma", chroma);
        m.put("speed", chromaSpeed);
        return m;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void fromJson(Object json) {
        if (!(json instanceof Map)) return;
        Map<String, Object> m = (Map<String, Object>) json;
        try {
            set((int) Long.parseLong(Json.str(m, "argb", String.format("%08X", defaultValue)), 16));
        } catch (NumberFormatException ignored) {
        }
        chroma = Json.bool(m, "chroma", false);
        chromaSpeed = Json.num(m, "speed", 1.0);
    }
}
