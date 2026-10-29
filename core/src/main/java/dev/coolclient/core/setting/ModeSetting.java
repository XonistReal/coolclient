package dev.coolclient.core.setting;

import java.util.Arrays;
import java.util.List;

public final class ModeSetting extends Setting<String> {
    private final List<String> modes;

    public ModeSetting(String name, String def, String... modes) {
        super(name, def);
        this.modes = Arrays.asList(modes);
    }

    public List<String> modes() { return modes; }

    public boolean is(String mode) { return value.equalsIgnoreCase(mode); }

    public int index() { return Math.max(0, modes.indexOf(value)); }

    public void cycle(int dir) {
        int i = (index() + dir + modes.size()) % modes.size();
        set(modes.get(i));
    }

    @Override
    protected String sanitize(String v) {
        for (String m : modes) if (m.equalsIgnoreCase(v)) return m;
        return defaultValue;
    }

    @Override
    public Object toJson() { return value; }

    @Override
    public void fromJson(Object json) {
        if (json instanceof String) set((String) json);
    }
}
