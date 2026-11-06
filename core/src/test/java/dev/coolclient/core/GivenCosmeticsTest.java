package dev.coolclient.core;

import dev.coolclient.core.cosmetic.Cosmetic;
import dev.coolclient.core.cosmetic.CosmeticCatalog;
import dev.coolclient.core.cosmetic.Entitlements;

import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;

/** Given cosmetics (Founder, Media, Owner) only show in the menu for players who have them. */
public class GivenCosmeticsTest {
    private static Entitlements owning(String... items) throws Exception {
        Entitlements e = new Entitlements(null);
        Field f = Entitlements.class.getDeclaredField("owned");
        f.setAccessible(true);
        f.set(e, new HashSet<String>(Arrays.asList(items)));
        return e;
    }

    private static boolean listed(Entitlements e, String id) {
        return e.listed(CosmeticCatalog.get(id));
    }

    @Test
    public void menuHidesGivenCosmeticsYouDontHave() throws Exception {
        Entitlements nobody = owning();
        for (String id : new String[] {"cape_founder", "cape_media", "cape_onair", "cape_premiere", "cape_owner"}) {
            assertEquals(id, false, listed(nobody, id));
        }
        for (Cosmetic c : CosmeticCatalog.all()) {
            if (c.unlock.sold() && c.season() == null) assertEquals(c.id, true, nobody.listed(c));
        }

        Entitlements media = owning("media");
        assertEquals(true, listed(media, "cape_media"));
        assertEquals(true, listed(media, "cape_premiere"));
        assertEquals(false, listed(media, "cape_owner"));
        assertEquals(false, listed(media, "cape_founder"));

        Entitlements founder = owning("cape_founder");
        assertEquals(true, listed(founder, "cape_founder"));
        assertEquals(false, listed(founder, "cape_media"));

        Entitlements owner = owning("owner");
        for (Cosmetic c : CosmeticCatalog.all()) assertEquals(c.id, true, owner.listed(c));
    }
}
