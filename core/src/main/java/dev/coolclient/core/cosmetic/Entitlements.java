package dev.coolclient.core.cosmetic;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.json.Json;
import dev.coolclient.core.net.ApiClient;
import dev.coolclient.core.util.Time;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * What the signed-in account owns, as reported by the API server. Ownership only matters when
 * the server says it enforces it (it then hides unowned cosmetics from other players); without
 * a server, or while it doesn't enforce, everything counts as owned.
 */
public final class Entitlements {
    public static final String SUPPORTER = "supporter";
    public static final String SUPPORTER_PLUS = "supporter_plus";
    /** Creators' rank: everything Supporter+ covers, plus the Media cosmetics. Never sold. */
    public static final String MEDIA = "media";
    /** The project owner's rank: everything, plus the Owner cosmetics. */
    public static final String OWNER = "owner";
    private static final long REFRESH_MS = 10 * 60_000L;

    private final ApiClient api;
    private volatile Set<String> owned = Collections.emptySet();
    private volatile boolean enforced;
    private volatile String storeUrl = "";
    private long lastRefresh = -REFRESH_MS;
    private Runnable onLoaded;

    public Entitlements(ApiClient api) {
        this.api = api;
    }

    /** True when the server hides unowned cosmetics from other players. */
    public boolean enforced() { return enforced && api.enabled(); }

    public boolean owns(Cosmetic c) {
        if (owned.contains(OWNER)) return true;
        // Seasonal cosmetics are yours once you've worn one during its season (the server keeps a record).
        if (c.season() != null) return owned.contains(c.id);
        // Exclusive, Media and Owner cosmetics are hidden from others even while everything else is free.
        if (c.unlock == Unlock.EXCLUSIVE || c.unlock == Unlock.OWNER) return owned.contains(c.id);
        if (c.unlock == Unlock.MEDIA) return owned.contains(c.id) || owned.contains(MEDIA);
        if (!enforced() || c.unlock == Unlock.FREE || owned.contains(c.id)) return true;
        if (owned.contains(SUPPORTER_PLUS) || owned.contains(MEDIA)) return true;
        return c.unlock == Unlock.SUPPORTER && owned.contains(SUPPORTER);
    }

    public boolean supporter() {
        return owned.contains(SUPPORTER) || owned.contains(SUPPORTER_PLUS);
    }

    public boolean media() {
        return owned.contains(MEDIA);
    }

    public boolean owner() {
        return owned.contains(OWNER);
    }

    /**
     * Whether the cosmetics menu lists it: everything that can be had (free, Supporter, store), plus
     * given cosmetics (Founder, Media, Owner) only for players who have them. Others only ever see
     * those on someone wearing them. Seasonal cosmetics are listed for everyone during their season,
     * and afterwards only for players who kept one.
     */
    public boolean listed(Cosmetic c) {
        return listed(c, System.currentTimeMillis());
    }

    public boolean listed(Cosmetic c, long now) {
        if (c.season() != null) return c.season().active(now) || owns(c);
        return c.unlock.sold() || owns(c);
    }

    /** Runs on the main thread each time the server's ownership list arrives. */
    void onLoaded(Runnable r) {
        this.onLoaded = r;
    }

    /** Store page for this account, or empty when the server has none. */
    public String storeUrl() {
        String base = storeUrl;
        if (base.isEmpty()) return "";
        String name = CoolClient.get().platform().game().selfName();
        return name == null ? base : base + (base.contains("?") ? "&" : "?") + "username=" + name;
    }

    /** Re-reads store info and ownership (at most every few minutes unless forced). */
    public void refresh(boolean force) {
        if (!api.enabled()) return;
        long now = Time.millis();
        if (!force && now - lastRefresh < REFRESH_MS) return;
        lastRefresh = now;
        api.async("GET", "/api/store", null, false, res -> {
            enforced = Boolean.TRUE.equals(res.get("enforced"));
            storeUrl = Json.str(res, "url", "");
        });
        if (!api.authenticated()) return;
        api.async("GET", "/api/me", null, true, res -> {
            Object list = res.get("entitlements");
            Set<String> s = new HashSet<String>();
            if (list instanceof List) for (Object o : (List<?>) list) if (o instanceof String) s.add((String) o);
            owned = s;
            if (onLoaded != null) onLoaded.run();
        });
    }
}
