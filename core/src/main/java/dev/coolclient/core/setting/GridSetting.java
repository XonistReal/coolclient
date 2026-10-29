package dev.coolclient.core.setting;

/** A square pixel grid (e.g. the crosshair editor), stored as a string of '0'/'1'. */
public final class GridSetting extends Setting<String> {
    private final int size;

    public GridSetting(String name, int size, String def) {
        super(name, def);
        this.size = size;
    }

    public int size() { return size; }

    public boolean pixel(int x, int y) {
        int i = y * size + x;
        return i >= 0 && i < value.length() && value.charAt(i) == '1';
    }

    public void setPixel(int x, int y, boolean on) {
        if (x < 0 || y < 0 || x >= size || y >= size) return;
        char[] c = value.toCharArray();
        c[y * size + x] = on ? '1' : '0';
        set(new String(c));
    }

    public void clear() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size * size; i++) sb.append('0');
        set(sb.toString());
    }

    @Override
    protected String sanitize(String v) {
        int n = size * size;
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) sb.append(i < v.length() && v.charAt(i) == '1' ? '1' : '0');
        return sb.toString();
    }

    @Override
    public Object toJson() { return value; }

    @Override
    public void fromJson(Object json) {
        if (json instanceof String) set((String) json);
    }
}
