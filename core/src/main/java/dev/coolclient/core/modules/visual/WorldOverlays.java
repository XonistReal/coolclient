package dev.coolclient.core.modules.visual;

import dev.coolclient.core.api.WorldDraw;
import dev.coolclient.core.api.WorldEntity;
import dev.coolclient.core.event.Events;
import dev.coolclient.core.module.Category;
import dev.coolclient.core.module.Module;
import dev.coolclient.core.render.Colors;
import dev.coolclient.core.setting.BoolSetting;
import dev.coolclient.core.setting.ColorSetting;
import dev.coolclient.core.setting.ModeSetting;
import dev.coolclient.core.setting.NumberSetting;
import dev.coolclient.core.util.MathUtil;

import java.util.List;

/** Things drawn into the world itself. None of them show anything through walls. */
public final class WorldOverlays {
    private WorldOverlays() {}

    private static void line(WorldDraw d, double x1, double y1, double z1, double x2, double y2, double z2, int c) {
        d.vertex(x1, y1, z1, c);
        d.vertex(x2, y2, z2, c);
    }

    // ================================================================== chunk borders

    public static final class ChunkBorders extends Module {
        private final ColorSetting corners = add(new ColorSetting("Corner Color", 0xFFFFE040));
        private final ColorSetting neighbours = add(new ColorSetting("Neighbour Color", 0xFFFF4040));
        private final ColorSetting grid = add(new ColorSetting("Grid Color", 0x7040E0FF));
        private final BoolSetting showGrid = add(new BoolSetting("Wall Grid", true));
        private final NumberSetting height = add(new NumberSetting("Height", 32, 8, 128, 8).suffix(" blocks")
                .describe("How far above and below you the lines go"));

        public ChunkBorders() {
            super("Chunk Borders", "Outlines the chunk you're in (bind a key to flip it on and off).", Category.VISUAL);
            on(Events.RenderWorld.class, e -> render(e.draw));
        }

        private void render(WorldDraw d) {
            if (!game().inWorld()) return;
            int cx = (int) Math.floor(game().x()) >> 4, cz = (int) Math.floor(game().z()) >> 4;
            double x0 = cx * 16, z0 = cz * 16, x1 = x0 + 16, z1 = z0 + 16;
            double py = Math.floor(game().y());
            double y0 = py - height.d(), y1 = py + height.d();
            d.lines(false);
            // Neighbouring chunks' corners, then this chunk's corners.
            for (int i = -1; i <= 2; i++)
                for (int j = -1; j <= 2; j++) {
                    if ((i == 0 || i == 1) && (j == 0 || j == 1)) continue;
                    double x = x0 + i * 16, z = z0 + j * 16;
                    line(d, x, y0, z, x, y1, z, neighbours.color());
                }
            for (double x : new double[] {x0, x1})
                for (double z : new double[] {z0, z1}) line(d, x, y0, z, x, y1, z, corners.color());
            if (showGrid.on()) {
                int g = grid.color();
                for (int k = 2; k < 16; k += 2) {
                    line(d, x0 + k, y0, z0, x0 + k, y1, z0, g);
                    line(d, x0 + k, y0, z1, x0 + k, y1, z1, g);
                    line(d, x0, y0, z0 + k, x0, y1, z0 + k, g);
                    line(d, x1, y0, z0 + k, x1, y1, z0 + k, g);
                }
                for (double y = Math.ceil(y0 / 2) * 2; y <= y1; y += 2) {
                    // Section borders (every 16 blocks) stand out.
                    int c = ((int) y & 15) == 0 ? Colors.withAlpha(0xFF4060FF, 200) : g;
                    line(d, x0, y, z0, x1, y, z0, c);
                    line(d, x1, y, z0, x1, y, z1, c);
                    line(d, x1, y, z1, x0, y, z1, c);
                    line(d, x0, y, z1, x0, y, z0, c);
                }
            }
            d.end();
        }
    }

    // ================================================================== TNT timer

