package dev.coolclient.core.modules.utility;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.api.Feature;
import dev.coolclient.core.api.ItemRef;
import dev.coolclient.core.gui.Notifications;
import dev.coolclient.core.module.Category;
import dev.coolclient.core.module.Module;
import dev.coolclient.core.modules.visual.Freelook;
import dev.coolclient.core.modules.visual.Zoom;
import dev.coolclient.core.setting.BoolSetting;
import dev.coolclient.core.setting.NumberSetting;
import dev.coolclient.core.util.Time;

/** Utility modules whose work happens in the version mixins; they hold settings and decisions. */
public final class UtilityTweaks {
    private UtilityTweaks() {}

    private static <T extends Module> T active(Class<T> type) {
        CoolClient c = CoolClient.get();
        if (c == null) return null;
        T m = c.modules().get(type);
        return m != null && m.isEnabled() ? m : null;
    }

    // ================================================================== auto hide HUD

    public static final class AutoHideHud extends Module {
        private final BoolSetting zooming = add(new BoolSetting("While Zooming", true));
        private final BoolSetting thirdPerson = add(new BoolSetting("In Third Person", false));
        private final BoolSetting freelook = add(new BoolSetting("While Freelooking", true));
        private final BoolSetting debug = add(new BoolSetting("With F3 Open", true));
        private final BoolSetting menus = add(new BoolSetting("In Menus", false).describe("Inventory, chat and other screens"));

        public AutoHideHud() {
            super("Auto Hide HUD", "Hides your HUD elements while zooming, in F3 or in menus.", Category.HUD);
        }

        /** True when Cool Client's HUD elements should stay hidden this frame. */
        public static boolean hidden() {
            AutoHideHud a = active(AutoHideHud.class);
            if (a == null || !game().inWorld()) return false;
            CoolClient c = CoolClient.get();
            Zoom z = c.modules().get(Zoom.class);
            if (a.zooming.on() && z != null && z.zooming()) return true;
            Freelook f = c.modules().get(Freelook.class);
            if (a.freelook.on() && f != null && f.active()) return true;
            if (a.thirdPerson.on() && game().perspective() != 0) return true;
            if (a.debug.on() && game().debugOpen()) return true;
            return a.menus.on() && platform().anyScreenOpen();
        }
    }

    // ================================================================== drop protection

    public static final class DropProtection extends Module {
        private final BoolSetting weapons = add(new BoolSetting("Weapons", true).describe("Swords, axes, bows, tridents, maces"));
        private final BoolSetting tools = add(new BoolSetting("Tools", true));
        private final BoolSetting armor = add(new BoolSetting("Armor", true));
        private final BoolSetting enchanted = add(new BoolSetting("Anything Enchanted", true));
        private final BoolSetting rare = add(new BoolSetting("Rare Items", true).describe("Golden apples, totems, rare and epic items"));
        private long warnedAt;
        private String warnedItem;

        public DropProtection() {
            super("Drop Protection", "Press drop twice to throw away weapons, tools, armor and rare items.", Category.UTILITY);
        }

        private boolean protects(ItemRef s) {
            String id = s.id();
            if (weapons.on() && (id.endsWith("_sword") || id.endsWith("_axe") || id.endsWith(":bow") || id.endsWith("crossbow")
                    || id.endsWith("trident") || id.endsWith(":mace"))) return true;
            if (tools.on() && (id.endsWith("_pickaxe") || id.endsWith("_shovel") || id.endsWith("_hoe") || id.endsWith("shears")
                    || id.endsWith("fishing_rod") || id.endsWith("flint_and_steel"))) return true;
            if (armor.on() && (id.endsWith("_helmet") || id.endsWith("_chestplate") || id.endsWith("_leggings") || id.endsWith("_boots")
                    || id.endsWith("elytra") || id.endsWith("shield"))) return true;
            if (enchanted.on() && !s.enchantments().isEmpty()) return true;
            return rare.on() && (s.rarity() >= 1 || id.endsWith("golden_apple") || id.endsWith("totem_of_undying"));
        }

        /** Called before the held item is dropped with the drop key; false cancels the drop. */
        public static boolean allow(ItemRef held) {
            DropProtection d = active(DropProtection.class);
            if (d == null || held == null || held.isEmpty() || !d.protects(held)) return true;
            long now = Time.millis();
            String key = held.id() + "|" + held.displayName();
            if (key.equals(d.warnedItem) && now - d.warnedAt < 1500) {
                d.warnedItem = null;
                return true;
            }
            d.warnedItem = key;
            d.warnedAt = now;
            CoolClient.get().notifications().post("Drop protected", "Press drop again to throw " + held.displayName(),
                    Notifications.Level.WARNING, 1500);
            return false;
        }
    }

    // ================================================================== toasts

    public static final class ToastControl extends Module {
        public final BoolSetting advancements = add(new BoolSetting("Hide Advancements", true).describe("Achievements on 1.8"));
        public final BoolSetting recipes = add(new BoolSetting("Hide Recipe Unlocks", true));
        public final BoolSetting tutorial = add(new BoolSetting("Hide Tutorial Hints", true));
        public final BoolSetting system = add(new BoolSetting("Hide System Messages", false).describe("World backups, pack loading ..."));

        public ToastControl() {
            super("Toast Control", "Hide the pop-ups in the top right: advancements, recipes, hints.", Category.UTILITY);
        }

