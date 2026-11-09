package dev.coolclient.core.hud;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.api.GuiRenderer;
import dev.coolclient.core.module.Module;
import dev.coolclient.core.util.Log;

import java.util.ArrayList;
import java.util.List;

/** Renders all enabled HUD elements. */
public final class HudManager {
    private final List<HudElement> elements = new ArrayList<HudElement>();

    public void collect() {
        elements.clear();
        for (Module m : CoolClient.get().modules().all()) {
            if (m instanceof HudElement) elements.add((HudElement) m);
        }
    }

    public List<HudElement> elements() { return elements; }

    public void render(GuiRenderer r) {
        if (dev.coolclient.core.modules.utility.UtilityTweaks.AutoHideHud.hidden()) return;
        for (HudElement e : elements) {
            if (!e.isEnabled() || !e.shouldRender()) continue;
            try {
                e.render(r, false);
            } catch (Throwable t) {
                Log.error("HUD element " + e.name() + " failed; disabling it", t);
                e.setEnabled(false);
            }
        }
    }

    public void resetAll() {
        for (HudElement e : elements) e.resetPosition();
        CoolClient.get().config().markDirty();
    }
}
