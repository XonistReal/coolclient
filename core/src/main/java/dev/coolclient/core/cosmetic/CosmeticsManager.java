package dev.coolclient.core.cosmetic;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.api.ModelRenderer;
import dev.coolclient.core.config.ConfigManager;
import dev.coolclient.core.cosmetic.cape.CapeCosmetic;
import dev.coolclient.core.cosmetic.cape.CapeTextures;
import dev.coolclient.core.json.Json;
import dev.coolclient.core.net.ApiClient;
import dev.coolclient.core.util.Log;
import dev.coolclient.core.util.Time;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Local loadout (always available, even offline) plus a cache of other players' loadouts
 * fetched from the Cool Client API.
 */
public final class CosmeticsManager {
    private static final long REMOTE_TTL = 5 * 60_000L;
    private static final long LOOKUP_INTERVAL = 2_000L;
    private static final long HEARTBEAT_INTERVAL = 60_000L;

    private static final class Remote {
        Loadout loadout;
        boolean clientUser;
        boolean supporter;
        long fetched;
    }

    private final File file;
    private final ApiClient api;
    private Loadout local = new Loadout();
    private final Map<UUID, Remote> remote = new HashMap<UUID, Remote>();
    private CapeTextures capes;
    private long lastLookup;
    private long lastHeartbeat;
    private long lastEvict;
    private boolean lookupInFlight;
    private boolean showOwn = true;
    private boolean dirty;
    private long dirtySince;

    private final Entitlements entitlements;

    public CosmeticsManager(File dir, ApiClient api) {
        this.file = new File(dir, "cosmetics.json");
        this.api = api;
        this.entitlements = new Entitlements(api);
    }

    /** What the signed-in account owns (store unlocks, supporter rank). */
    public Entitlements entitlements() { return entitlements; }

    /** Supporters get a gold badge instead of the usual one. */
    public boolean isSupporter(UUID uuid) {
        if (uuid == null) return false;
        if (uuid.equals(CoolClient.get().platform().game().selfUuid())) return entitlements.supporter();
        Remote r = remote.get(uuid);
        return r != null && r.supporter;
    }

    // ------------------------------------------------------------------ local loadout

    public Loadout local() { return local; }

    public boolean showOwn() { return showOwn; }

    public void setShowOwn(boolean v) {
        showOwn = v;
        saveNow();
    }

    public void equip(Cosmetic c) {
        local.equip(c);
        changed();
    }

    public void unequip(CosmeticType t) {
        local.unequip(t);
        changed();
    }

    public void toggle(Cosmetic c) {
        if (local.isEquipped(c)) unequip(c.type); else equip(c);
    }

    public void setColor(Cosmetic c, int argb) {
        local.setColor(c, argb);
        changed();
    }

    public void setChroma(Cosmetic c, boolean on) {
        local.setChroma(c, on);
        changed();
    }

    /** Debounced: colour pickers fire on every drag frame. Flushed from {@link #tick}. */
    private void changed() {
        dirty = true;
        dirtySince = Time.millis();
    }

    /** Uploads the local loadout if signed in. */
    public void pushLoadout() {
        if (!api.authenticated()) return;
        api.async("PUT", "/api/me/loadout", local.toJson(), true, null);
    }

    // ------------------------------------------------------------------ queries

    public Loadout loadoutOf(UUID uuid) {
        if (uuid == null) return null;
        UUID self = CoolClient.get().platform().game().selfUuid();
        if (uuid.equals(self)) return showOwn ? local : null;
        Remote r = remote.get(uuid);
        return r == null ? null : r.loadout;
    }

    /** Blocks to raise a player's nametag so it clears their hat or halo. */
    public float nametagLift(UUID uuid) {
        Loadout l = loadoutOf(uuid);
        if (l == null) return 0;
        float px = 0;
        Cosmetic hat = l.cosmetic(CosmeticType.HAT), halo = l.cosmetic(CosmeticType.HALO);
        if (hat != null) px = Math.max(px, hat.headroom());
        if (halo != null) px = Math.max(px, halo.headroom());
        return px / 16f;
    }

    public boolean isClientUser(UUID uuid) {
        if (uuid == null) return false;
        if (uuid.equals(CoolClient.get().platform().game().selfUuid())) return true;
        Remote r = remote.get(uuid);
        return r != null && r.clientUser;
    }

    /** Cape texture handle for a player, or -1 for none. */
    public int capeTexture(UUID uuid) {
        Loadout l = loadoutOf(uuid);
        if (l == null) return -1;
        Cosmetic c = l.cosmetic(CosmeticType.CAPE);
        if (!(c instanceof CapeCosmetic)) return -1;
        return capes().get((CapeCosmetic) c, l.color(c));
    }

    public int capeTexture(CapeCosmetic cape, int tint) {
        return capes().get(cape, tint);
    }

