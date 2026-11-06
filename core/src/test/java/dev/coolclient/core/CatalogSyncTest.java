package dev.coolclient.core;

import dev.coolclient.core.cosmetic.Cosmetic;
import dev.coolclient.core.cosmetic.CosmeticCatalog;
import dev.coolclient.core.cosmetic.Season;
import dev.coolclient.core.json.Json;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/** The API server decides who may show which cosmetic from server/catalog.json; it must match the client. */
public class CatalogSyncTest {
    @Test
    public void serverCatalogMatchesClient() throws Exception {
        File f = new File("../server/catalog.json");
        Map<String, Object> root = Json.parseObject(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
        Map<String, Object> tiers = Json.obj(root, "cosmetics");
        assertEquals("cosmetic count", CosmeticCatalog.all().size(), tiers.size());
        for (Cosmetic c : CosmeticCatalog.all()) {
            assertEquals(c.id, c.unlock.name().toLowerCase(Locale.ROOT), tiers.get(c.id));
        }
        // Seasonal drops: same cosmetics in the same seasons, with the same dates.
        Map<String, Object> seasonal = Json.obj(root, "seasonal");
        int count = 0;
        for (Cosmetic c : CosmeticCatalog.all()) {
            if (c.season() != null) count++;
            assertEquals(c.id + " season", c.season() == null ? null : c.season().id, seasonal.get(c.id));
        }
        assertEquals("seasonal count", count, seasonal.size());
        Map<String, Object> seasons = Json.obj(root, "seasons");
        assertEquals("season count", Season.values().length, seasons.size());
        for (Season s : Season.values()) {
            Map<String, Object> w = Json.obj(seasons, s.id);
            assertEquals(s.id, s.fromKey() + ".." + s.toKey(), w.get("from") + ".." + w.get("to"));
        }
    }
}