    public static final class TntTimer extends Module {
        private final NumberSetting decimals = add(new NumberSetting("Decimals", 1, 0, 2, 1));
        private final NumberSetting scale = add(new NumberSetting("Scale", 1, 0.5, 2.5, 0.1).suffix("x"));

        public TntTimer() {
            super("TNT Timer", "Shows how long lit TNT has left before it blows.", Category.VISUAL);
            on(Events.RenderWorld.class, e -> {
                if (!game().inWorld()) return;
                List<WorldEntity> tnt = game().primedTnt(e.partialTicks);
                for (WorldEntity t : tnt) {
                    double secs = Math.max(0, (t.fuse - e.partialTicks) / 20.0);
                    float k = (float) MathUtil.clamp(secs / 4.0, 0, 1);
                    int c = Colors.lerp(0xFFFF3B3B, 0xFF5CFF6B, k);
                    e.draw.text(MathUtil.format(secs, decimals.i()), t.x, t.y + t.height + 0.45, t.z,
                            0.025f * scale.f(), c, false, 0x60000000);
                }
            });
        }
    }

    // ================================================================== loot beams

    public static final class LootBeams extends Module {
        private final ModeSetting minimum = add(new ModeSetting("Show For", "Uncommon+", "All Items", "Uncommon+", "Rare+", "Epic Only"));
        private final NumberSetting beamHeight = add(new NumberSetting("Beam Height", 3, 1, 12, 0.5).suffix(" blocks"));
        private final NumberSetting range = add(new NumberSetting("Range", 48, 8, 128, 8).suffix(" blocks"));
        private final ColorSetting common = add(new ColorSetting("Common Color", 0xFFFFFFFF));

        public LootBeams() {
            super("Loot Beams", "Dropped items send up a beam coloured by how rare they are.", Category.VISUAL);
            on(Events.RenderWorld.class, e -> render(e.draw, e.partialTicks));
        }

        private int minRarity() {
            return minimum.index();
        }

        private int color(int rarity) {
            switch (rarity) {
                case 1: return 0xFFFFE65C;
                case 2: return 0xFF55FFFF;
                case 3: return 0xFFFF55FF;
                default: return common.color();
            }
        }

        private void render(WorldDraw d, float pt) {
            if (!game().inWorld()) return;
            List<WorldEntity> items = game().droppedItems(pt, range.d());
            if (items.isEmpty()) return;
            double cx = d.cameraX(), cz = d.cameraZ();
            d.quads(false);
            float h = beamHeight.f();
            for (WorldEntity it : items) {
                int rarity = it.item.rarity();
                if (rarity < minRarity()) continue;
                int c = color(rarity);
                int bottom = Colors.withAlpha(c, 150), top = Colors.withAlpha(c, 0);
                // One quad turned toward the camera, so the beam always has its full width.
                double dx = it.x - cx, dz = it.z - cz;
                double len = Math.sqrt(dx * dx + dz * dz);
                double ox = len < 1e-4 ? 0.08 : -dz / len * 0.08, oz = len < 1e-4 ? 0 : dx / len * 0.08;
                double y0 = it.y + 0.1, y1 = it.y + h;
                d.vertex(it.x - ox, y0, it.z - oz, bottom);
                d.vertex(it.x + ox, y0, it.z + oz, bottom);
                d.vertex(it.x + ox, y1, it.z + oz, top);
                d.vertex(it.x - ox, y1, it.z - oz, top);
            }
            d.end();
        }
    }

    // ================================================================== light levels

    public static final class LightOverlay extends Module {
        private final ModeSetting mode = add(new ModeSetting("Mode", "Crosses", "Crosses", "Numbers"));
        private final NumberSetting radius = add(new NumberSetting("Radius", 16, 4, 32, 1).suffix(" blocks"));
        private final BoolSetting safe = add(new BoolSetting("Show Safe Spots", false).describe("Green marks where mobs can't spawn"));
        private int[] spots = new int[0];
        private int ticks;

