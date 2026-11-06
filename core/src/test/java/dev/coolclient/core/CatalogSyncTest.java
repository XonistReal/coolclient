package dev.coolclient.core;

import dev.coolclient.core.cosmetic.Cosmetic;
import dev.coolclient.core.cosmetic.CosmeticCatalog;
import dev.coolclient.core.cosmetic.CosmeticType;
import dev.coolclient.core.cosmetic.RemoteCatalog;
import dev.coolclient.core.cosmetic.Season;
import dev.coolclient.core.json.Json;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The API server decides who may show which cosmetic from server/catalog.json; it must match the
 * client: every built-in cosmetic with the same tier and season, and every server-delivered one
 * ("remote") understood by the client's loader.
 */
public class CatalogSyncTest {
    private static Map<String, Object> catalog() throws Exception {
        File f = new File("../server/catalog.json");
        return Json.parseObject(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
    }

    @Test
    public void serverCatalogMatchesClient() throws Exception {
        Map<String, Object> root = catalog();
        Map<String, Object> tiers = Json.obj(root, "cosmetics");
        Map<String, Object> remote = Json.obj(root, "remote");
        Map<String, Object> seasonal = Json.obj(root, "seasonal");
        Map<String, Object> seasons = Json.obj(root, "seasons");

        int builtIn = 0;
        for (Cosmetic c : CosmeticCatalog.all()) {
            if (CosmeticCatalog.remote(c.id)) continue;
            builtIn++;
            assertEquals(c.id, c.unlock.name().toLowerCase(Locale.ROOT), tiers.get(c.id));
            assertEquals(c.id + " is built in", false, remote.containsKey(c.id));
            assertEquals(c.id + " season", c.season() == null ? null : c.season().id, seasonal.get(c.id));
        }
        assertEquals("every tier is a built-in or server cosmetic", builtIn + remote.size(), tiers.size());
        for (String id : seasonal.keySet()) {
            assertTrue(id + " has a tier", tiers.containsKey(id));
            assertTrue(id + "'s season exists", seasons.containsKey(String.valueOf(seasonal.get(id))));
        }
        for (Season s : new Season[]{Season.THANKSGIVING}) {
            Map<String, Object> w = Json.obj(seasons, s.id);
            assertNotNull(s.id, w);
            assertEquals(s.id, s.fromKey() + ".." + s.toKey(), w.get("from") + ".." + w.get("to"));
        }
    }

    @Test
    public void clientUnderstandsEveryServerCosmetic() throws Exception {
        Map<String, Object> root = catalog();
        Map<String, Object> tiers = Json.obj(root, "cosmetics");
        Map<String, Object> remote = Json.obj(root, "remote");
        RemoteCatalog loader = new RemoteCatalog(null, null);
        for (String id : remote.keySet()) {
            Map<String, Object> d = new LinkedHashMap<String, Object>(Json.obj(remote, id));
            d.put("id", id);
            d.put("unlock", tiers.get(id));
            Cosmetic c = loader.build(d, false);
            assertNotNull(id + " is understood", c);
            assertEquals(id, CosmeticType.valueOf(String.valueOf(d.get("type"))), c.type);
            for (String file : new String[]{"image", "model", "texture"}) {
                if (d.containsKey(file)) assertTrue(id + " " + file, new File("../server/public/cosmetics/" + d.get(file)).isFile());
            }
        }
    }
}
