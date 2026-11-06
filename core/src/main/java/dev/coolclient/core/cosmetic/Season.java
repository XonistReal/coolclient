package dev.coolclient.core.cosmetic;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * A seasonal drop. Its cosmetics can be had during a window that comes back every year (UTC
 * dates). Equipping one while signed in during the window keeps it for good (the server records
 * it); outside the window only players who kept one see it in the menu, and others only see it
 * on them.
 *
 * <p>Seasons come from the server's catalog ({@link RemoteCatalog}), so a new drop or new dates
 * need no mod update; the ones defined here are the built-in fallback for offline play. Keep
 * them in sync with "seasons" in server/catalog.json (CatalogSyncTest checks it).
 */
public final class Season {
    private static final Map<String, Season> ALL = new LinkedHashMap<String, Season>();
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static final String[] MONTHS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

    public static final Season THANKSGIVING = define("thanksgiving", "Thanksgiving", 0xFFFF9A3C, "11-01", "11-30");

    public final String id;
    /** Shown in the menu; the server can rename it. */
    public volatile String displayName;
    public volatile int color;
    /** First and last day, as month * 100 + day. */
    private volatile int from, to;

    private Season(String id) {
        this.id = id;
    }

    /**
     * Adds a season, or updates its name, colour and dates when it already exists. Dates are
     * "MM-DD"; returns null (and changes nothing) when they don't parse.
     */
    public static synchronized Season define(String id, String name, int color, String fromKey, String toKey) {
        int f = parse(fromKey), t = parse(toKey);
        if (id == null || id.isEmpty() || f < 0 || t < 0) return null;
        Season s = ALL.get(id);
        if (s == null) {
            s = new Season(id);
            ALL.put(id, s);
        }
        s.displayName = name == null || name.isEmpty() ? id : name;
        s.color = color;
        s.from = f;
        s.to = t;
        return s;
    }

    public static synchronized Season get(String id) {
        return ALL.get(id);
    }

    public static synchronized List<Season> values() {
        return Collections.unmodifiableList(new ArrayList<Season>(ALL.values()));
    }

    private static int parse(String key) {
        if (key == null || !key.matches("\\d{2}-\\d{2}")) return -1;
        int m = Integer.parseInt(key.substring(0, 2)), d = Integer.parseInt(key.substring(3));
        return m >= 1 && m <= 12 && d >= 1 && d <= 31 ? m * 100 + d : -1;
    }

    /** Whether the drop is on at this moment (the window may wrap past New Year). */
    public boolean active(long epochMillis) {
        Calendar c = Calendar.getInstance(UTC, Locale.ROOT);
        c.setTimeInMillis(epochMillis);
        int day = (c.get(Calendar.MONTH) + 1) * 100 + c.get(Calendar.DAY_OF_MONTH);
        int f = from, t = to;
        return f <= t ? day >= f && day <= t : day >= f || day <= t;
    }

    public boolean active() {
        return active(System.currentTimeMillis());
    }

    /** The last day as shown to players, e.g. "Nov 30". */
    public String lastDay() {
        return MONTHS[to / 100 - 1] + " " + to % 100;
    }

    /** Short label for the menu's tier pill, e.g. "NOV ONLY". */
    public String chip() {
        String a = MONTHS[from / 100 - 1].toUpperCase(Locale.ROOT), b = MONTHS[to / 100 - 1].toUpperCase(Locale.ROOT);
        return a.equals(b) ? a + " ONLY" : a + "-" + b;
    }

    /** The window as catalog.json writes it: "MM-DD". */
    public String fromKey() { return key(from); }

    public String toKey() { return key(to); }

    private static String key(int monthDay) {
        return String.format(Locale.ROOT, "%02d-%02d", monthDay / 100, monthDay % 100);
    }

    @Override
    public String toString() { return id; }
}
