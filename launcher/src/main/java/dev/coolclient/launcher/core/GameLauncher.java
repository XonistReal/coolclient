package dev.coolclient.launcher.core;

import dev.coolclient.core.json.Json;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Installs a version (vanilla + loader + Java + Cool Client) and starts it. */
public final class GameLauncher {
    private static final Pattern VAR = Pattern.compile("\\$\\{([a-zA-Z_]+)}");

    private GameLauncher() {}

    public static final class Options {
        public String version;
        public Accounts.Account account;
        public LauncherSettings settings;
        /** Server to join right away ("host[:port]"), or null. */
        public String server;
        /** Passes the client's automated screenshot tour flag (development). */
        public boolean selfTest;
    }

    /** Installs everything and starts the game; returns the running process. */
    public static Process launch(Options o, Progress progress, Consumer<String> log) throws IOException {
        String version = o.version;
        File gameDir = LauncherPaths.instance(version);
        new File(gameDir, "mods").mkdirs();

        Mojang.Install vanilla = Mojang.install(version, progress);
        Loaders.Install loader = Loaders.install(version, progress);
        File java = javaFor(vanilla, o.settings, progress);
        progress.update("Preparing Cool Client", 1f);
        File client = ModJars.ensure(version);

        // Loader libraries win over vanilla ones with the same coordinates (e.g. ASM).
        Set<String> seen = new HashSet<String>();
        List<String> classpath = new ArrayList<String>();
        for (File f : loader.classpath) {
            seen.add(artifactKey(f));
            classpath.add(f.getAbsolutePath());
        }
        for (File f : vanilla.classpath) {
            if (seen.add(artifactKey(f))) classpath.add(f.getAbsolutePath());
        }
        classpath.add(vanilla.clientJar.getAbsolutePath());
        String cp = String.join(File.pathSeparator, classpath);

        Accounts.Account acc = o.account;
        File assetsRoot = vanilla.virtualAssets ? new File(LauncherPaths.ASSETS, "virtual/" + vanilla.assetIndex) : LauncherPaths.ASSETS;
        Map<String, String> vars = new HashMap<String, String>();
        vars.put("auth_player_name", acc.name);
        vars.put("version_name", version);
        vars.put("game_directory", gameDir.getAbsolutePath());
        vars.put("assets_root", assetsRoot.getAbsolutePath());
        vars.put("game_assets", new File(LauncherPaths.ASSETS, "virtual/" + vanilla.assetIndex).getAbsolutePath());
        vars.put("assets_index_name", vanilla.assetIndex);
        vars.put("auth_uuid", acc.uuid.replace("-", ""));
        vars.put("auth_access_token", acc.accessToken);
        vars.put("auth_session", acc.accessToken);
        vars.put("user_type", acc.microsoft() ? "msa" : "legacy");
        vars.put("version_type", "Cool Client");
        vars.put("user_properties", "{}");
        vars.put("natives_directory", vanilla.nativesDir.getAbsolutePath());
        vars.put("launcher_name", "coolclient-launcher");
        vars.put("launcher_version", LauncherInfo.VERSION);
        vars.put("classpath", cp);
        vars.put("classpath_separator", File.pathSeparator);
        vars.put("library_directory", LauncherPaths.LIBRARIES.getAbsolutePath());
        vars.put("clientid", "");
        vars.put("auth_xuid", "");
        vars.put("resolution_width", String.valueOf(o.settings.width));
        vars.put("resolution_height", String.valueOf(o.settings.height));
        vars.put("quickPlayMultiplayer", o.server == null ? "" : o.server);
        Map<String, Boolean> features = new HashMap<String, Boolean>();
        features.put("has_custom_resolution", true);
        features.put("is_quick_play_multiplayer", o.server != null);

        int mem = o.settings.memoryMb;
        List<String> args = new ArrayList<String>();
        args.add(java.getAbsolutePath());
        args.add("-Xmx" + mem + "M");
        args.add("-Xms" + Math.min(mem, 1024) + "M");
        args.add("-XX:+UnlockExperimentalVMOptions");
        args.add("-XX:+UseG1GC");
        args.add("-XX:G1NewSizePercent=20");
        args.add("-XX:G1ReservePercent=20");
        args.add("-XX:MaxGCPauseMillis=50");
        args.add("-XX:G1HeapRegionSize=32M");
        args.add("-Dfabric.addMods=" + client.getAbsolutePath());
        args.add("-Dcoolclient.dataDir=" + LauncherPaths.CLIENT_DATA.getAbsolutePath());
        if (o.selfTest) args.add("-Dcoolclient.selftest=true");
        if (vanilla.loggingArg != null) args.add(vanilla.loggingArg);
        Map<String, Object> arguments = Json.obj(vanilla.json, "arguments");
        if (arguments != null && Json.arr(arguments, "jvm") != null) {
            args.addAll(expand(Json.arr(arguments, "jvm"), vars, features));
        } else {
            args.add("-Djava.library.path=" + vanilla.nativesDir.getAbsolutePath());
            args.add("-cp");
            args.add(cp);
        }
        args.addAll(expand(loader.jvmArgs, vars, features));
        for (String extra : o.settings.jvmArgs.trim().split("\\s+")) if (!extra.isEmpty()) args.add(extra);

        args.add(loader.mainClass);
        if (arguments != null && Json.arr(arguments, "game") != null) {
            List<String> game = expand(Json.arr(arguments, "game"), vars, features);
            if (!game.contains("--width")) {
                game.add("--width");
                game.add(vars.get("resolution_width"));
                game.add("--height");
                game.add(vars.get("resolution_height"));
            }
            args.addAll(game);
        } else {
            for (String a : Json.str(vanilla.json, "minecraftArguments", "").split(" ")) args.add(substitute(a, vars));
            args.add("--width");
            args.add(vars.get("resolution_width"));
            args.add("--height");
            args.add(vars.get("resolution_height"));
            if (o.server != null) {
                String[] hp = o.server.split(":");
                args.add("--server");
                args.add(hp[0]);
                args.add("--port");
                args.add(hp.length > 1 ? hp[1] : "25565");
            }
        }
        args.addAll(expand(loader.gameArgs, vars, features));
        if (o.settings.fullscreen) args.add("--fullscreen");

        StringBuilder shown = new StringBuilder("[Launcher] Starting:");
        for (String a : args) shown.append(' ').append(a.equals(acc.accessToken) && a.length() > 4 ? "********" : a);
        log.accept(shown.toString());
        progress.update("Starting Minecraft", 1f);

        ProcessBuilder pb = new ProcessBuilder(args).directory(gameDir).redirectErrorStream(true);
        Process p = pb.start();
        Thread reader = new Thread(() -> {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder event = null;
                for (String line; (line = in.readLine()) != null; ) {
                    // Vanilla's log config prints XML events (for the official launcher); show them as text.
                    if (event == null && line.trim().startsWith("<log4j:Event")) event = new StringBuilder();
                    if (event == null) {
                        log.accept(line);
                        continue;
                    }
                    event.append(line).append('\n');
                    if (line.contains("</log4j:Event>")) {
                        for (String l : formatEvent(event.toString()).split("\n")) log.accept(l);
                        event = null;
                    }
                }
            } catch (IOException ignored) {
                // process ended
            }
        }, "game-output");
        reader.setDaemon(true);
        reader.start();
        return p;
    }

    private static final Pattern EVENT_ATTR = Pattern.compile("(logger|timestamp|level|thread)=\"([^\"]*)\"");
    private static final Pattern CDATA = Pattern.compile("<log4j:(Message|Throwable)><!\\[CDATA\\[(.*?)]]></log4j:\\1>", Pattern.DOTALL);

    /** "[12:34:56] [Render thread/INFO] (Logger) message" from a log4j XMLLayout event. */
    static String formatEvent(String xml) {
        Map<String, String> a = new HashMap<String, String>();
        Matcher m = EVENT_ATTR.matcher(xml);
        while (m.find()) a.put(m.group(1), m.group(2));
        StringBuilder sb = new StringBuilder();
        try {
            long ts = Long.parseLong(a.getOrDefault("timestamp", "0"));
            sb.append(new java.text.SimpleDateFormat("[HH:mm:ss] ").format(new java.util.Date(ts)));
        } catch (NumberFormatException ignored) {
            // no timestamp
        }
        sb.append('[').append(a.getOrDefault("thread", "?")).append('/').append(a.getOrDefault("level", "INFO")).append("] ");
        String logger = a.getOrDefault("logger", "");
        if (!logger.isEmpty()) sb.append('(').append(logger.substring(logger.lastIndexOf('.') + 1)).append(") ");
        Matcher c = CDATA.matcher(xml);
        boolean first = true;
        while (c.find()) {
            if (!first) sb.append('\n');
            sb.append(c.group(2).trim());
            first = false;
        }
        return sb.toString();
    }

    private static File javaFor(Mojang.Install vanilla, LauncherSettings s, Progress progress) throws IOException {
        if (!s.javaPath.isEmpty() && new File(s.javaPath).isFile()) return new File(s.javaPath);
        Map<String, Object> jv = Json.obj(vanilla.json, "javaVersion");
        return JavaRuntimes.ensure(Json.str(jv, "component", "jre-legacy"), progress);
    }

    /** group:artifact[:classifier] for a library file, so duplicates can be dropped. */
    static String artifactKey(File f) {
        String rel = LauncherPaths.LIBRARIES.toPath().relativize(f.toPath()).toString().replace('\\', '/');
        String[] parts = rel.split("/");
        if (parts.length < 4) return rel;
        String version = parts[parts.length - 2], artifact = parts[parts.length - 3];
        String group = String.join(".", java.util.Arrays.copyOf(parts, parts.length - 3));
        String base = parts[parts.length - 1].replaceAll("\\.jar$", "");
        String prefix = artifact + "-" + version + "-";
        String classifier = base.startsWith(prefix) ? base.substring(prefix.length()) : "";
        return group + ":" + artifact + (classifier.isEmpty() ? "" : ":" + classifier);
    }

    static String substitute(String s, Map<String, String> vars) {
        Matcher m = VAR.matcher(s);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String v = vars.get(m.group(1));
            m.appendReplacement(sb, Matcher.quoteReplacement(v != null ? v : m.group()));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /** Expands Mojang-style argument lists (plain strings or {rules, value} objects). */
    @SuppressWarnings("unchecked")
    static List<String> expand(List<Object> list, Map<String, String> vars, Map<String, Boolean> features) {
        List<String> out = new ArrayList<String>();
        if (list == null) return out;
        for (Object a : list) {
            if (a instanceof String) {
                out.add(substitute((String) a, vars));
            } else if (a instanceof Map) {
                Map<String, Object> m = (Map<String, Object>) a;
                if (!Mojang.allowed(Json.arr(m, "rules"), features)) continue;
                Object v = m.get("value");
                if (v instanceof String) out.add(substitute((String) v, vars));
                else if (v instanceof List) for (Object x : (List<Object>) v) out.add(substitute(String.valueOf(x), vars));
            }
        }
        return out;
    }
}
