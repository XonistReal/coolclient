package dev.coolclient.core.net;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.config.ConfigManager;
import dev.coolclient.core.json.Json;
import dev.coolclient.core.util.Log;
import dev.coolclient.core.util.Scheduler;
import dev.coolclient.core.util.Time;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Talks to the Cool Client API (cosmetics, presence). Identity is proven with the Mojang session
 * "join" handshake: the API hands out a random server id, the client joins it through Mojang,
 * and the API checks Mojang's hasJoined endpoint before issuing a session token.
 */
public final class ApiClient {
    private final ConfigManager config;
    private volatile String token;
    private volatile UUID tokenUuid;
    private volatile boolean authInFlight;
    private long nextAttempt;
    private long backoff = 15_000;
    private volatile boolean reachable;

    public ApiClient(ConfigManager config) {
        this.config = config;
        this.token = Json.str(config.clientData(), "apiToken", null);
        String u = Json.str(config.clientData(), "apiUuid", null);
        try {
            this.tokenUuid = u == null ? null : UUID.fromString(u);
        } catch (IllegalArgumentException e) {
            this.tokenUuid = null;
        }
    }

    public boolean enabled() {
        return CoolClient.get().client().onlineFeatures.on();
    }

    public boolean reachable() { return reachable; }

    public boolean authenticated() {
        UUID self = CoolClient.get().platform().game().selfUuid();
        return token != null && self != null && self.equals(tokenUuid);
    }

    public String baseUrl() {
        String u = CoolClient.get().client().apiUrl.get().trim();
        while (u.endsWith("/")) u = u.substring(0, u.length() - 1);
        return u;
    }

    /** Called every tick: (re)authenticates in the background when needed. */
    public void tick() {
        if (!enabled() || authInFlight || authenticated()) return;
        if (Time.millis() < nextAttempt) return;
        if (CoolClient.get().platform().game().selfUuid() == null) return;
        authInFlight = true;
        nextAttempt = Time.millis() + backoff;
        Scheduler.async(this::authenticate);
    }

    private void authenticate() {
        try {
            String name = CoolClient.get().platform().game().selfName();
            UUID uuid = CoolClient.get().platform().game().selfUuid();
            Map<String, Object> body = new LinkedHashMap<String, Object>();
            body.put("name", name);
            body.put("uuid", uuid.toString());
            Map<String, Object> challenge = request("POST", "/api/auth/challenge", body, false);
            String serverId = Json.str(challenge, "serverId", null);
            if (serverId == null) throw new IOException("No challenge from API");
            boolean joined = CoolClient.get().platform().joinSession(serverId);
            body.put("serverId", serverId);
            body.put("offline", !joined);
            Map<String, Object> res = request("POST", "/api/auth/verify", body, false);
            String t = Json.str(res, "token", null);
            if (t == null) throw new IOException("API refused login: " + Json.str(res, "error", "unknown"));
            token = t;
            tokenUuid = uuid;
            reachable = true;
            backoff = 15_000;
            config.clientData().put("apiToken", t);
            config.clientData().put("apiUuid", uuid.toString());
            config.markDirty();
            Log.info("Signed in to Cool Client API as " + name);
            CoolClient.get().cosmetics().pushLoadout();
            CoolClient.get().cosmetics().entitlements().refresh(true);
        } catch (Exception e) {
            reachable = false;
            backoff = Math.min(backoff * 2, 5 * 60_000);
            Log.warn("Cool Client API sign-in failed (" + e.getMessage() + "); retrying in " + backoff / 1000 + "s");
        } finally {
            authInFlight = false;
        }
    }

    public void invalidate() {
        token = null;
        tokenUuid = null;
        config.clientData().remove("apiToken");
        config.markDirty();
    }

    /** Runs a request on a worker thread and delivers the result on the game thread. */
    public void async(final String method, final String path, final Object body, final boolean auth,
                      final Consumer<Map<String, Object>> onResult) {
        if (!enabled()) return;
        Scheduler.async(() -> {
            try {
                final Map<String, Object> res = request(method, path, body, auth);
                reachable = true;
                if (onResult != null) Scheduler.main(() -> onResult.accept(res));
            } catch (IOException e) {
                reachable = false;
                Log.warn("API " + method + " " + path + " failed: " + e.getMessage());
            }
        });
    }

    /** Blocking JSON request. Never call on the game thread. */
    public Map<String, Object> request(String method, String path, Object body, boolean auth) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(baseUrl() + path).openConnection();
        c.setConnectTimeout(4000);
        c.setReadTimeout(6000);
        c.setRequestMethod(method);
        c.setRequestProperty("User-Agent", "CoolClient/" + CoolClient.VERSION + " (MC " + CoolClient.get().platform().minecraftVersion() + ")");
        c.setRequestProperty("Accept", "application/json");
        if (auth && token != null) c.setRequestProperty("Authorization", "Bearer " + token);
        if (body != null) {
            byte[] data = Json.stringify(body).getBytes(StandardCharsets.UTF_8);
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json");
            c.setFixedLengthStreamingMode(data.length);
            OutputStream os = c.getOutputStream();
            try {
                os.write(data);
            } finally {
                os.close();
            }
        }
        int code = c.getResponseCode();
        InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
        String text = in == null ? "" : readAll(in);
        if (code == 401 && auth) invalidate();
        if (code >= 400 && text.isEmpty()) throw new IOException("HTTP " + code);
        Map<String, Object> result = text.isEmpty() ? new LinkedHashMap<String, Object>() : Json.parseObject(text);
        if (code >= 400) result.put("error", Json.str(result, "error", "HTTP " + code));
        return result;
    }

    private static String readAll(InputStream in) throws IOException {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            in.close();
        }
    }
}