        public LightOverlay() {
            super("Light Levels", "Marks spots where mobs can spawn: red always, yellow at night.", Category.VISUAL);
            on(Events.Tick.class, e -> {
                if (!game().inWorld()) {
                    spots = new int[0];
                    return;
                }
                if (ticks++ % 10 == 0) spots = game().spawnSpots(radius.i(), 8);
            });
            on(Events.RenderWorld.class, e -> render(e.draw));
        }

        @Override
        protected void onEnable() { ticks = 0; }

        /** 1.18+ only spawns mobs in block light 0; 1.8 spawns them up to light 7. */
        private static boolean legacy() { return platform().minecraftVersion().startsWith("1.8"); }

        /** 0 = safe, 1 = spawns at night only, 2 = spawns any time. */
        private static int danger(int block, int sky) {
            int limit = legacy() ? 7 : 0;
            if (block > limit) return 0;
            return sky > limit ? 1 : 2;
        }

        private void render(WorldDraw d) {
            int[] s = spots;
            if (s.length == 0) return;
            boolean numbers = mode.is("Numbers");
            if (!numbers) d.lines(false);
            int shown = 0;
            for (int i = 0; i + 4 < s.length; i += 5) {
                int danger = danger(s[i + 3], s[i + 4]);
                if (danger == 0 && !safe.on()) continue;
                int c = danger == 2 ? 0xFFFF3030 : danger == 1 ? 0xFFFFD030 : 0xFF40FF60;
                double x = s[i], y = s[i + 1] + 0.02, z = s[i + 2];
                if (numbers) {
                    if (shown++ > 400) break;
                    d.text(String.valueOf(s[i + 3]), x + 0.5, y + 0.2, z + 0.5, 0.02f, c, false, 0);
                } else {
                    double m = 0.2;
                    line(d, x + m, y, z + m, x + 1 - m, y, z + 1 - m, c);
                    line(d, x + 1 - m, y, z + m, x + m, y, z + 1 - m, c);
                }
            }
            if (!numbers) d.end();
        }
    }

    // ================================================================== mob outline

    public static final class MobOutline extends Module {
        private final ColorSetting color = add(new ColorSetting("Outline Color", 0xFFFFFFFF));
        private final BoolSetting fill = add(new BoolSetting("Fill", false));
        private final ColorSetting fillColor = add(new ColorSetting("Fill Color", 0x30FFFFFF).visibleWhen(fill::on));

        public MobOutline() {
            super("Mob Outline", "Outlines the player or mob under your crosshair, like the block outline.", Category.VISUAL);
            on(Events.RenderWorld.class, e -> {
                if (!game().inWorld()) return;
                double[] b = game().targetEntityBox(e.partialTicks);
                if (b == null) return;
                double g = 0.02;
                double x0 = b[0] - g, y0 = b[1] - g, z0 = b[2] - g, x1 = b[3] + g, y1 = b[4] + g, z1 = b[5] + g;
                if (fill.on()) {
                    int f = fillColor.color();
                    double[][] faces = {
                        {x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1}, {x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0},
                        {x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0}, {x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1},
                        {x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0}, {x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1}};
                    e.draw.quads(false);
                    for (double[] q : faces)
                        for (int i = 0; i < 4; i++) e.draw.vertex(q[i * 3], q[i * 3 + 1], q[i * 3 + 2], f);
                    e.draw.end();
                }
                int c = color.color();
                WorldDraw d = e.draw;
                d.lines(false);
                double[] xs = {x0, x1}, ys = {y0, y1}, zs = {z0, z1};
                for (int i = 0; i < 2; i++)
                    for (int j = 0; j < 2; j++) {
                        line(d, xs[0], ys[i], zs[j], xs[1], ys[i], zs[j], c);
                        line(d, xs[i], ys[0], zs[j], xs[i], ys[1], zs[j], c);
                        line(d, xs[i], ys[j], zs[0], xs[i], ys[j], zs[1], c);
                    }
                d.end();
            });
        }
    }
}
