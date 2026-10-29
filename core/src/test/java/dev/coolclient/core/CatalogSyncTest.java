package dev.coolclient.core;

import dev.coolclient.core.cosmetic.Cosmetic;
import dev.coolclient.core.cosmetic.CosmeticCatalog;
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
    }
}
