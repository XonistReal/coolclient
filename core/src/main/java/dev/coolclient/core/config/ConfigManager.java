package dev.coolclient.core.config;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.json.Json;
import dev.coolclient.core.module.Module;
import dev.coolclient.core.util.Log;
import dev.coolclient.core.util.Scheduler;
import dev.coolclient.core.util.Time;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Persists module state into profiles under {@code <gameDir>/coolclient/profiles}. Saves are
 * debounced (one second after the last change) and written atomically.
 */
public final class ConfigManager {
    private final File dir;
    private final File profilesDir;
    private final File clientFile;
    private String activeProfile = "default";
    private Map<String, Object> clientData = new LinkedHashMap<String, Object>();
    private volatile boolean dirty;
    private long dirtySince;
    private boolean loading;

    public ConfigManager(File gameDir) {
        // The launcher points every version (and itself) at one shared folder.
        String shared = System.getProperty("coolclient.dataDir");
        this.dir = shared != null && !shared.isEmpty() ? new File(shared) : new File(gameDir, "coolclient");
        this.profilesDir = new File(dir, "profiles");
        this.clientFile = new File(dir, "client.json");
        profilesDir.mkdirs();
    }

    public File dir() { return dir; }

    public String activeProfile() { return activeProfile; }

    /** Global (non-profile) client data such as the active profile and API session. */
    public Map<String, Object> clientData() { return clientData; }

    public void load() {
        loading = true;
        try {
            Map<String, Object> c = readJson(clientFile);
            if (c != null) clientData = c;
            activeProfile = Json.str(clientData, "profile", "default");
            loadProfile(activeProfile);
        } finally {
            loading = false;
        }
    }

    private void loadProfile(String name) {
        Map<String, Object> data = readJson(profileFile(name));
        Map<String, Object> modules = data == null ? null : Json.obj(data, "modules");
        for (Module m : CoolClient.get().modules().all()) {
            Map<String, Object> md = modules == null ? null : Json.obj(modules, m.id());
            try {
                m.load(md);
            } catch (Throwable t) {
                Log.error("Failed to load settings for " + m.id(), t);
            }
        }
    }

    public void markDirty() {
        if (loading) return;
        if (!dirty) dirtySince = Time.millis();
        dirty = true;
    }

    /** Called every tick; flushes pending changes after a short debounce. */
    public void tick() {
        if (dirty && Time.millis() - dirtySince > 1000) save();
    }

    public void save() {
        dirty = false;
        Map<String, Object> modules = new LinkedHashMap<String, Object>();
        for (Module m : CoolClient.get().modules().all()) modules.put(m.id(), m.save());
        Map<String, Object> profile = new LinkedHashMap<String, Object>();
        profile.put("version", 1);
        profile.put("modules", modules);
        final String profileJson = Json.pretty(profile);
        clientData.put("profile", activeProfile);
        final String clientJson = Json.pretty(clientData);
        final File pf = profileFile(activeProfile);
        Scheduler.async(() -> {
            write(pf, profileJson);
            write(clientFile, clientJson);
        });
    }

    /** Synchronous save used on shutdown. */
    public void saveNow() {
        Map<String, Object> modules = new LinkedHashMap<String, Object>();
        for (Module m : CoolClient.get().modules().all()) modules.put(m.id(), m.save());
        Map<String, Object> profile = new LinkedHashMap<String, Object>();
        profile.put("version", 1);
        profile.put("modules", modules);
        clientData.put("profile", activeProfile);
        write(profileFile(activeProfile), Json.pretty(profile));
        write(clientFile, Json.pretty(clientData));
        dirty = false;
    }

    // ------------------------------------------------------------------ profiles

    public List<String> profiles() {
        List<String> list = new ArrayList<String>();
        File[] files = profilesDir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.getName().endsWith(".json")) list.add(f.getName().substring(0, f.getName().length() - 5));
            }
        }
        if (!list.contains(activeProfile)) list.add(activeProfile);
        Collections.sort(list);
        return list;
    }

    public void switchProfile(String name) {
        if (name.equals(activeProfile)) return;
        saveNow();
        activeProfile = sanitize(name);
        loading = true;
        try {
            loadProfile(activeProfile);
        } finally {
            loading = false;
        }
        saveNow();
    }

    public void createProfile(String name) {
        name = sanitize(name);
        if (name.isEmpty()) return;
        saveNow();
        activeProfile = name;
        saveNow(); // copies current settings into the new profile
    }

    public void deleteProfile(String name) {
        if (name.equals(activeProfile)) return;
        File f = profileFile(name);
        if (f.exists() && !f.delete()) Log.warn("Could not delete profile " + name);
    }

    private static String sanitize(String name) {
        return name.trim().replaceAll("[^A-Za-z0-9 _-]", "").replace(' ', '_');
    }

    private File profileFile(String name) { return new File(profilesDir, name + ".json"); }

    // ------------------------------------------------------------------ io

    public static Map<String, Object> readJson(File f) {
        if (!f.isFile()) return null;
        try {
            return Json.parseObject(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
        } catch (Exception e) {
            Log.warn("Could not read " + f + ": " + e.getMessage());
            File bak = new File(f.getParentFile(), f.getName() + ".broken");
            f.renameTo(bak);
            return null;
        }
    }

    public static void write(File f, String content) {
        try {
            f.getParentFile().mkdirs();
            File tmp = new File(f.getParentFile(), f.getName() + ".tmp");
            Files.write(tmp.toPath(), content.getBytes(StandardCharsets.UTF_8));
            Files.move(tmp.toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Log.error("Could not write " + f, e);
        }
    }
}
