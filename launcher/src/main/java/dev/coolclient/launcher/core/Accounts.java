package dev.coolclient.launcher.core;

import dev.coolclient.core.json.Json;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * Minecraft accounts. Microsoft sign-in uses the OAuth device-code flow (Microsoft, Xbox Live,
 * XSTS, Minecraft services). It needs an Azure app ID that Mojang has approved for the Minecraft
 * API, set in Settings. Offline accounts work in singleplayer, LAN and offline-mode servers.
 */
public final class Accounts {
    private static final String MS = "https://login.microsoftonline.com/consumers/oauth2/v2.0";
    private static final String SCOPE = "XboxLive.signin offline_access";

    public static final class Account {
        public String id, type, name, uuid, accessToken, refreshToken;
        public long expiresAt;

        public boolean microsoft() { return "msa".equals(type); }

        Map<String, Object> toJson() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("id", id);
            m.put("type", type);
            m.put("name", name);
            m.put("uuid", uuid);
            m.put("accessToken", accessToken);
            if (refreshToken != null) m.put("refreshToken", refreshToken);
            if (expiresAt > 0) m.put("expiresAt", (double) expiresAt);
            return m;
        }

        static Account fromJson(Map<String, Object> m) {
            Account a = new Account();
            a.id = Json.str(m, "id", null);
            a.type = Json.str(m, "type", "offline");
            a.name = Json.str(m, "name", "Player");
            a.uuid = Json.str(m, "uuid", null);
            a.accessToken = Json.str(m, "accessToken", "0");
            a.refreshToken = Json.str(m, "refreshToken", null);
            a.expiresAt = (long) Json.num(m, "expiresAt", 0);
            return a;
        }
    }

    /** The code the player types at microsoft.com/link. */
    public static final class DeviceCode {
        public String userCode, deviceCode, verificationUri;
        public int interval, expiresIn;
    }

    private final List<Account> accounts = new ArrayList<Account>();
    private String selected;

    public static Accounts load() {
        Accounts a = new Accounts();
        try {
            if (LauncherPaths.ACCOUNTS.isFile()) {
                Map<String, Object> m = Json.parseObject(new String(Files.readAllBytes(LauncherPaths.ACCOUNTS.toPath()), StandardCharsets.UTF_8));
                a.selected = Json.str(m, "selected", null);
                List<Object> list = Json.arr(m, "accounts");
                if (list != null) {
                    for (Object o : list) {
                        @SuppressWarnings("unchecked")
                        Account acc = Account.fromJson((Map<String, Object>) o);
                        if (acc.id != null && acc.uuid != null) a.accounts.add(acc);
                    }
                }
            }
        } catch (Exception e) {
            dev.coolclient.core.util.Log.error("Failed to read accounts", e);
        }
        return a;
    }

    public synchronized void save() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("selected", selected);
        List<Object> list = new ArrayList<Object>();
        for (Account a : accounts) list.add(a.toJson());
        m.put("accounts", list);
        LauncherSettings.writeAtomic(LauncherPaths.ACCOUNTS, Json.pretty(m));
    }

    public synchronized List<Account> all() { return new ArrayList<Account>(accounts); }

    public synchronized Account selected() {
        for (Account a : accounts) if (a.id.equals(selected)) return a;
        return accounts.isEmpty() ? null : accounts.get(0);
    }

    public synchronized void select(Account a) {
        selected = a.id;
        save();
    }

    public synchronized void remove(Account a) {
        accounts.removeIf(x -> x.id.equals(a.id));
        if (a.id.equals(selected)) selected = accounts.isEmpty() ? null : accounts.get(0).id;
        save();
    }

    public synchronized Account put(Account a) {
        accounts.removeIf(x -> x.id.equals(a.id));
        accounts.add(a);
        selected = a.id;
        save();
        return a;
    }

    // ------------------------------------------------------------------ offline

    public static boolean validName(String name) {
        return name != null && name.matches("[A-Za-z0-9_]{3,16}");
    }

    public static Account offline(String name) {
        if (!validName(name)) throw new IllegalArgumentException("Names must be 3-16 letters, numbers or underscores.");
        Account a = new Account();
        a.type = "offline";
        a.name = name;
        a.uuid = offlineUuid(name);
        a.id = "offline-" + a.uuid;
        a.accessToken = "0";
        return a;
    }

    /** Vanilla's offline UUID: name-based v3 of "OfflinePlayer:<name>". */
    public static String offlineUuid(String name) {
        try {
            byte[] h = MessageDigest.getInstance("MD5").digest(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
            h[6] = (byte) ((h[6] & 0x0f) | 0x30);
            h[8] = (byte) ((h[8] & 0x3f) | 0x80);
            return dashed(Http.hex(h));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String dashed(String hex) {
        return hex.substring(0, 8) + "-" + hex.substring(8, 12) + "-" + hex.substring(12, 16) + "-" + hex.substring(16, 20) + "-" + hex.substring(20);
    }

    // ------------------------------------------------------------------ Microsoft

    private static String form(String... kv) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            if (sb.length() > 0) sb.append('&');
            sb.append(URLEncoder.encode(kv[i], StandardCharsets.UTF_8)).append('=').append(URLEncoder.encode(kv[i + 1], StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> body(Object[] res) { return (Map<String, Object>) res[1]; }

    private static int status(Object[] res) { return (Integer) res[0]; }

    public static DeviceCode startDeviceCode(String clientId) throws IOException {
        if (clientId == null || clientId.trim().isEmpty()) {
            throw new IOException("Microsoft sign-in needs an Azure app ID. Add one in Settings, or use an offline account.");
        }
        Object[] r = Http.post(MS + "/devicecode", "application/x-www-form-urlencoded", form("client_id", clientId.trim(), "scope", SCOPE));
        if (status(r) != 200) throw new IOException(Json.str(body(r), "error_description", "Microsoft refused the request."));
        DeviceCode c = new DeviceCode();
        c.userCode = Json.str(body(r), "user_code", "");
        c.deviceCode = Json.str(body(r), "device_code", "");
        c.verificationUri = Json.str(body(r), "verification_uri", "https://www.microsoft.com/link");
        c.interval = (int) Json.num(body(r), "interval", 5);
        c.expiresIn = (int) Json.num(body(r), "expires_in", 900);
        return c;
    }

    /** Polls until the player approves the code, then signs in to Minecraft. */
    public static Account finishDeviceCode(String clientId, DeviceCode code, BooleanSupplier cancelled) throws IOException {
        long deadline = System.currentTimeMillis() + code.expiresIn * 1000L;
        long interval = Math.max(1, code.interval) * 1000L;
        while (System.currentTimeMillis() < deadline) {
            if (cancelled.getAsBoolean()) throw new IOException("Sign-in cancelled.");
            try {
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Sign-in cancelled.");
            }
            Object[] r = Http.post(MS + "/token", "application/x-www-form-urlencoded",
                    form("grant_type", "urn:ietf:params:oauth:grant-type:device_code", "client_id", clientId.trim(), "device_code", code.deviceCode));
            if (status(r) == 200) return minecraft(body(r));
            String err = Json.str(body(r), "error", "");
            if (err.equals("authorization_pending")) continue;
            if (err.equals("slow_down")) {
                interval += 5000;
                continue;
            }
            throw new IOException(Json.str(body(r), "error_description", "Sign-in failed."));
        }
        throw new IOException("The sign-in code expired. Try again.");
    }

    /** Returns a usable account, refreshing a Microsoft token that is about to expire. */
    public Account fresh(Account a, String clientId) throws IOException {
        if (!a.microsoft() || a.expiresAt - System.currentTimeMillis() > 5 * 60_000L) return a;
        if (a.refreshToken == null || clientId == null || clientId.isEmpty()) throw new IOException("Your Microsoft session expired. Sign in again.");
        Object[] r = Http.post(MS + "/token", "application/x-www-form-urlencoded",
                form("grant_type", "refresh_token", "client_id", clientId.trim(), "refresh_token", a.refreshToken, "scope", SCOPE));
        if (status(r) != 200) throw new IOException("Your Microsoft session expired. Sign in again.");
        return put(minecraft(body(r)));
    }

    private static String xstsError(double code) {
        long c = (long) code;
        if (c == 2148916233L) return "This Microsoft account has no Xbox profile. Sign in at minecraft.net once first.";
        if (c == 2148916235L) return "Xbox Live is not available in your country.";
        if (c == 2148916236L || c == 2148916237L) return "This account needs adult verification.";
        if (c == 2148916238L) return "This is a child account and must be added to a Family by an adult.";
        return "Xbox sign-in failed.";
    }

    @SuppressWarnings("unchecked")
    private static Account minecraft(Map<String, Object> ms) throws IOException {
        String msToken = Json.str(ms, "access_token", "");
        Object[] xbl = Http.post("https://user.auth.xboxlive.com/user/authenticate", "application/json", Json.stringify(map(
                "Properties", map("AuthMethod", "RPS", "SiteName", "user.auth.xboxlive.com", "RpsTicket", "d=" + msToken),
                "RelyingParty", "http://auth.xboxlive.com", "TokenType", "JWT")));
        if (status(xbl) != 200) throw new IOException("Xbox Live sign-in failed.");
        String xblToken = Json.str(body(xbl), "Token", "");
        Map<String, Object> xui = (Map<String, Object>) Json.arr(Json.obj(body(xbl), "DisplayClaims"), "xui").get(0);
        String uhs = Json.str(xui, "uhs", "");

        List<Object> userTokens = new ArrayList<Object>();
        userTokens.add(xblToken);
        Object[] xsts = Http.post("https://xsts.auth.xboxlive.com/xsts/authorize", "application/json", Json.stringify(map(
                "Properties", map("SandboxId", "RETAIL", "UserTokens", userTokens),
                "RelyingParty", "rp://api.minecraftservices.com/", "TokenType", "JWT")));
        if (status(xsts) != 200) throw new IOException(xstsError(Json.num(body(xsts), "XErr", 0)));

        Object[] mc = Http.post("https://api.minecraftservices.com/authentication/login_with_xbox", "application/json",
                Json.stringify(map("identityToken", "XBL3.0 x=" + uhs + ";" + Json.str(body(xsts), "Token", ""))));
        if (status(mc) != 200) {
            throw new IOException(status(mc) == 403
                    ? "Minecraft refused this app ID. It must be approved by Mojang for the Minecraft API."
                    : "Minecraft sign-in failed (HTTP " + status(mc) + ").");
        }
        String mcToken = Json.str(body(mc), "access_token", "");
        java.net.http.HttpResponse<String> prof = Http.send(java.net.http.HttpRequest.newBuilder(
                java.net.URI.create("https://api.minecraftservices.com/minecraft/profile")).header("Authorization", "Bearer " + mcToken).GET());
        if (prof.statusCode() == 404) throw new IOException("This Microsoft account does not own Minecraft: Java Edition.");
        if (prof.statusCode() != 200) throw new IOException("Could not read your Minecraft profile (HTTP " + prof.statusCode() + ").");
        Map<String, Object> profile = Json.parseObject(prof.body());
        Account a = new Account();
        a.type = "msa";
        a.uuid = dashed(Json.str(profile, "id", ""));
        a.id = "msa-" + a.uuid;
        a.name = Json.str(profile, "name", "Player");
        a.accessToken = mcToken;
        a.refreshToken = Json.str(ms, "refresh_token", null);
        a.expiresAt = System.currentTimeMillis() + (long) Json.num(body(mc), "expires_in", 86400) * 1000L;
        return a;
    }

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        for (int i = 0; i + 1 < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }
}
