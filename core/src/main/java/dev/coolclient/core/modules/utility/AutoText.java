package dev.coolclient.core.modules.utility;

import dev.coolclient.core.event.Events;
import dev.coolclient.core.module.Category;
import dev.coolclient.core.module.Module;
import dev.coolclient.core.setting.KeySetting;
import dev.coolclient.core.setting.TextSetting;
import dev.coolclient.core.util.Keys;
import dev.coolclient.core.util.Time;

/** Text hotkeys: one message per key press, with a cooldown so it can't spam. */
public final class AutoText extends Module {
    private static final int SLOTS = 5;
    private final TextSetting[] texts = new TextSetting[SLOTS];
    private final KeySetting[] keys = new KeySetting[SLOTS];
    private long lastSent;

    public AutoText() {
        super("Text Hotkeys", "Bind chat messages or commands to keys.", Category.UTILITY);
        for (int i = 0; i < SLOTS; i++) {
            texts[i] = add(new TextSetting("Message " + (i + 1), i == 0 ? "/l" : "", 256));
            keys[i] = add(new KeySetting("Key " + (i + 1), Keys.NONE));
        }
        on(Events.Key.class, e -> {
            if (!e.pressed) return;
            for (int i = 0; i < SLOTS; i++) {
                if (keys[i].bound() && keys[i].key() == e.key && !texts[i].get().trim().isEmpty()) {
                    if (Time.millis() - lastSent < 1000) return;
                    lastSent = Time.millis();
                    platform().sendChat(texts[i].get().trim());
                    return;
                }
            }
        });
    }

    @Override
    public boolean bindable() { return false; }
}
