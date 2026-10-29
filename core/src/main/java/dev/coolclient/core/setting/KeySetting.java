package dev.coolclient.core.setting;

import dev.coolclient.core.util.Keys;

/** A key or mouse-button binding (GLFW codes; mouse = {@link Keys#MOUSE_BASE} + button). */
public final class KeySetting extends Setting<Integer> {
    public KeySetting(String name, int def) {
        super(name, def);
    }

    public int key() { return value; }

    public boolean bound() { return value != Keys.NONE; }

    public String keyName() { return Keys.name(value); }

    @Override
    public Object toJson() { return value; }

    @Override
    public void fromJson(Object json) {
        if (json instanceof Number) set(((Number) json).intValue());
    }
}
