package dev.coolclient.launcher.core;

import dev.coolclient.core.json.Json;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Vanilla Minecraft install: version JSON, client jar, libraries, natives and assets. */
public final class Mojang {
    private static final String MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";

    private Mojang() {}

    /** Everything the launch step needs from the vanilla install. */
    public static final class Install {
        public Map<String, Object> json;
        public final List<File> classpath = new ArrayList<File>();
        public File clientJar;
        public File nativesDir;
        public String loggingArg;
        public String assetIndex;
        public boolean virtualAssets;
    }

    public static String osName() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("win") ? "windows" : os.contains("mac") ? "osx" : "linux";
    }

    private static String arch() {
        return System.getProperty("os.arch");
    }

    /** Evaluates Mojang library/argument rules. */
    @SuppressWarnings("unchecked")
    public static boolean allowed(List<Object> rules, Map<String, Boolean> features) {
        if (rules == null || rules.isEmpty()) return true;
        boolean result = false;
        for (Object o : rules) {
            Map<String, Object> rule = (Map<String, Object>) o;
            boolean matches = true;
            Map<String, Object> os = Json.obj(rule, "os");
            if (os != null) {
                String name = Json.str(os, "name", null);
                if (name != null && !name.equals(osName())) matches = false;
                String a = Json.str(os, "arch", null);
                if (a != null && !(a.equals("x86") ? arch().matches("x86|i[3-6]86") : arch().equals(a))) matches = false;
                String ver = Json.str(os, "version", null);
                if (ver != null && !Pattern.compile(ver).matcher(System.getProperty("os.version")).find()) matches = false;
            }
            Map<String, Object> feats = Json.obj(rule, "features");
            if (feats != null) {
                for (Map.Entry<String, Object> e : feats.entrySet()) {
                    boolean have = features != null && Boolean.TRUE.equals(features.get(e.getKey()));
                    if (have != Boolean.TRUE.equals(e.getValue())) matches = false;
                }
            }
            if (matches) result = "allow".equals(Json.str(rule, "action", "allow"));
        }
        return result;
    }

    /** Maven coordinate (group:artifact:version[:classifier]) to a relative path. */
    public static String mavenPath(String name) {
        String[] p = name.split(":");
        String file = p[1] + "-" + p[2] + (p.length > 3 ? "-" + p[3] : "") + ".jar";
        return p[0].replace('.', '/') + "/" + p[1] + "/" + p[2] + "/" + file;
    }

    public static Map<String, Object> versionJson(String id) throws IOException {
        File file = new File(LauncherPaths.VERSIONS, id + "/" + id + ".json");
        if (file.isFile()) {
            try {
                return Json.parseObject(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
            } catch (RuntimeException e) {
                // corrupt: download again
            }
        }
        Map<String, Object> manifest = Http.getJson(MANIFEST);
        for (Object o : Json.arr(manifest, "versions")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> v = (Map<String, Object>) o;
            if (id.equals(Json.str(v, "id", null))) {
                Http.download(Json.str(v, "url", null), file, Json.str(v, "sha1", null), 0);
                return Json.parseObject(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
            }
        }
        throw new IOException("Unknown Minecraft version " + id);
    }

    @SuppressWarnings("unchecked")
    public static Install install(String id, Progress progress) throws IOException {
        Install out = new Install();
        out.json = versionJson(id);
        List<Http.Task> tasks = new ArrayList<Http.Task>();
        List<File> nativeJars = new ArrayList<File>();

        Map<String, Object> client = Json.obj(Json.obj(out.json, "downloads"), "client");
        out.clientJar = new File(LauncherPaths.VERSIONS, id + "/" + id + ".jar");
        tasks.add(dl(Json.str(client, "url", null), out.clientJar, client));

        for (Object o : Json.arr(out.json, "libraries")) {
            Map<String, Object> lib = (Map<String, Object>) o;
            if (!allowed(Json.arr(lib, "rules"), null)) continue;
            Map<String, Object> downloads = Json.obj(lib, "downloads");
            Map<String, Object> artifact = Json.obj(downloads, "artifact");
            if (artifact != null) {
                File dest = new File(LauncherPaths.LIBRARIES, Json.str(artifact, "path", mavenPath(Json.str(lib, "name", ""))));
                out.classpath.add(dest);
                tasks.add(dl(Json.str(artifact, "url", null), dest, artifact));
            }
            Map<String, Object> natives = Json.obj(lib, "natives");
            if (natives != null && natives.get(osName()) != null) {
                String classifier = ((String) natives.get(osName())).replace("${arch}", arch().contains("64") ? "64" : "32");
                Map<String, Object> nat = Json.obj(Json.obj(downloads, "classifiers"), classifier);
                if (nat != null) {
                    File dest = new File(LauncherPaths.LIBRARIES, Json.str(nat, "path", ""));
                    nativeJars.add(dest);
                    tasks.add(dl(Json.str(nat, "url", null), dest, nat));
                }
            }
        }

        Map<String, Object> logging = Json.obj(Json.obj(out.json, "logging"), "client");
        if (logging != null && Json.obj(logging, "file") != null) {
            Map<String, Object> f = Json.obj(logging, "file");
            File dest = new File(LauncherPaths.ASSETS, "log_configs/" + Json.str(f, "id", "client.xml"));
            tasks.add(dl(Json.str(f, "url", null), dest, f));
            out.loggingArg = Json.str(logging, "argument", "").replace("${path}", dest.getAbsolutePath());
        }

        Http.pool(tasks, 8, progress.stage("Downloading Minecraft " + id));

        // Assets
        Map<String, Object> idx = Json.obj(out.json, "assetIndex");
        out.assetIndex = Json.str(idx, "id", id);
        File idxFile = new File(LauncherPaths.ASSETS, "indexes/" + out.assetIndex + ".json");
        Http.download(Json.str(idx, "url", null), idxFile, Json.str(idx, "sha1", null), (long) Json.num(idx, "size", 0));
        Map<String, Object> index = Json.parseObject(new String(Files.readAllBytes(idxFile.toPath()), StandardCharsets.UTF_8));
        out.virtualAssets = Json.bool(index, "virtual", false) || Json.bool(index, "map_to_resources", false);
        List<Http.Task> assetTasks = new ArrayList<Http.Task>();
        for (Map.Entry<String, Object> e : Json.obj(index, "objects").entrySet()) {
            Map<String, Object> obj = (Map<String, Object>) e.getValue();
            String hash = Json.str(obj, "hash", "");
            long size = (long) Json.num(obj, "size", 0);
            File dest = new File(LauncherPaths.ASSETS, "objects/" + hash.substring(0, 2) + "/" + hash);
            final String name = e.getKey();
            final boolean virtual = out.virtualAssets;
            assetTasks.add(() -> {
                // Asset files are named by their hash, so an existing file of the right size is good.
                if (!(dest.isFile() && dest.length() == size)) Http.download("https://resources.download.minecraft.net/" + hash.substring(0, 2) + "/" + hash, dest, hash, size);
                if (virtual) {
                    File legacy = new File(LauncherPaths.ASSETS, "virtual/" + out.assetIndex + "/" + name);
                    if (!legacy.isFile() || legacy.length() != size) {
                        legacy.getParentFile().mkdirs();
                        Files.copy(dest.toPath(), legacy.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            });
        }
        Http.pool(assetTasks, 16, progress.stage("Downloading assets"));

        // Natives: LWJGL 2 (1.8.9) ships them in classifier jars; modern versions unpack their own.
        out.nativesDir = LauncherPaths.natives(id);
        if (!nativeJars.isEmpty()) {
            deleteTree(out.nativesDir);
            for (File jar : nativeJars) extract(jar, out.nativesDir);
        }
        out.nativesDir.mkdirs();
        return out;
    }

    private static Http.Task dl(String url, File dest, Map<String, Object> info) {
        String sha1 = Json.str(info, "sha1", null);
        long size = (long) Json.num(info, "size", 0);
        return () -> Http.download(url, dest, sha1, size);
    }

    static void extract(File jar, File dir) throws IOException {
        try (ZipFile zip = new ZipFile(jar)) {
            java.util.Enumeration<? extends ZipEntry> en = zip.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                if (e.isDirectory() || e.getName().startsWith("META-INF/")) continue;
                File dest = new File(dir, e.getName());
                if (!dest.getCanonicalPath().startsWith(dir.getCanonicalPath() + File.separator)) continue;
                dest.getParentFile().mkdirs();
                try (InputStream in = zip.getInputStream(e)) {
                    Files.copy(in, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    static void deleteTree(File f) {
        File[] kids = f.listFiles();
        if (kids != null) for (File k : kids) deleteTree(k);
        f.delete();
    }
}
