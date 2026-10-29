package dev.coolclient.core.modules.utility;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.event.Events;
import dev.coolclient.core.module.Category;
import dev.coolclient.core.module.Module;
import dev.coolclient.core.setting.BoolSetting;
import dev.coolclient.core.setting.NumberSetting;
import dev.coolclient.core.setting.TextSetting;
import dev.coolclient.core.util.Scheduler;
import dev.coolclient.core.util.Time;

/** Says gg when a game ends. Sends one message per game, never spams. */
public final class AutoGG extends Module {
    private static final String[] TRIGGERS = {
            "1st Killer - ", "1st Place - ", "Winner: ", " - Damage Dealt - ", "Winning Team -", "1st - ",
            "Winners: ", "Winner - ", "WINNER!", "Top Survivors", "won the game", "#1 Killer"
    };

    public final TextSetting message = add(new TextSetting("Message", "gg", 64));
    public final NumberSetting delay = add(new NumberSetting("Delay", 0.5, 0, 5, 0.1).suffix("s"));
    public final BoolSetting hypixelAllChat = add(new BoolSetting("Use /ac On Hypixel", true));
    public final TextSetting extraTriggers = add(new TextSetting("Extra Triggers", "", 200)
            .describe("Comma-separated phrases that also count as a game end"));
    private long lastSent;

    public AutoGG() {
        super("Auto GG", "Automatically says gg at the end of a game.", Category.UTILITY);
        on(Events.ChatReceived.class, e -> {
            if (!matches(e.plain) || Time.millis() - lastSent < 10_000) return;
            lastSent = Time.millis();
            final String msg = message.get().trim();
            if (msg.isEmpty()) return;
            final String server = CoolClient.get().server();
            final boolean hypixel = server != null && server.toLowerCase().contains("hypixel");
            long wait = (long) (delay.d() * 1000);
            Scheduler.async(() -> {
                try {
                    Thread.sleep(wait);
                } catch (InterruptedException ignored) {
                }
                Scheduler.main(() -> platform().sendChat(hypixel && hypixelAllChat.on() ? "/ac " + msg : msg));
            });
        });
    }

    private boolean matches(String line) {
        for (String t : TRIGGERS) if (line.contains(t)) return true;
        for (String t : extraTriggers.get().split(",")) {
            String s = t.trim();
            if (!s.isEmpty() && line.contains(s)) return true;
        }
        return false;
    }
}
