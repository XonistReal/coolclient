package dev.coolclient.core;

import dev.coolclient.core.cosmetic.Cosmetic;
import dev.coolclient.core.cosmetic.CosmeticCatalog;
import dev.coolclient.core.cosmetic.Entitlements;
import dev.coolclient.core.cosmetic.Season;

import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Seasonal drops are in everyone's menu during their season, and afterwards only for players who kept one. */
public class SeasonalCosmeticsTest {
    private static final String[] THANKSGIVING = {"cape_autumn", "cape_gobbler", "cape_harvestmoon", "hat_pilgrim", "hat_turkey", "aura_leaves"};

    private static Entitlements owning(String... items) throws Exception {
        Entitlements e = new Entitlements(null);
        Field f = Entitlements.class.getDeclaredField("owned");
        f.setAccessible(true);
        f.set(e, new HashSet<String>(Arrays.asList(items)));
        return e;
    }

    private static long utc(int year, int month, int day, int hour) {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.clear();
        c.set(year, month - 1, day, hour, 0);
        return c.getTimeInMillis();
    }

    @Test
    public void thanksgivingIsNovember() {
        Season s = Season.THANKSGIVING;
        assertEquals(false, s.active(utc(2026, 10, 31, 23)));
        assertEquals(true, s.active(utc(2026, 11, 1, 0)));
        assertEquals(true, s.active(utc(2026, 11, 26, 12)));
        assertEquals(true, s.active(utc(2026, 11, 30, 23)));
        assertEquals(false, s.active(utc(2026, 12, 1, 0)));
        assertEquals(true, s.active(utc(2027, 11, 15, 12)));
        assertEquals("Nov 30", s.lastDay());
        assertEquals("NOV ONLY", s.chip());
        for (String id : THANKSGIVING) assertEquals(id, s, CosmeticCatalog.get(id).season());
    }

    @Test
    public void menuDuringAndAfterTheSeason() throws Exception {
        long during = utc(2026, 11, 20, 12), after = utc(2026, 12, 5, 12);
        Entitlements nobody = owning(), kept = owning("cape_gobbler"), owner = owning("owner");
        for (String id : THANKSGIVING) {
            Cosmetic c = CosmeticCatalog.get(id);
            assertEquals(id, true, nobody.listed(c, during));
            assertEquals(id, false, nobody.owns(c));
            assertEquals(id, false, nobody.listed(c, after));
            assertEquals(id, id.equals("cape_gobbler"), kept.listed(c, after));
            assertTrue(id, owner.listed(c, after));
        }
        // Everything that isn't seasonal is unaffected by the date.
        for (Cosmetic c : CosmeticCatalog.all()) {
            if (c.season() == null) assertEquals(c.id, nobody.listed(c, during), nobody.listed(c, after));
        }
    }
}
