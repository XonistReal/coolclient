package dev.coolclient.core.setting;

import dev.coolclient.core.util.MathUtil;

public final class NumberSetting extends Setting<Double> {
    private final double min;
    private final double max;
    private final double step;
    private String suffix = "";

    public NumberSetting(String name, double def, double min, double max, double step) {
        super(name, def);
        this.min = min;
        this.max = max;
        this.step = step;
    }

    public NumberSetting suffix(String suffix) {
        this.suffix = suffix;
        return this;
    }

    public double min() { return min; }

    public double max() { return max; }

    public double step() { return step; }

    public String suffix() { return suffix; }

    public float f() { return value.floatValue(); }

    public int i() { return (int) Math.round(value); }

    public double d() { return value; }

    /** 0..1 position of the value within its range. */
    public double fraction() { return max == min ? 0 : (value - min) / (max - min); }

    public void setFraction(double f) { set(min + (max - min) * MathUtil.clamp(f, 0, 1)); }

    public String display() {
        int decimals = step >= 1 ? 0 : (step >= 0.1 ? 1 : 2);
        return MathUtil.format(value, decimals) + suffix;
    }

    @Override
    protected Double sanitize(Double v) {
        return MathUtil.clamp(MathUtil.roundTo(v, step), min, max);
    }

    @Override
    public Object toJson() { return value; }

    @Override
    public void fromJson(Object json) {
        if (json instanceof Number) set(((Number) json).doubleValue());
    }
}
