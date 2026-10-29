package dev.coolclient.core.api;

/** A potion/status effect currently on the local player. */
public final class EffectInfo {
    public final String id;
    public final String name;
    public final int amplifier;
    public final int durationTicks;
    public final boolean beneficial;
    /** Platform object used to draw the icon. */
    public final Object handle;

    public EffectInfo(String id, String name, int amplifier, int durationTicks, boolean beneficial, Object handle) {
        this.id = id;
        this.name = name;
        this.amplifier = amplifier;
        this.durationTicks = durationTicks;
        this.beneficial = beneficial;
        this.handle = handle;
    }

    public boolean infinite() { return durationTicks < 0 || durationTicks > 20 * 60 * 60 * 24; }

    public String durationText() {
        if (infinite()) return "**:**";
        int secs = durationTicks / 20;
        return String.format("%d:%02d", secs / 60, secs % 60);
    }

    public String levelText() {
        switch (amplifier) {
            case 0: return "";
            case 1: return " II";
            case 2: return " III";
            case 3: return " IV";
            case 4: return " V";
            default: return " " + (amplifier + 1);
        }
    }
}
