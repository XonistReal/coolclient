package dev.coolclient.core.cosmetic;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * A seasonal drop. Its cosmetics can be had during a window that comes back every year (UTC
 * dates). Equipping one while signed in during the window keeps it for good (the server records
 * it); outside the window only players who kept one see it in the menu, and others only see it
 * on them. Keep in sync with "seasons" in server/catalog.json (CatalogSyncTest checks it).
 */
public enum Season {
    THANKSGIVING("thanksgiving", "Thanksgiving", 0xFFFF9A3C, 11, 1, 11, 30);

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static final String[] MONTHS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

    public final String id;
    public final String displayName;
    public final int color;
    /** First and last day, as month * 100 + day. */
    private final int from, to;

    Season(String id, String displayName, int color, int fromMonth, int fromDay, int toMonth, int toDay) {
        this.id = id;
        this.displayName = displayName;
        this.color = color;
        this.from = fromMonth * 100 + fromDay;
        this.to = toMonth * 100 + toDay;
    }

    /** Whether the drop is on at this moment (the window may wrap past New Year). */
    public boolean active(long epochMillis) {
        Calendar c = Calendar.getInstance(UTC, Locale.ROOT);
        c.setTimeInMillis(epochMillis);
        int day = (c.get(Calendar.MONTH) + 1) * 100 + c.get(Calendar.DAY_OF_MONTH);
        return from <= to ? day >= from && day <= to : day >= from || day <= to;
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
}
