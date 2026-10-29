package dev.coolclient.core.setting;

public final class BoolSetting extends Setting<Boolean> {
    public BoolSetting(String name, boolean def) {
        super(name, def);
    }

    public boolean on() { return value; }

    public void toggle() { set(!value); }

    @Override
    public Object toJson() { return value; }

    @Override
    public void fromJson(Object json) {
        if (json instanceof Boolean) set((Boolean) json);
    }
}
