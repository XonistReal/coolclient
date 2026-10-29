package dev.coolclient.core.setting;

public final class TextSetting extends Setting<String> {
    private final int maxLength;

    public TextSetting(String name, String def) {
        this(name, def, 256);
    }

    public TextSetting(String name, String def, int maxLength) {
        super(name, def);
        this.maxLength = maxLength;
    }

    public int maxLength() { return maxLength; }

    @Override
    protected String sanitize(String v) {
        return v.length() > maxLength ? v.substring(0, maxLength) : v;
    }

    @Override
    public Object toJson() { return value; }

    @Override
    public void fromJson(Object json) {
        if (json instanceof String) set((String) json);
    }
}
