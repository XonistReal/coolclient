package dev.coolclient.launcher.core;

import dev.coolclient.core.json.Json;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Fabric (1.21.4) and Legacy Fabric (1.8.9) loader profiles. */
public final class Loaders {
    /** Loader build both metas serve; the client jars require 0.16 or newer. */
    public static final String LOADER_VERSION = "0.18.3";

    private Loaders() {}

    public static final class Install {
        public String mainClass;
        public final List<File> classpath = new ArrayList<File>();
        public List<Object> jvmArgs = Collections.emptyList();
        public List<Object> gameArgs = Collections.emptyList();
    }

    private static String meta(String version) {
        return version.equals("1.8.9")
                ? "https://meta.legacyfabric.net/v2/versions/loader/1.8.9"
                : "https://meta.fabricmc.net/v2/versions/loader/" + version;
    }

    /** The launch profile, cached so the game still starts offline. */
    private static Map<String, Object> profile(String version) throws IOException {
        File cache = new File(LauncherPaths.VERSIONS, "loader-" + version + "/profile-" + LOADER_VERSION + ".json");
        try {
            Map<String, Object> p = Http.getJson(meta(version) + "/" + LOADER_VERSION + "/profile/json");
            cache.getParentFile().mkdirs();
            Files.write(cache.toPath(), Json.pretty(p).getBytes(StandardCharsets.UTF_8));
            return p;
        } catch (IOException e) {
            if (cache.isFile()) return Json.parseObject(new String(Files.readAllBytes(cache.toPath()), StandardCharsets.UTF_8));
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    public static Install install(String version, Progress progress) throws IOException {
        Map<String, Object> p = profile(version);
        Install out = new Install();
        out.mainClass = Json.str(p, "mainClass", "net.fabricmc.loader.impl.launch.knot.KnotClient");
        List<Http.Task> tasks = new ArrayList<Http.Task>();
        List<File> natives = new ArrayList<File>();
        for (Object o : Json.arr(p, "libraries")) {
            Map<String, Object> lib = (Map<String, Object>) o;
            String name = Json.str(lib, "name", "");
            String repo = Json.str(lib, "url", "https://maven.fabricmc.net/");
            if (!repo.endsWith("/")) repo += "/";
            Map<String, Object> nat = Json.obj(lib, "natives");
            if (nat != null) {
                // Legacy Fabric's patched LWJGL 2 ships natives as a classifier jar to unpack.
                String classifier = Json.str(nat, Mojang.osName(), null);
                if (classifier == null) continue;
                classifier = classifier.replace("${arch}", System.getProperty("os.arch").contains("64") ? "64" : "32");
                String rel = Mojang.mavenPath(name + ":" + classifier);
                File dest = new File(LauncherPaths.LIBRARIES, rel);
                String url = repo + rel;
                natives.add(dest);
                tasks.add(() -> Http.download(url, dest, null, 0));
                continue;
            }
            String rel = Mojang.mavenPath(name);
            File dest = new File(LauncherPaths.LIBRARIES, rel);
            String url = repo + rel;
            String sha1 = Json.str(lib, "sha1", null);
            long size = (long) Json.num(lib, "size", 0);
            out.classpath.add(dest);
            tasks.add(() -> Http.download(url, dest, sha1, size));
        }
        Http.pool(tasks, 6, progress.stage("Downloading mod loader"));
        // Unpacked over vanilla's natives, which the launch step installed just before.
        File nativesDir = LauncherPaths.natives(version);
        for (File jar : natives) Mojang.extract(jar, nativesDir);
        Map<String, Object> args = Json.obj(p, "arguments");
        if (args != null) {
            if (Json.arr(args, "jvm") != null) out.jvmArgs = Json.arr(args, "jvm");
            if (Json.arr(args, "game") != null) out.gameArgs = Json.arr(args, "game");
        }
        return out;
    }
}
