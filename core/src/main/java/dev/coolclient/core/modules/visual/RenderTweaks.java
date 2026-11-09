package dev.coolclient.core.modules.visual;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.api.GuiRenderer;
import dev.coolclient.core.api.ItemRef;
import dev.coolclient.core.event.Events;
import dev.coolclient.core.module.Category;
import dev.coolclient.core.module.Module;
import dev.coolclient.core.render.Colors;
import dev.coolclient.core.render.gpu.GpuDraw;
import dev.coolclient.core.render.gpu.PostFx;
import dev.coolclient.core.setting.BoolSetting;
import dev.coolclient.core.setting.ColorSetting;
import dev.coolclient.core.setting.NumberSetting;
import dev.coolclient.core.util.Time;

/** Settings-heavy visual modules whose work happens in the version mixins (or a post pass). */
public final class RenderTweaks {
    private RenderTweaks() {}

    private static <T extends Module> T active(Class<T> type) {
        CoolClient c = CoolClient.get();
        if (c == null) return null;
        T m = c.modules().get(type);
        return m != null && m.isEnabled() ? m : null;
    }

    // ================================================================== enchantment glint

    public static final class Glint extends Module {
        public final ColorSetting color = add(new ColorSetting("Glint Color", 0xFFA755FF).describe("The default is the vanilla purple"));
        public final BoolSetting shinyPotions = add(new BoolSetting("Shiny Potions", true)
                .describe("Potions with effects glint like enchanted items")
                .visibleWhen(() -> !platform().minecraftVersion().startsWith("1.8")));

        public Glint() {
            super("Glint", "Recolour the enchantment glint (and make potions shine).", Category.VISUAL);
        }

        /** Glint colour as {r, g, b} in 0..1 when the module is on, or null for vanilla. */
        public static float[] rgb() {
            Glint g = active(Glint.class);
            if (g == null) return null;
            int c = g.color.color();
            return new float[] {Colors.r(c) / 255f, Colors.g(c) / 255f, Colors.b(c) / 255f};
        }

        public static boolean potions() {
            Glint g = active(Glint.class);
            return g != null && g.shinyPotions.on();
        }
    }

    // ================================================================== fog

    public static final class Fog extends Module {
        public final BoolSetting noTerrain = add(new BoolSetting("Remove Distance Fog", false));
        public final NumberSetting distance = add(new NumberSetting("Fog Distance", 100, 25, 300, 5).suffix("%")
                .visibleWhen(() -> !noTerrain.on()));
        public final NumberSetting water = add(new NumberSetting("Water Visibility", 150, 50, 400, 10).suffix("%"));

        public Fog() {
            super("Fog", "Push distance fog back (or remove it) and see further underwater.", Category.VISUAL);
        }

        /** Multiplier for distance fog start/end, {@code Float.POSITIVE_INFINITY} for none, 1 when off. */
        public static float terrain() {
            Fog f = active(Fog.class);
            if (f == null) return 1f;
            return f.noTerrain.on() ? Float.POSITIVE_INFINITY : f.distance.f() / 100f;
        }

        public static float water() {
            Fog f = active(Fog.class);
            return f == null ? 1f : f.water.f() / 100f;
        }
    }

    // ================================================================== UHC overlay

    public static final class UhcOverlay extends Module {
        public final NumberSetting gold = add(new NumberSetting("Gold Ingots & Nuggets", 2, 1, 4, 0.25).suffix("x"));
        public final NumberSetting apples = add(new NumberSetting("Golden Apples", 2, 1, 4, 0.25).suffix("x"));
        public final NumberSetting heads = add(new NumberSetting("Heads", 2, 1, 4, 0.25).suffix("x"));
        public final NumberSetting ore = add(new NumberSetting("Gold Ore", 1.5, 1, 4, 0.25).suffix("x"));

        public UhcOverlay() {
            super("UHC Overlay", "Makes dropped gold, golden apples and heads bigger so you spot them.", Category.VISUAL);
        }

