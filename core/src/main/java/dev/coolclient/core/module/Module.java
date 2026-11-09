package dev.coolclient.core.module;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.api.Feature;
import dev.coolclient.core.event.EventBus;
import dev.coolclient.core.json.Json;
import dev.coolclient.core.setting.KeySetting;
import dev.coolclient.core.setting.Setting;
import dev.coolclient.core.util.Keys;
import dev.coolclient.core.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Base class for every CoolClient module. */
public abstract class Module implements EventBus.Owner {
    private String id;
    private final String name;
    private final String description;
    private final Category category;
    private final List<Setting<?>> settings = new ArrayList<Setting<?>>();
    private final Feature[] requires;
    private boolean enabled;
    private boolean blocked;
    private String blockReason;
    private boolean defaultEnabled;
    private String versionTag;

    protected final KeySetting keybind = new KeySetting("Keybind", Keys.NONE);

    protected Module(String name, String description, Category category, Feature... requires) {
        this.name = name;
        this.id = name.toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
        this.description = description;
        this.category = category;
        this.requires = requires;
    }

    // ------------------------------------------------------------------ identity

    public String id() { return id; }

    /** Keeps the config id of a module that was renamed, so saved settings still load. */
    protected void keepId(String oldId) { this.id = oldId; }

    public String name() { return name; }

    public String description() { return description; }

    public Category category() { return category; }

    public Feature[] requires() { return requires; }

    /** Small badge shown on version-specific modules, e.g. "1.8" or "1.21". */
    public String versionTag() { return versionTag; }

    protected void versionTag(String tag) { this.versionTag = tag; }

    /** Modules that can't be turned off (e.g. client settings) return false. */
    public boolean toggleable() { return true; }

    /** Whether the module's keybind setting should be shown. */
    public boolean bindable() { return true; }

    // ------------------------------------------------------------------ state

    public boolean isEnabled() { return (enabled || !toggleable()) && !blocked; }

    /** Raw user preference, ignoring server blocks. */
    public boolean isEnabledRaw() { return enabled; }

    @Override
    public boolean isListening() { return isEnabled(); }

    public void setEnabled(boolean on) {
        if (on == enabled) return;
        boolean wasActive = isEnabled();
        enabled = on;
        transition(wasActive);
        CoolClient.get().config().markDirty();
    }

    public void toggle() { setEnabled(!enabled); }

    public boolean isBlocked() { return blocked; }

    public String blockReason() { return blockReason; }

    public void setBlocked(boolean blocked, String reason) {
        if (this.blocked == blocked) return;
        boolean wasActive = isEnabled();
        this.blocked = blocked;
        this.blockReason = reason;
        transition(wasActive);
    }

    private void transition(boolean wasActive) {
        boolean active = isEnabled();
        if (active == wasActive) return;
        try {
            if (active) onEnable(); else onDisable();
        } catch (Throwable t) {
            Log.error("Module " + name + " failed to " + (active ? "enable" : "disable"), t);
        }
    }

    /** Applied when no saved state exists (see {@link #load}), so onEnable still runs. */
    protected void enabledByDefault() {
        this.defaultEnabled = true;
    }

    public boolean defaultEnabled() { return defaultEnabled; }

    protected void onEnable() {}

    protected void onDisable() {}

    protected static dev.coolclient.core.api.GameState game() {
        return CoolClient.get().platform().game();
    }

    protected static dev.coolclient.core.api.Platform platform() {
        return CoolClient.get().platform();
    }

    /** True if a key code (keyboard or mouse) is currently held. */
    protected static boolean isDown(int code) {
        if (code == Keys.NONE) return false;
        if (Keys.isMouse(code)) return platform().input().isMouseDown(code - Keys.MOUSE_BASE);
        return platform().input().isKeyDown(code);
    }

    // ------------------------------------------------------------------ settings & events

    protected <S extends Setting<?>> S add(S setting) {
        settings.add(setting);
        return setting;
    }

    public List<Setting<?>> settings() { return Collections.unmodifiableList(settings); }

    public KeySetting keybind() { return keybind; }

    /** Subscribes to an event; the listener only fires while this module is enabled. */
    protected <E> void on(Class<E> type, EventBus.Listener<E> listener) {
        CoolClient.get().events().on(type, this, 0, listener);
    }

    /** Called when the module's keybind is pressed. Toggles by default. */
    public void onKeybind() {
        if (toggleable()) toggle();
    }

    public void resetSettings() {
        for (Setting<?> s : settings) s.reset();
        keybind.reset();
    }

    // ------------------------------------------------------------------ persistence

    public Map<String, Object> save() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("enabled", enabled);
        m.put("key", keybind.toJson());
        Map<String, Object> s = new LinkedHashMap<String, Object>();
        for (Setting<?> setting : settings) s.put(setting.id(), setting.toJson());
        m.put("settings", s);
        return m;
    }

    public void load(Map<String, Object> m) {
        if (m == null) {
            setEnabled(defaultEnabled);
            return;
        }
        keybind.fromJson(m.get("key"));
        Map<String, Object> s = Json.obj(m, "settings");
        if (s != null) {
            for (Setting<?> setting : settings) {
                if (s.containsKey(setting.id())) {
                    try {
                        setting.fromJson(s.get(setting.id()));
                    } catch (RuntimeException e) {
                        Log.warn("Bad value for " + id + "." + setting.id());
                    }
                }
            }
        }
        setEnabled(Json.bool(m, "enabled", defaultEnabled));
    }
}
