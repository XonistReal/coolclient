package dev.coolclient.launcher.core;

import dev.coolclient.core.json.Json;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

/** Launcher settings, stored as JSON next to the game files. */
public final class LauncherSettings {
    public static final String[] VERSIONS = {"1.8.9", "1.21.4"};

    public String version = "1.8.9";
    public int memoryMb = 3072;
    public int width = 1280;
    public int height = 720;
    public boolean fullscreen;
    public String jvmArgs = "";
    public String javaPath = "";
    /** "minimize", "keep" or "close" (hide the launcher until the game exits). */
    public String afterLaunch = "minimize";
    /** Azure app (client) ID approved for the Minecraft API; needed for Microsoft sign-in. */
    public String msClientId = "";

    public static LauncherSettings load() {
        LauncherSettings s = new LauncherSettings();
        File f = LauncherPaths.SETTINGS;
        if (!f.isFile()) return s;
        try {
            Map<String, Object> m = Json.parseObject(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
            s.version = Json.str(m, "version", s.version);
            s.memoryMb = (int) Json.num(m, "memoryMb", s.memoryMb);
            s.width = (int) Json.num(m, "width", s.width);
            s.height = (int) Json.num(m, "height", s.height);
            s.fullscreen = Json.bool(m, "fullscreen", s.fullscreen);
            s.jvmArgs = Json.str(m, "jvmArgs", s.jvmArgs);
            s.javaPath = Json.str(m, "javaPath", s.javaPath);
            s.afterLaunch = Json.str(m, "afterLaunch", s.afterLaunch);
            s.msClientId = Json.str(m, "msClientId", s.msClientId);
        } catch (Exception e) {
            // keep defaults
        }
        s.clamp();
        return s;
    }

    private void clamp() {
        boolean known = false;
        for (String v : VERSIONS) known |= v.equals(version);
        if (!known) version = VERSIONS[0];
        memoryMb = Math.max(1024, Math.min(32768, memoryMb));
        width = Math.max(320, width);
        height = Math.max(240, height);
    }

    public synchronized void save() {
        clamp();
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("version", version);
        m.put("memoryMb", memoryMb);
        m.put("width", width);
        m.put("height", height);
        m.put("fullscreen", fullscreen);
        m.put("jvmArgs", jvmArgs);
        m.put("javaPath", javaPath);
        m.put("afterLaunch", afterLaunch);
        m.put("msClientId", msClientId);
        writeAtomic(LauncherPaths.SETTINGS, Json.pretty(m));
    }

    static void writeAtomic(File f, String text) {
        try {
            f.getParentFile().mkdirs();
            File tmp = new File(f.getPath() + ".tmp");
            Files.write(tmp.toPath(), text.getBytes(StandardCharsets.UTF_8));
            Files.move(tmp.toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            dev.coolclient.core.util.Log.error("Failed to write " + f, e);
        }
    }
}