    private CapeTextures capes() {
        if (capes == null) capes = new CapeTextures(CoolClient.get().platform().textures());
        return capes;
    }

    // ------------------------------------------------------------------ rendering

    /** Called by the platform with the pose at the player's head. */
    public void renderHead(UUID uuid, ModelRenderer mr, CosmeticContext ctx) {
        Loadout l = loadoutOf(uuid);
        if (l == null) return;
        render(l, CosmeticType.HAT, mr, ctx);
        render(l, CosmeticType.HALO, mr, ctx);
        render(l, CosmeticType.BANDANA, mr, ctx);
    }

    /** Called by the platform with the pose at the player's body. */
    public void renderBody(UUID uuid, ModelRenderer mr, CosmeticContext ctx) {
        Loadout l = loadoutOf(uuid);
        if (l == null) return;
        render(l, CosmeticType.WINGS, mr, ctx);
        render(l, CosmeticType.AURA, mr, ctx);
    }

    private void render(Loadout l, CosmeticType type, ModelRenderer mr, CosmeticContext ctx) {
        Cosmetic c = l.cosmetic(type);
        if (c == null) return;
        mr.push();
        mr.scale(1 / 16f, 1 / 16f, 1 / 16f);
        try {
            c.render(mr, ctx, l.color(c));
        } catch (Throwable t) {
            Log.error("Cosmetic " + c.id + " failed to render", t);
        } finally {
            mr.pop();
        }
    }

    // ------------------------------------------------------------------ sync

    public void onWorldJoin() {
        lastHeartbeat = 0;
    }

    public void tick() {
        long now = Time.millis();
        entitlements.refresh(false);
        if (dirty && now - dirtySince > 750) {
            dirty = false;
            saveNow();
            pushLoadout();
        }
        if (capes != null && now - lastEvict > 10_000) {
            capes.evictUnused();
            lastEvict = now;
        }
        if (!api.enabled() || !CoolClient.get().platform().game().inWorld()) return;
        if (api.authenticated() && now - lastHeartbeat > HEARTBEAT_INTERVAL) {
            lastHeartbeat = now;
            api.async("POST", "/api/heartbeat", new LinkedHashMap<String, Object>(), true, null);
        }
        // A failed request never calls back, so treat in-flight lookups older than 15s as dead.
        if (lookupInFlight && now - lastLookup < 15_000) return;
        if (now - lastLookup < LOOKUP_INTERVAL) return;
        lastLookup = now;
        final List<Object> ids = new ArrayList<Object>();
        UUID self = CoolClient.get().platform().game().selfUuid();
        for (UUID u : CoolClient.get().platform().game().loadedPlayers()) {
            if (u.equals(self)) continue;
            Remote r = remote.get(u);
            if (r == null || now - r.fetched > REMOTE_TTL) {
                ids.add(u.toString());
                if (ids.size() >= 100) break;
            }
        }
        if (ids.isEmpty()) return;
        // Mark as fetched now so failures don't cause a request storm.
        for (Object o : ids) {
            UUID u = UUID.fromString((String) o);
            Remote r = remote.get(u);
            if (r == null) {
                r = new Remote();
                remote.put(u, r);
            }
            r.fetched = now;
        }
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("uuids", ids);
        lookupInFlight = true;
        api.async("POST", "/api/players", body, false, res -> {
            lookupInFlight = false;
            Map<String, Object> players = Json.obj(res, "players");
            if (players == null) return;
            for (Map.Entry<String, Object> e : players.entrySet()) {
                try {
                    UUID u = UUID.fromString(e.getKey());
                    Remote r = remote.get(u);
                    if (r == null) {
                        r = new Remote();
                        remote.put(u, r);
                    }
                    @SuppressWarnings("unchecked")
                    Map<String, Object> p = e.getValue() instanceof Map ? (Map<String, Object>) e.getValue() : null;
                    r.clientUser = true;
                    r.supporter = p != null && Boolean.TRUE.equals(p.get("supporter"));
                    r.loadout = Loadout.fromJson(Json.obj(p, "loadout"));
                    r.fetched = Time.millis();
                } catch (IllegalArgumentException ignored) {
                }
            }
        });
    }

    // ------------------------------------------------------------------ persistence

    public void load() {
        Map<String, Object> m = ConfigManager.readJson(file);
        if (m == null) {
            // Start new players with something to show off.
            local.equip(CosmeticCatalog.get("cape_aurora"));
            return;
        }
        local = Loadout.fromJson(Json.obj(m, "loadout"));
        showOwn = Json.bool(m, "showOwn", true);
    }

    public void saveNow() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("loadout", local.toJson());
        m.put("showOwn", showOwn);
        ConfigManager.write(file, Json.pretty(m));
    }
}
