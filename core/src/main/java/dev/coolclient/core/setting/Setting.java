package dev.coolclient.core.setting;

import java.util.function.BooleanSupplier;

/** A single configurable value owned by a module. */
public abstract class Setting<T> {
    private final String id;
    private final String name;
    private String description = "";
    protected T value;
    protected final T defaultValue;
    private BooleanSupplier visibility = () -> true;
    private Runnable changeListener;

    protected Setting(String name, T defaultValue) {
        this.name = name;
        this.id = toId(name);
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    static String toId(String name) {
        StringBuilder sb = new StringBuilder();
        boolean upper = false;
        for (char c : name.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                sb.append(sb.length() == 0 ? Character.toLowerCase(c) : (upper ? Character.toUpperCase(c) : c));
                upper = false;
            } else {
                upper = sb.length() > 0;
            }
        }
        return sb.toString();
    }

    public String id() { return id; }

    public String name() { return name; }

    public String description() { return description; }

    public T get() { return value; }

    public T defaultValue() { return defaultValue; }

    public void set(T v) {
        if (v == null) return;
        T old = value;
        value = sanitize(v);
        if (changeListener != null && !value.equals(old)) changeListener.run();
    }

    public void reset() { set(defaultValue); }

    protected T sanitize(T v) { return v; }

    public boolean visible() { return visibility.getAsBoolean(); }

    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S describe(String text) {
        this.description = text;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S visibleWhen(BooleanSupplier condition) {
        this.visibility = condition;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S onChange(Runnable listener) {
        this.changeListener = listener;
        return (S) this;
    }

    public abstract Object toJson();

    public abstract void fromJson(Object json);
}
