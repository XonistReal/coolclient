package dev.coolclient.core.util;

public final class MathUtil {
    private MathUtil() {}

    public static float clamp(float v, float min, float max) { return v < min ? min : (v > max ? max : v); }
    public static double clamp(double v, double min, double max) { return v < min ? min : (v > max ? max : v); }
    public static int clamp(int v, int min, int max) { return v < min ? min : (v > max ? max : v); }

    public static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    public static double lerp(double a, double b, double t) { return a + (b - a) * t; }

    public static float wrapDegrees(float deg) {
        deg %= 360f;
        if (deg >= 180f) deg -= 360f;
        if (deg < -180f) deg += 360f;
        return deg;
    }

    public static double roundTo(double v, double step) {
        if (step <= 0) return v;
        return Math.round(v / step) * step;
    }

    public static String format(double v, int decimals) {
        if (decimals <= 0) return String.valueOf(Math.round(v));
        double p = Math.pow(10, decimals);
        double r = Math.round(v * p) / p;
        String s = String.valueOf(r);
        int dot = s.indexOf('.');
        if (dot < 0) { s += "."; dot = s.length() - 1; }
        while (s.length() - dot - 1 < decimals) s += "0";
        return s;
    }

    public static boolean inside(double px, double py, double x, double y, double w, double h) {
        return px >= x && py >= y && px < x + w && py < y + h;
    }

    /** Time-based smoothing factor that is independent of frame rate. */
    public static float smooth(float speed, float deltaSeconds) {
        return 1f - (float) Math.exp(-speed * deltaSeconds);
    }
}