        /** True if a toast of this kind ("advancement", "recipe", "tutorial", "system") should be dropped. */
        public static boolean hide(String kind) {
            ToastControl t = active(ToastControl.class);
            if (t == null) return false;
            switch (kind) {
                case "advancement": return t.advancements.on();
                case "recipe": return t.recipes.on();
                case "tutorial": return t.tutorial.on();
                case "system": return t.system.on();
                default: return false;
            }
        }
    }

    // ================================================================== reconnect

    public static final class Reconnect extends Module {
        public final BoolSetting auto = add(new BoolSetting("Auto Reconnect", false));
        public final NumberSetting delay = add(new NumberSetting("Delay", 5, 1, 60, 1).suffix("s").visibleWhen(auto::on));

        public Reconnect() {
            super("Reconnect", "Adds a Reconnect button when you get disconnected, with optional auto-reconnect.", Category.UTILITY);
            enabledByDefault();
        }

        /** Seconds left before auto-reconnect, given when the disconnect screen opened; -1 for manual only. */
        public static int countdown(long openedAt) {
            Reconnect r = active(Reconnect.class);
            if (r == null || !r.auto.on()) return -1;
            long left = (long) (r.delay.d() * 1000) - (Time.millis() - openedAt);
            return (int) Math.max(0, (left + 999) / 1000);
        }

        public static boolean on() { return active(Reconnect.class) != null; }
    }

    // ================================================================== tooltips

    public static final class Tooltips extends Module {
        public final BoolSetting scroll = add(new BoolSetting("Scroll Long Tooltips", true).describe("Mouse wheel moves tooltips taller than the screen"));
        public final BoolSetting durability = add(new BoolSetting("Durability Line", true));
        public final BoolSetting shulkers = add(new BoolSetting("Shulker Box Preview", true)
                .visibleWhen(() -> platform().supports(Feature.SHULKER_BOXES)));
        private static int offset;
        private static boolean tall;
        private static long tallFrame = -10;
        private static Object lastItem;

        public Tooltips() {
            super("Tooltips", "Scroll long tooltips, see durability, preview shulker boxes.", Category.UTILITY);
            enabledByDefault();
        }

        public static boolean scrolling() {
            Tooltips t = active(Tooltips.class);
            return t != null && t.scroll.on();
        }

        public static boolean durabilityLine() {
            Tooltips t = active(Tooltips.class);
            return t != null && t.durability.on();
        }

        public static boolean shulkerPreview() {
            Tooltips t = active(Tooltips.class);
            return t != null && t.shulkers.on();
        }

        /**
         * Called when a tooltip is about to draw: returns the vertical offset to apply. {@code item} is
         * whatever identifies the hovered stack; a new one resets the scroll.
         */
        public static int offset(Object item, int height, int screenHeight) {
            if (!scrolling()) return 0;
            // Keys are rebuilt every frame, so compare by value.
            if (!java.util.Objects.equals(item, lastItem)) {
                lastItem = item;
                offset = 0;
            }
            tall = height > screenHeight - 8;
            if (tall) tallFrame = Time.frameIndex();
            if (!tall) offset = 0;
            else offset = Math.max(-(height - screenHeight + 16), Math.min(0, offset));
            return offset;
        }

        /** Mouse wheel over a screen: true if it scrolled a tooltip (and should be consumed). */
        public static boolean scroll(double amount) {
            // Only while a tall tooltip is actually on screen (drawn in the last few frames).
            if (!scrolling() || !tall || Time.frameIndex() - tallFrame > 3) return false;
            offset += (int) Math.round(amount * 12);
            return true;
        }
    }

    // ================================================================== sounds

    public static final class Sounds extends Module {
        private final NumberSetting rain = add(new NumberSetting("Rain", 100, 0, 200, 5).suffix("%"));
        private final NumberSetting thunder = add(new NumberSetting("Thunder", 100, 0, 200, 5).suffix("%"));
        private final NumberSetting explosions = add(new NumberSetting("Explosions", 100, 0, 200, 5).suffix("%"));
        private final NumberSetting hurt = add(new NumberSetting("Player Hurt", 100, 0, 200, 5).suffix("%"));
        private final NumberSetting hits = add(new NumberSetting("Hits", 100, 0, 200, 5).suffix("%").describe("Attack swooshes and crits (1.9+)"));
        private final NumberSetting eating = add(new NumberSetting("Eating & Drinking", 100, 0, 200, 5).suffix("%"));
        private final NumberSetting fireworks = add(new NumberSetting("Fireworks", 100, 0, 200, 5).suffix("%"));
        private final NumberSetting portals = add(new NumberSetting("Nether Portals", 100, 0, 200, 5).suffix("%"));

        public Sounds() {
            super("Sounds", "Turn individual sounds up or down: rain, explosions, portals ...", Category.UTILITY);
        }

        /** Volume factor for a sound id (1 = unchanged). Works with both 1.8 and 1.21 sound names. */
        public static float volume(String id) {
            Sounds s = active(Sounds.class);
            if (s == null || id == null) return 1f;
            NumberSetting n = null;
            if (id.contains("weather.rain")) n = s.rain;
            else if (id.contains("thunder")) n = s.thunder;
            else if (id.contains("explode")) n = s.explosions;
            else if (id.contains("player.hurt")) n = s.hurt;
            else if (id.contains("player.attack")) n = s.hits;
            else if (id.contains(".eat") || id.contains(".drink") || id.contains("burp")) n = s.eating;
            else if (id.contains("firework")) n = s.fireworks;
            else if (id.contains("portal")) n = s.portals;
            return n == null ? 1f : n.f() / 100f;
        }
    }
}