        /** Scale for a dropped item of this id (1 = vanilla). */
        public static float scale(String id) {
            UhcOverlay u = active(UhcOverlay.class);
            if (u == null || id == null) return 1f;
            if (id.endsWith("gold_ingot") || id.endsWith("gold_nugget") || id.endsWith("raw_gold")) return u.gold.f();
            if (id.endsWith("golden_apple")) return u.apples.f();
            if (id.endsWith("_head") || id.endsWith("skull")) return u.heads.f();
            if (id.endsWith("gold_ore")) return u.ore.f();
            return 1f;
        }
    }

    // ================================================================== 2D items

    public static final class FlatItems extends Module {
        public FlatItems() {
            super("2D Items", "Dropped items always face you, like flat sprites.", Category.VISUAL);
        }

        public static boolean on() { return active(FlatItems.class) != null; }
    }

    // ================================================================== titles

    public static final class Titles extends Module {
        public final NumberSetting scale = add(new NumberSetting("Scale", 70, 30, 150, 5).suffix("%"));
        public final BoolSetting hide = add(new BoolSetting("Hide Titles", false));

        public Titles() {
            super("Titles", "Shrink or hide the big on-screen titles servers send.", Category.VISUAL);
        }

        /** Scale for titles, 0 to hide them, 1 when off. */
        public static float factor() {
            Titles t = active(Titles.class);
            if (t == null) return 1f;
            return t.hide.on() ? 0f : t.scale.f() / 100f;
        }
    }

    // ================================================================== camera

    public static final class Camera extends Module {
        public final NumberSetting distance = add(new NumberSetting("Third Person Distance", 4, 1, 12, 0.5).suffix(" blocks"));
        public final BoolSetting skipFront = add(new BoolSetting("Skip Front View", false)
                .describe("F5 goes first person, back view, first person"));
        private int lastPerspective;

        public Camera() {
            super("Camera", "Third-person camera distance, and F5 without the front view.", Category.VISUAL);
            on(Events.Tick.class, e -> {
                if (!game().inWorld()) return;
                int p = game().perspective();
                // Only the F5 step from back view to front view; Freelook and Snaplook may pick front on purpose.
                Freelook fl = CoolClient.get().modules().get(Freelook.class);
                boolean freelook = fl != null && fl.active();
                if (skipFront.on() && p == 2 && lastPerspective == 1 && !freelook && !Snaplook.holding()) {
                    game().setPerspective(0);
                    p = 0;
                }
                lastPerspective = p;
            });
        }

        /** Third-person camera distance in blocks (vanilla 4). */
        public static float thirdPersonDistance() {
            Camera c = active(Camera.class);
            return c == null ? 4f : c.distance.f();
        }
    }

    // ================================================================== armor bar

    public static final class ArmorBar extends Module {
        public final BoolSetting materials = add(new BoolSetting("Colour By Material", true));
        public final BoolSetting enchanted = add(new BoolSetting("Pulse Enchanted Pieces", true));

        public ArmorBar() {
            super("Armor Bar", "Colours each armor icon by the piece behind it, and pulses enchanted ones.", Category.VISUAL);
        }

        private static int material(String id) {
            if (id.contains("netherite")) return 0xFF6E6478;
            if (id.contains("diamond")) return 0xFF5CF2E8;
            if (id.contains("gold")) return 0xFFFFD84A;
            if (id.contains("chainmail")) return 0xFFB8BEC8;
            if (id.contains("iron")) return 0xFFE6E6E6;
            if (id.contains("leather")) return 0xFFB5754A;
            if (id.contains("turtle")) return 0xFF57C24A;
            return 0xFFFFFFFF;
        }

        /** Armor points a piece gives, by vanilla's defaults (helmet, chest, legs, boots). */
        private static int points(String id) {
            String m = id.contains("netherite") || id.contains("diamond") ? "d" : id.contains("iron") ? "i"
                    : id.contains("chainmail") ? "c" : id.contains("gold") ? "g" : id.contains("leather") ? "l"
                    : id.contains("turtle") ? "t" : "";
            int slot = id.endsWith("helmet") ? 0 : id.endsWith("chestplate") ? 1 : id.endsWith("leggings") ? 2 : id.endsWith("boots") ? 3 : -1;
            if (slot < 0) return 0;
            int[] d = {3, 8, 6, 3}, i = {2, 6, 5, 2}, c = {2, 5, 4, 1}, g = {2, 5, 3, 1}, l = {1, 3, 2, 1};
            switch (m) {
                case "d": return d[slot];
                case "i": return i[slot];
                case "c": return c[slot];
                case "g": return g[slot];
                case "l": return l[slot];
                case "t": return slot == 0 ? 2 : 0;
                default: return 0;
            }
        }

