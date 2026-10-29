package dev.coolclient.core.modules.utility;

import dev.coolclient.core.module.Category;
import dev.coolclient.core.module.Module;
import dev.coolclient.core.setting.BoolSetting;
import dev.coolclient.core.setting.ModeSetting;
import dev.coolclient.core.setting.NumberSetting;
import dev.coolclient.core.setting.TextSetting;

import java.text.SimpleDateFormat;
import java.util.Date;

/** Settings-only utility modules read by the version mixins. */
public final class UtilityModules {
    private UtilityModules() {}

    public static final class ChatTweaks extends Module {
        public final BoolSetting timestamps = add(new BoolSetting("Timestamps", false));
        public final ModeSetting timeFormat = add(new ModeSetting("Time Format", "HH:mm", "HH:mm", "HH:mm:ss", "h:mm a")
                .visibleWhen(timestamps::on));
        public final BoolSetting stack = add(new BoolSetting("Stack Duplicates", true).describe("Repeated lines show as (x2)"));
        public final NumberSetting background = add(new NumberSetting("Background Opacity", 50, 0, 100, 5).suffix("%"));
        public final BoolSetting longHistory = add(new BoolSetting("Longer History", true).describe("Keep 1000 lines instead of 100"));

        public ChatTweaks() {
            super("Chat", "Timestamps, spam stacking and longer history.", Category.UTILITY);
            enabledByDefault();
        }

        public String timestamp() {
            return "§8[§7" + new SimpleDateFormat(timeFormat.get()).format(new Date()) + "§8] §r";
        }
    }

    public static final class ScoreboardTweaks extends Module {
        public final BoolSetting hide = add(new BoolSetting("Hide Scoreboard", false));
        public final BoolSetting hideNumbers = add(new BoolSetting("Hide Red Numbers", true));
        public final NumberSetting background = add(new NumberSetting("Background Opacity", 30, 0, 100, 5).suffix("%"));
        public final NumberSetting scale = add(new NumberSetting("Scale", 1, 0.5, 1.5, 0.05));
        public final BoolSetting shadow = add(new BoolSetting("Text Shadow", false));

        public ScoreboardTweaks() {
            super("Scoreboard", "Clean up the sidebar scoreboard.", Category.UTILITY);
            enabledByDefault();
        }
    }

    public static final class TabListTweaks extends Module {
        public final BoolSetting numericPing = add(new BoolSetting("Numeric Ping", true));
        public final BoolSetting colorPing = add(new BoolSetting("Color Ping", true).visibleWhen(numericPing::on));

        public TabListTweaks() {
            super("Tab List", "Show exact ping numbers in the player list.", Category.UTILITY);
            enabledByDefault();
        }

        public static int pingColor(int ping) {
            if (ping < 0) return 0xFFAAAAAA;
            if (ping < 80) return 0xFF55FF55;
            if (ping < 150) return 0xFFFFFF55;
            if (ping < 300) return 0xFFFFAA00;
            return 0xFFFF5555;
        }
    }

    public static final class BossBarTweaks extends Module {
        public final BoolSetting hide = add(new BoolSetting("Hide Boss Bar", true));

        public BossBarTweaks() {
            super("Boss Bar", "Hide the boss bar (lobby advertisements, etc).", Category.UTILITY);
        }
    }

    public static final class NickHider extends Module {
        public final TextSetting nick = add(new TextSetting("Nickname", "You", 16));

        public NickHider() {
            super("Nick Hider", "Replace your name client-side (for recording).", Category.UTILITY);
        }

        public String apply(String text, String realName) {
            if (!isEnabled() || text == null || realName == null || realName.isEmpty()) return text;
            return text.replace(realName, nick.get());
        }
    }

    public static final class Waypoints extends Module {
        public final dev.coolclient.core.setting.KeySetting addKey = add(new dev.coolclient.core.setting.KeySetting("Add Waypoint Key", dev.coolclient.core.util.Keys.NONE));
        public final BoolSetting beam = add(new BoolSetting("Beacon Beam", true));
        public final BoolSetting distance = add(new BoolSetting("Show Distance", true));
        public final NumberSetting maxDistance = add(new NumberSetting("Max Distance", 2000, 50, 10000, 50).suffix("m"));

        public Waypoints() {
            super("Waypoints", "Your own markers in the world.", Category.UTILITY);
            on(dev.coolclient.core.event.Events.Key.class, e -> {
                if (e.pressed && addKey.bound() && e.key == addKey.key() && game().inWorld()) {
                    int n = dev.coolclient.core.CoolClient.get().waypoints().allForServer().size() + 1;
                    dev.coolclient.core.CoolClient.get().waypoints().add("Waypoint " + n,
                            (int) Math.floor(game().x()), (int) Math.floor(game().y()), (int) Math.floor(game().z()));
                    dev.coolclient.core.CoolClient.get().notifications().info("Waypoint added", "Waypoint " + n + " at your position");
                }
            });
        }
    }
}
