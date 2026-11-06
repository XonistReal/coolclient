package dev.coolclient.core.cosmetic;

import dev.coolclient.core.cosmetic.cape.CapeCosmetic;
import dev.coolclient.core.cosmetic.cape.ImageCapeStyle;
import dev.coolclient.core.cosmetic.model.ModelCosmetic;
import dev.coolclient.core.json.Json;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Server-delivered cosmetics and seasons: what the client builds from /api/catalog. */
public class RemoteCatalogTest {
    @Test
    public void appliesSeasonsAndCosmetics() {
        RemoteCatalog rc = new RemoteCatalog(null, null);
        rc.apply(Json.parseObject("{"
                + "\"seasons\": {\"testwinter\": {\"name\": \"Test Winter\", \"color\": \"#55AAFF\", \"from\": \"12-01\", \"to\": \"01-15\"}},"
                + "\"seasonal\": {\"halo_test_frost\": \"testwinter\", \"cape_galaxy\": \"testwinter\"},"
                + "\"cosmetics\": ["
                + "  {\"id\": \"halo_test_frost\", \"type\": \"HALO\", \"kind\": \"halo\", \"name\": \"Test Frost\", \"rarity\": \"EPIC\", \"color\": \"#AEE6FF\", \"unlock\": \"free\"},"
                + "  {\"id\": \"aura_test_embers\", \"type\": \"AURA\", \"kind\": \"flame\", \"colors\": [\"#FFEE88\", \"#FF2200\"]},"
                + "  {\"id\": \"cape_test_image\", \"type\": \"CAPE\", \"image\": \"capes/test.png\", \"frames\": 4, \"unlock\": \"supporter\"},"
                + "  {\"id\": \"hat_test_model\", \"type\": \"HAT\", \"model\": \"models/x.json\", \"texture\": \"models/x.png\", \"spin\": 20},"
                + "  {\"id\": \"cape_galaxy\", \"type\": \"CAPE\", \"image\": \"capes/override.png\"},"
                + "  {\"id\": \"wings_test_bad\", \"type\": \"WINGS\", \"kind\": \"halo\"},"
                + "  {\"id\": \"../escape\", \"type\": \"CAPE\", \"image\": \"capes/a.png\"},"
                + "  {\"id\": \"cape_test_path\", \"type\": \"CAPE\", \"image\": \"../../secret.png\"}"
                + "]}"));

        Season winter = Season.get("testwinter");
        assertNotNull(winter);
        assertEquals("Test Winter", winter.displayName);
        assertEquals(0xFF55AAFF, winter.color);
        assertEquals("DEC-JAN", winter.chip());

        Cosmetic halo = CosmeticCatalog.get("halo_test_frost");
        assertEquals(CosmeticType.HALO, halo.type);
        assertEquals(Rarity.EPIC, halo.rarity);
        assertEquals(0xFFAEE6FF, halo.defaultColor);
        assertEquals(winter, halo.season());
        assertTrue(CosmeticCatalog.remote("halo_test_frost"));
        assertTrue(halo.headroom() > 0);

        assertEquals(CosmeticType.AURA, CosmeticCatalog.get("aura_test_embers").type);
        Cosmetic cape = CosmeticCatalog.get("cape_test_image");
        assertTrue(cape instanceof CapeCosmetic && ((CapeCosmetic) cape).style instanceof ImageCapeStyle);
        assertEquals(Unlock.SUPPORTER, cape.unlock);
        assertTrue(CosmeticCatalog.get("hat_test_model") instanceof ModelCosmetic);

        // Built-in cosmetics keep the mod's version, but the server can put them in a season.
        assertTrue(!CosmeticCatalog.remote("cape_galaxy"));
        assertEquals(winter, CosmeticCatalog.get("cape_galaxy").season());
        CosmeticCatalog.get("cape_galaxy").in(null);

        // Nonsense is skipped.
        assertNull(CosmeticCatalog.get("wings_test_bad"));
        assertNull(CosmeticCatalog.get("../escape"));
        assertNull(CosmeticCatalog.get("cape_test_path"));
    }

    @Test
    public void imageCapeFramesAndLayouts() {
        ImageCapeStyle s = new ImageCapeStyle(2, 1f);
        // 2 x 2 face, two frames: frame 0 red, frame 1 blue.
        s.setImage(new int[]{0xFFFF0000, 0xFFFF0000, 0xFFFF0000, 0xFFFF0000, 0xFF0000FF, 0xFF0000FF, 0xFF0000FF, 0xFF0000FF}, 2, 4);
        assertEquals(0xFFFF0000, s.sample(0.5f, 0.5f, 0.2f, 0));
        assertEquals(0xFF0000FF, s.sample(0.5f, 0.5f, 1.2f, 0));
        // A 64 x 32 vanilla cape: the face starts at (1, 1).
        int[] px = new int[64 * 32];
        px[1 * 64 + 1] = 0xFF00FF00;
        ImageCapeStyle v = new ImageCapeStyle(1, 1f);
        v.setImage(px, 64, 32);
        assertEquals(0xFF00FF00, v.sample(0.01f, 0.01f, 0f, 0));
    }

    @Test
    public void pumpkinHatModel() throws Exception {
        File f = new File("../server/public/cosmetics/models/pumpkin_hat.json");
        Map<String, Object> json = Json.parseObject(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
        ModelCosmetic hat = new ModelCosmetic("hat_x", "X", "", CosmeticType.HAT, Rarity.EPIC, -1, Unlock.FREE, false, 0, 0, 3);
        hat.setModel(json);
        // pumpkin (6 faces) + rim (6) + stem (6) + leaf (2)
        assertEquals(20, hat.quadCount());
        // The stem tops out about 8.5 px above the head.
        assertTrue(String.valueOf(hat.headroom()), hat.headroom() > 7.5f && hat.headroom() < 9f);
    }
}
