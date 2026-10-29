package dev.coolclient.core.module;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.api.Feature;
import dev.coolclient.core.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Owns every module; registers only those whose required features the platform supports. */
public final class ModuleManager {
    private final List<Module> modules = new ArrayList<Module>();
    private final Map<Class<?>, Module> byClass = new HashMap<Class<?>, Module>();
    private final Map<String, Module> byId = new HashMap<String, Module>();

    public void register(Module m) {
        for (Feature f : m.requires()) {
            if (!CoolClient.get().platform().supports(f)) return;
        }
        if (byId.containsKey(m.id())) {
            Log.warn("Duplicate module id " + m.id());
            return;
        }
        modules.add(m);
        byClass.put(m.getClass(), m);
        byId.put(m.id(), m);
    }

    public List<Module> all() { return Collections.unmodifiableList(modules); }

    @SuppressWarnings("unchecked")
    public <T extends Module> T get(Class<T> type) {
        return (T) byClass.get(type);
    }

    /** True if the module exists on this version and is active. */
    public boolean active(Class<? extends Module> type) {
        Module m = byClass.get(type);
        return m != null && m.isEnabled();
    }

    public Module byId(String id) { return byId.get(id); }

    public List<Module> inCategory(Category c) {
        List<Module> list = new ArrayList<Module>();
        for (Module m : modules) if (m.category() == c) list.add(m);
        return list;
    }

    /** Dispatches a key/mouse press to modules bound to it. */
    public void onBindPressed(int code) {
        for (Module m : modules) {
            if (m.keybind().bound() && m.keybind().key() == code && !m.isBlocked()) {
                try {
                    m.onKeybind();
                } catch (Throwable t) {
                    Log.error("Keybind for " + m.name() + " failed", t);
                }
            }
        }
    }
}
