package dev.coolclient.launcher.core;

import dev.coolclient.core.json.Json;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Mojang's official Java runtimes ("jre-legacy" = Java 8, "java-runtime-delta" = Java 21). */
public final class JavaRuntimes {
    private static final String INDEX = "https://launchermeta.mojang.com/v1/products/java-runtime/2ec0cc96c44e5a76b9c8b7c39df7210883d12871/all.json";

    private JavaRuntimes() {}

    private static String platformKey() {
        String os = Mojang.osName(), arch = System.getProperty("os.arch");
        boolean arm = arch.contains("aarch64") || arch.contains("arm");
        if (os.equals("windows")) return arm ? "windows-arm64" : arch.contains("64") ? "windows-x64" : "windows-x86";
        if (os.equals("osx")) return arm ? "mac-os-arm64" : "mac-os";
        return arch.contains("64") ? "linux" : "linux-i386";
    }

    private static File executable(File root) {
        String os = Mojang.osName();
        if (os.equals("windows")) return new File(root, "bin/javaw.exe");
        if (os.equals("osx")) return new File(root, "jre.bundle/Contents/Home/bin/java");
        return new File(root, "bin/java");
    }

    @SuppressWarnings("unchecked")
    public static File ensure(String component, Progress progress) throws IOException {
        File root = new File(LauncherPaths.RUNTIMES, component);
        File exe = executable(root);
        File marker = new File(root, ".coolclient-complete");
        if (exe.isFile() && marker.isFile()) return exe;

        Map<String, Object> all = Http.getJson(INDEX);
        List<Object> list = Json.arr(Json.obj(all, platformKey()), component);
        if (list == null || list.isEmpty()) throw new IOException("Mojang has no Java runtime \"" + component + "\" for " + platformKey());
        Map<String, Object> manifest = Http.getJson(Json.str(Json.obj((Map<String, Object>) list.get(0), "manifest"), "url", null));
        List<Http.Task> tasks = new ArrayList<Http.Task>();
        List<File> executables = new ArrayList<File>();
        for (Map.Entry<String, Object> e : Json.obj(manifest, "files").entrySet()) {
            Map<String, Object> entry = (Map<String, Object>) e.getValue();
            File dest = new File(root, e.getKey());
            String type = Json.str(entry, "type", "");
            if (type.equals("directory")) {
                dest.mkdirs();
            } else if (type.equals("file")) {
                Map<String, Object> raw = Json.obj(Json.obj(entry, "downloads"), "raw");
                String url = Json.str(raw, "url", null), sha1 = Json.str(raw, "sha1", null);
                long size = (long) Json.num(raw, "size", 0);
                tasks.add(() -> Http.download(url, dest, sha1, size));
                if (Json.bool(entry, "executable", false)) executables.add(dest);
            } else if (type.equals("link") && !Mojang.osName().equals("windows")) {
                final String target = Json.str(entry, "target", "");
                tasks.add(() -> {
                    dest.getParentFile().mkdirs();
                    if (!dest.exists()) Files.createSymbolicLink(dest.toPath(), new File(target).toPath());
                });
            }
        }
        Http.pool(tasks, 8, progress.stage("Downloading Java"));
        for (File f : executables) f.setExecutable(true);
        Files.write(marker.toPath(), new byte[0]);
        return exe;
    }
}
