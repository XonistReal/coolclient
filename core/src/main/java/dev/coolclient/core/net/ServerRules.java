package dev.coolclient.core.net;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.gui.Notifications;
import dev.coolclient.core.module.Module;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Turns off modules that particular servers don't allow. Rules match the server address or any
 * parent domain (e.g. "mc.hypixel.net" matches "hypixel.net").
 */
public final class ServerRules {
    private final Map<String, List<String>> blocked = new HashMap<String, List<String>>();

    public ServerRules() {
        // Hypixel asks clients to disable free look.
        blocked.put("hypixel.net", new ArrayList<String>(Arrays.asList("freelook")));
    }

    public void addRule(String domain, String moduleId) {
        String d = domain.toLowerCase(Locale.ROOT);
        List<String> l = blocked.get(d);
        if (l == null) {
            l = new ArrayList<String>();
            blocked.put(d, l);
        }
        if (!l.contains(moduleId)) l.add(moduleId);
    }

    public void apply(String serverAddress) {
        List<String> ids = new ArrayList<String>();
        if (serverAddress != null) {
            String host = serverAddress.toLowerCase(Locale.ROOT);
            int colon = host.lastIndexOf(':');
            if (colon > 0 && host.indexOf(':') == colon) host = host.substring(0, colon);
            while (host.endsWith(".")) host = host.substring(0, host.length() - 1);
            for (Map.Entry<String, List<String>> e : blocked.entrySet()) {
                if (host.equals(e.getKey()) || host.endsWith("." + e.getKey())) ids.addAll(e.getValue());
            }
        }
        for (Module m : CoolClient.get().modules().all()) {
            boolean block = ids.contains(m.id());
            if (block && !m.isBlocked() && m.isEnabledRaw()) {
                CoolClient.get().notifications().post(m.name() + " disabled", "Not allowed on this server", Notifications.Level.WARNING);
            }
            m.setBlocked(block, block ? "Disabled by server rules" : null);
        }
    }
}