        /**
         * Tint for vanilla armor icon {@code index} (0..9, left to right; each covers two points), or
         * 0xFFFFFFFF to leave it alone. Pieces fill from the helmet down.
         */
        public static int tint(int index) {
            ArmorBar a = active(ArmorBar.class);
            if (a == null || !game().inWorld()) return 0xFFFFFFFF;
            int point = index * 2 + 1, sum = 0;
            for (int slot = 3; slot >= 0; slot--) {
                ItemRef piece = game().armor(slot);
                if (piece.isEmpty()) continue;
                sum += points(piece.id());
                if (point <= sum) {
                    int c = a.materials.on() ? material(piece.id()) : 0xFFFFFFFF;
                    if (a.enchanted.on() && !piece.enchantments().isEmpty()) {
                        float k = 0.5f + 0.5f * (float) Math.sin(Time.seconds() * 4 + index * 0.6);
                        c = Colors.lerp(c, 0xFFD58CFF, k * 0.6f);
                    }
                    return c;
                }
            }
            return 0xFFFFFFFF;
        }
    }

    // ================================================================== menu blur

    public static final class MenuBlur extends Module {
        public final NumberSetting strength = add(new NumberSetting("Strength", 3, 1, 6, 0.5));
        public final NumberSetting darken = add(new NumberSetting("Darken", 25, 0, 80, 5).suffix("%"));
        public final BoolSetting containers = add(new BoolSetting("In Inventories", true));
        public final BoolSetting others = add(new BoolSetting("In Other Menus", true).describe("Pause menu, options ..."));

        public MenuBlur() {
            super("Menu Blur", "Blurs the game behind inventories and menus.", Category.VISUAL);
        }

        /** Draws the blur behind a vanilla screen; false if it didn't (module off, or no shaders). */
        public static boolean draw(GuiRenderer r, boolean container) {
            MenuBlur m = active(MenuBlur.class);
            if (m == null || !game().inWorld() || !(container ? m.containers.on() : m.others.on())) return false;
            int tint = Colors.withAlpha(0xFF0A0C12, Math.round(m.darken.f() * 2.55f));
            if (!GpuDraw.glass(r, 0, 0, r.width(), r.height(), 0, tint, 1f, m.strength.f())) return false;
            GpuDraw.invalidateBackdrop();
            return true;
        }

        public static boolean wanted(boolean container) {
            MenuBlur m = active(MenuBlur.class);
            return m != null && game().inWorld() && (container ? m.containers.on() : m.others.on());
        }
    }

    // ================================================================== colour grading

    public static final class ColorGrading extends Module {
        public final NumberSetting saturation = add(new NumberSetting("Saturation", 120, 0, 200, 5).suffix("%"));
        public final NumberSetting contrast = add(new NumberSetting("Contrast", 105, 50, 150, 5).suffix("%"));
        public final NumberSetting brightness = add(new NumberSetting("Brightness", 100, 50, 150, 5).suffix("%"));
        public final NumberSetting hue = add(new NumberSetting("Hue Shift", 0, -180, 180, 5).suffix("°"));

        public ColorGrading() {
            super("Color Grading", "Saturation, contrast, brightness and hue for the world.", Category.VISUAL);
            on(Events.PostWorld.class, e -> PostFx.grade(e.renderer, saturation.f() / 100f, contrast.f() / 100f,
                    brightness.f() / 100f, hue.f()));
        }
    }

    // ================================================================== motion blur on core-profile versions

    /** Frame blending for platforms whose {@code Gpu} can keep last frame (1.21.4). */
    public static void motionBlur(GuiRenderer r) {
        VisualTweaks.MotionBlur mb = active(VisualTweaks.MotionBlur.class);
        if (mb == null) {
            PostFx.resetBlur();
            return;
        }
        PostFx.blur(r, mb.amount());
    }
}
