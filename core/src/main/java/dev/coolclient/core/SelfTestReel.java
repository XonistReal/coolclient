package dev.coolclient.core;

import dev.coolclient.core.cosmetic.CosmeticCatalog;
import dev.coolclient.core.cosmetic.CosmeticType;
import dev.coolclient.core.event.Events;
import dev.coolclient.core.gui.screen.CosmeticsScreen;
import dev.coolclient.core.gui.screen.HudEditorScreen;
import dev.coolclient.core.gui.screen.ModMenuScreen;
import dev.coolclient.core.hud.HudElement;
import dev.coolclient.core.module.Module;
import dev.coolclient.core.modules.cpvp.CrystalPvpTweaks;
import dev.coolclient.core.modules.cpvp.TotemPopCounter;
import dev.coolclient.core.modules.hud.ArmorStatusHud;
import dev.coolclient.core.modules.hud.ComboCounterHud;
import dev.coolclient.core.modules.hud.CoordinatesHud;
import dev.coolclient.core.modules.hud.CpsGraphHud;
import dev.coolclient.core.modules.hud.CpsHud;
import dev.coolclient.core.modules.hud.DirectionHud;
import dev.coolclient.core.modules.hud.DurabilityAlertsHud;
import dev.coolclient.core.modules.hud.FpsHud;
import dev.coolclient.core.modules.hud.ItemCounterHud;
import dev.coolclient.core.modules.hud.KeystrokesHud;
import dev.coolclient.core.modules.hud.PingHud;
import dev.coolclient.core.modules.hud.PotionEffectsHud;
import dev.coolclient.core.modules.hud.ReachDisplayHud;
import dev.coolclient.core.modules.legacy.LegacyModules;
import dev.coolclient.core.modules.utility.UtilityModules;
import dev.coolclient.core.modules.visual.BlockOutline;
import dev.coolclient.core.modules.visual.Crosshair;
import dev.coolclient.core.modules.visual.Freelook;
import dev.coolclient.core.modules.visual.HandChams;
import dev.coolclient.core.modules.visual.Skybox;
import dev.coolclient.core.modules.visual.ViewModel;
import dev.coolclient.core.modules.visual.VisualTweaks;
import dev.coolclient.core.modules.visual.Zoom;
import dev.coolclient.core.render.Theme;
import dev.coolclient.core.render.gpu.SkyStyle;
import dev.coolclient.core.setting.GridSetting;
import dev.coolclient.core.setting.Setting;
import dev.coolclient.core.util.Log;
import dev.coolclient.core.waypoint.WaypointManager.Waypoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.DoubleConsumer;

/**
 * Promo-footage variant of the self-test, enabled with {@code -Dcoolclient.selftest.reel=true} next to
 * {@code -Dcoolclient.selftest=true}. Instead of one screenshot per screen it records short clips as
 * ~30 fps frame sequences ({@code screenshots/reel-<clip>-<ms>.png}, named by milliseconds into the
 * clip): every theme on the main menu and mod menu, the HUD, a camera orbit around cosmetic loadouts,
 * and each shader sky.
 */
final class SelfTestReel {
    private static final double SETTLE = 0.6;

    /** One stage of the reel; returns true when it is finished. */
    private interface Step {
        boolean frame();
    }

    private final SelfTest.Driver driver;
    private final List<Step> steps = new ArrayList<Step>();
    private int index;
    private boolean busy;
    private float facing;

    static boolean enabled() {
        return Boolean.getBoolean("coolclient.selftest.reel");
    }

    /** {@code -Dcoolclient.selftest.reel.only=hud,sky-}: record only clips starting with these (setups still run). */
    private static boolean wanted(String clip) {
        String only = System.getProperty("coolclient.selftest.reel.only", "");
        if (only.isEmpty()) return true;
        for (String prefix : only.split(",")) if (clip.startsWith(prefix.trim())) return true;
        return false;
    }

    SelfTestReel(SelfTest.Driver driver) {
        this.driver = driver;
        CoolClient a = CoolClient.get();

        // Menus: every theme behind the main menu, then on the mod menu.
        for (final String theme : Theme.presetNames()) {
            steps.add(clip("menu-" + slug(theme), 2.0, () -> a.client().theme.set(theme), null));
        }
        steps.add(once(() -> a.platform().openScreen(new ModMenuScreen(null))));
        for (final String theme : Theme.presetNames()) {
            steps.add(clip("modmenu-" + slug(theme), 1.6, () -> a.client().theme.set(theme), null));
        }
        steps.add(clip("themes-page", 3.0, () -> {
            a.client().theme.set("Aurora");
            a.platform().openScreen(ModMenuScreen.themes(null));
        }, null));
        steps.add(clip("module-settings", 2.0,
                () -> a.platform().openScreen(ModMenuScreen.forModule(null, a.modules().get(HandChams.class))), null));
        steps.add(clip("locker", 3.0, () -> a.platform().openScreen(new CosmeticsScreen(null)), null));

        // World with HUD, armor and effects so every PvP element has something to show.
        steps.add(once(() -> {
            a.platform().openScreen(null);
            driver.createWorld("coolclient-reel");
        }));
        steps.add(new Step() {
            private int frames;

            @Override
            public boolean frame() {
                if (!a.platform().game().inWorld() || a.platform().anyScreenOpen()) {
                    frames = 0;
                    return false;
                }
                if (++frames == 1) {
                    a.platform().sendChat("/gamerule sendCommandFeedback false");
                    a.platform().sendChat("/gamerule announceAdvancements false");
                    for (String slot : new String[] {"head:diamond_helmet", "chest:diamond_chestplate",
                            "legs:diamond_leggings", "feet:diamond_boots"}) {
                        String[] s = slot.split(":");
                        a.platform().sendChat("/item replace entity @s armor." + s[0] + " with minecraft:" + s[1]);
                    }
                    a.platform().sendChat("/item replace entity @s weapon.mainhand with minecraft:diamond_sword");
                    a.platform().sendChat("/effect give @s minecraft:speed 900 1 true");
                    a.platform().sendChat("/effect give @s minecraft:strength 900 0 true");
                }
                // Long enough for chunks to load and the advancement toast to slide away.
                return frames > 720;
            }
        });
        // 5 fps: the HUD barely moves, and slow capture keeps the FPS counter honest.
        steps.add(clip("hud", 4.0, 5, () -> {
            Skybox sky = a.modules().get(Skybox.class);
            sky.style.set(SkyStyle.AURORA.displayName);
            sky.setEnabled(true);
            for (Class<? extends HudElement> c : hudSet()) {
                HudElement h = a.modules().get(c);
                if (h != null) h.setEnabled(true);
            }
            FpsHud fps = a.modules().get(FpsHud.class);
            if (fps != null) {
                fps.style.background.set(true);
                fps.style.blur.set(true);
            }
        }, null));
        steps.add(clip("hud-editor", 2.5, 5, () -> a.platform().openScreen(new HudEditorScreen(null)), null));

        // Cosmetics: a slow orbit (Freelook) with a slight zoom, HUD hidden.
        steps.add(once(() -> {
            a.platform().openScreen(null);
            a.platform().sendChat("/effect clear @s");
            a.platform().sendChat("/clear @s");
            for (Class<? extends HudElement> c : hudSet()) {
                HudElement h = a.modules().get(c);
                if (h != null) h.setEnabled(false);
            }
            a.platform().game().setHudHidden(true);
            facing = a.platform().game().yaw();
            Freelook fl = a.modules().get(Freelook.class);
            fl.mode.set("Toggle");
            fl.view.set("Back");
            fl.setEnabled(true);
            a.events().post(new Events.Key(fl.keybind().key(), true));
            Zoom zoom = a.modules().get(Zoom.class);
            zoom.mode.set("Toggle");
            zoom.factor.set(2.0);
            zoom.setEnabled(true);
            a.events().post(new Events.Key(zoom.keybind().key(), true));
            // Each scroll step divides the zoom by 1.18: 2 / 1.18^3 is about 1.2x.
            for (int i = 0; i < 3; i++) a.events().post(new Events.Scroll(-1));
        }));
        steps.add(orbit("look-hero", 5.0, 180, 540, "wings_energy", "halo_aurora", "aura_sparkle", "cape_aurora"));
        String[][] looks = {
            {"wings_dragon"},
            {"wings_angel", "halo_gold"},
            {"wings_demon", "hat_horns"},
            {"wings_butterfly", "aura_hearts"},
            {"hat_crown", "aura_orbit", "cape_gradient"},
            {"hat_witch", "aura_flame"},
            {"hat_catears", "bandana_aurora", "aura_snow"},
            {"halo_crimson", "bandana_black", "cape_void"},
        };
        for (String[] look : looks) steps.add(orbit("look-" + look[0], 2.4, 125, 235, look));
        String[] capes = {"cape_galaxy", "cape_inferno", "cape_rainbow", "cape_synthwave", "cape_matrix",
            "cape_ocean", "cape_sakura", "cape_storm", "cape_logo"};
        for (String cape : capes) steps.add(orbit(cape, 1.8, -55, 55, cape));
        steps.add(orbit("cape_founder", 4.0, -70, 70, "cape_founder"));
        for (String cape : new String[] {"cape_media", "cape_onair", "cape_premiere", "cape_owner"}) steps.add(orbit(cape, 4.0, -70, 70, cape));
        // Thanksgiving drop: the hats and the leaves from the front, then each cape from behind.
        steps.add(orbit("look-hat_turkey", 4.0, 125, 235, "hat_turkey", "aura_leaves", "cape_gobbler"));
        steps.add(orbit("look-hat_pilgrim", 4.0, 125, 235, "hat_pilgrim", "cape_harvestmoon"));
        for (String cape : new String[] {"cape_autumn", "cape_gobbler", "cape_harvestmoon"}) steps.add(orbit(cape, 4.0, -70, 70, cape));

        // Skies from a floating spectator camera; the edit pans across the wide frame.
        steps.add(once(() -> {
            Freelook fl = a.modules().get(Freelook.class);
            a.events().post(new Events.Key(fl.keybind().key(), true));
            Zoom zoom = a.modules().get(Zoom.class);
            a.events().post(new Events.Key(zoom.keybind().key(), true));
            a.platform().sendChat("/gamemode spectator");
            a.platform().sendChat("/time set 18000");
            a.platform().sendChat("/tp @s ~ ~40 ~");
        }));
        Object[][] skies = {
            {SkyStyle.AURORA, 0, -22}, {SkyStyle.SPACE, -160, -25}, {SkyStyle.NEBULA, 30, -20},
            {SkyStyle.SUNSET, -76, -8}, {SkyStyle.SYNTHWAVE, 180, -6}, {SkyStyle.COTTON_CANDY, 90, -14},
            {SkyStyle.BLOOD_MOON, 138, -20},
        };
        for (final Object[] s : skies) {
            final SkyStyle style = (SkyStyle) s[0];
            steps.add(clip("sky-" + slug(style.displayName), 3.5, () -> {
                a.modules().get(Skybox.class).style.set(style.displayName);
                a.platform().sendChat("/tp @s ~ ~ ~ " + s[1] + " " + s[2]);
            }, null));
        }

        // Before/after clips of single modules use modern commands (/damage, /item, components).
        if (!a.platform().minecraftVersion().startsWith("1.8")) features(a);
        else legacyFeatures(a);

        // Put the dev profile back the way the normal self-test expects it.
        steps.add(once(() -> {
            Freelook fl = a.modules().get(Freelook.class);
            fl.mode.set("Hold");
            fl.setEnabled(false);
            Zoom zoom = a.modules().get(Zoom.class);
            zoom.mode.set("Hold");
            zoom.factor.set(4.0);
            a.modules().get(Skybox.class).setEnabled(false);
            a.platform().game().setHudHidden(false);
        }));
    }

    /** Runs one frame of the reel; returns true when every clip is recorded. */
    boolean frame() {
        // Creating a world runs nested frames before it returns; those must not re-enter the step.
        if (busy) return false;
        busy = true;
        try {
            while (index < steps.size() && steps.get(index).frame()) index++;
        } finally {
            busy = false;
        }
        return index >= steps.size();
    }

    // ---------------------------------------------------------------- feature clips

    /** Floor block under the camera in the cleared "studio"; the camera faces south (+z) from it. */
    private int bx, by, bz;

    private static final String[] HEART = {
        "...............",
        "...............",
        "...XX.....XX...",
        "..X..X...X..X..",
        ".X....X.X....X.",
        ".X.....X.....X.",
        ".X...........X.",
        "..X....X....X..",
        "...X.......X...",
        "....X.....X....",
        ".....X...X.....",
        "......X.X......",
        ".......X.......",
        "...............",
        "...............",
    };
    private static final String[] SMILEY = {
        "....XXXXXXX....",
        "...X.......X...",
        "..X.........X..",
        ".X...........X.",
        "X...XX...XX...X",
        "X...XX...XX...X",
        "X.............X",
        "X......X......X",
        "X..X.......X..X",
        "X...X.....X...X",
        ".X...XXXXX...X.",
        "..X.........X..",
        "...X.......X...",
        "....XXXXXXX....",
        "...............",
    };
    private static final String[] CREEPER = {
        "...............",
        "...............",
        "..XXXX....XXXX.",
        "..XXXX....XXXX.",
        "..XXXX....XXXX.",
        "..XXXX....XXXX.",
        "......XXXX.....",
        "......XXXX.....",
        "....XXXXXXXX...",
        "....XXXXXXXX...",
        "....XXXXXXXX...",
        "....XXXXXXXX...",
        "....XX....XX...",
        "....XX....XX...",
        "...............",
    };

    /**
     * One module per clip pair, filmed in a cleared patch of the reel world: fire, totems, dropped items,
     * hurt cam, shield, weather, crosshairs, hit colours, outlines, view model, hand chams, waypoints,
     * nick hider, crystals, zoom, potions, armor, chat, motion blur, a full setup and fullbright.
     */
    private void features(final CoolClient a) {
        final VisualTweaks.LowFire lowFire = a.modules().get(VisualTweaks.LowFire.class);
        final CrystalPvpTweaks.TotemPopAnimation totem = a.modules().get(CrystalPvpTweaks.TotemPopAnimation.class);
        final VisualTweaks.ItemPhysics physics = a.modules().get(VisualTweaks.ItemPhysics.class);
        final VisualTweaks.HurtCam hurtCam = a.modules().get(VisualTweaks.HurtCam.class);
        final CrystalPvpTweaks.ShieldTweaks shield = a.modules().get(CrystalPvpTweaks.ShieldTweaks.class);
        final VisualTweaks.WeatherChanger weather = a.modules().get(VisualTweaks.WeatherChanger.class);
        final VisualTweaks.TimeChanger time = a.modules().get(VisualTweaks.TimeChanger.class);
        final Crosshair crosshair = a.modules().get(Crosshair.class);
        final VisualTweaks.HitColor hitColor = a.modules().get(VisualTweaks.HitColor.class);
        final BlockOutline outline = a.modules().get(BlockOutline.class);
        final ViewModel viewModel = a.modules().get(ViewModel.class);
        final HandChams chams = a.modules().get(HandChams.class);
        final UtilityModules.Waypoints waypoints = a.modules().get(UtilityModules.Waypoints.class);
        final VisualTweaks.Nametags nametags = a.modules().get(VisualTweaks.Nametags.class);
        final UtilityModules.NickHider nick = a.modules().get(UtilityModules.NickHider.class);
        final CrystalPvpTweaks.CrystalTweaks crystals = a.modules().get(CrystalPvpTweaks.CrystalTweaks.class);
        final UtilityModules.ChatTweaks chat = a.modules().get(UtilityModules.ChatTweaks.class);
        final VisualTweaks.MotionBlur blur = a.modules().get(VisualTweaks.MotionBlur.class);
        final VisualTweaks.Fullbright fullbright = a.modules().get(VisualTweaks.Fullbright.class);
        final Skybox sky = a.modules().get(Skybox.class);
        final Zoom zoom = a.modules().get(Zoom.class);
        final Freelook freelook = a.modules().get(Freelook.class);
        final PotionEffectsHud potions = a.modules().get(PotionEffectsHud.class);
        final ItemCounterHud counter = a.modules().get(ItemCounterHud.class);
        final ArmorStatusHud armor = a.modules().get(ArmorStatusHud.class);
        final DurabilityAlertsHud alerts = a.modules().get(DurabilityAlertsHud.class);
        // Its chat lines would hang around in the next clips.
        final TotemPopCounter popCounter = a.modules().get(TotemPopCounter.class);
        // These go back to their defaults afterwards; HUD elements only get their on/off state back (positions live in settings).
        final List<Module> tweaked = new ArrayList<Module>();
        for (Module m : new Module[] {lowFire, totem, physics, hurtCam, shield, weather, time, crosshair, hitColor, outline,
                viewModel, chams, waypoints, nametags, nick, crystals, chat, blur, fullbright, popCounter}) {
            if (m != null) tweaked.add(m);
        }
        final List<Module> touched = new ArrayList<Module>(tweaked);
        touched.add(sky);
        touched.add(potions);
        touched.add(counter);
        touched.add(armor);
        touched.add(alerts);
        for (Class<? extends HudElement> c : hudSet()) touched.add(a.modules().get(c));
        final Map<Module, Boolean> before = new HashMap<Module, Boolean>();

        // Back on the ground in survival (resistance V, no hunger), then clear a patch with a grass floor.
        steps.add(once(() -> {
            for (Module m : touched) {
                if (m == null) continue;
                before.put(m, m.isEnabledRaw());
                m.setEnabled(false);
            }
            for (CosmeticType t : CosmeticType.values()) a.cosmetics().unequip(t);
            a.platform().openScreen(null);
            a.platform().game().setHudHidden(false);
            cmd("/tp @s ~ ~-40 ~", "/gamemode survival", "/difficulty normal", "/gamerule doMobSpawning false",
                    "/gamerule doDaylightCycle false", "/gamerule doWeatherCycle false", "/gamerule doFireTick false",
                    "/time set 6000", "/weather clear",
                    // Earn the totem advancement and every recipe now, so their toasts are long gone by the clips.
                    "/advancement grant @s through minecraft:adventure/totem_of_undying", "/recipe give @s *");
        }));
        steps.add(pause(2.0));
        steps.add(once(() -> {
            bx = (int) Math.floor(a.platform().game().x());
            by = (int) Math.floor(a.platform().game().y());
            bz = (int) Math.floor(a.platform().game().z());
            // Failed commands print red errors into the footage ("No blocks were filled" on flat ground,
            // "No entity was found"), so clears go through barrier first and kills through execute.
            cmd("/fill " + b(-10, 0, -4) + " " + b(10, 10, 18) + " minecraft:barrier",
                    "/fill " + b(-10, 0, -4) + " " + b(10, 10, 18) + " air",
                    "/fill " + b(-10, -1, -4) + " " + b(10, -1, 18) + " minecraft:barrier",
                    "/fill " + b(-10, -1, -4) + " " + b(10, -1, 18) + " minecraft:grass_block",
                    // Sight line for the zoom clip.
                    "/fill " + b(-2, 0, 19) + " " + b(2, 10, 46) + " minecraft:barrier",
                    "/fill " + b(-2, 0, 19) + " " + b(2, 10, 46) + " air",
                    killAll("!minecraft:player"), look(0, 4));
            kit();
            safe();
        }));
        steps.add(pause(2.0));

        // Low Fire: stand in fire until burning, then put the block out (burning lasts 8 s) and let its smoke clear.
        steps.add(once(() -> cmd(look(0, 8), "/effect give @s minecraft:fire_resistance infinite 0 true")));
        for (final boolean on : new boolean[] {false, true}) {
            steps.add(once(() -> cmd("/setblock " + b(0, 0, 0) + " minecraft:fire")));
            steps.add(pause(1.3));
            steps.add(once(() -> cmd("/setblock " + b(0, 0, 0) + " air")));
            steps.add(pause(3.0));
            steps.add(clip(on ? "fx-fire-on" : "fx-fire-off", 2.6, () -> {
                lowFire.height.set(0.18); // the default 0.3 often hides the flames entirely
                lowFire.setEnabled(on);
            }, null));
        }
        steps.add(pause(6.0));

        // Totem pops: vanilla, shrunk, hidden. Resistance would stop the lethal hit, so it's off for the pop.
        final Runnable pop = () -> cmd("/damage @s 1000 minecraft:generic");
        final Runnable recover = () -> {
            cmd("/effect clear @s", "/effect give @s minecraft:instant_health 1 10 true");
            safe();
        };
        steps.add(once(() -> cmd(look(0, 4))));
        String[] pops = {"off", "small", "hidden"};
        for (final String mode : pops) {
            steps.add(clip("fx-totem-" + mode, 3.0, () -> {
                totem.hideOverlay.set(mode.equals("hidden"));
                totem.setEnabled(!mode.equals("off"));
                cmd("/item replace entity @s weapon.offhand with minecraft:totem_of_undying",
                        "/effect clear @s minecraft:resistance");
            }, new Cues(3.0).at(0.3, pop)));
            steps.add(once(recover));
            steps.add(pause(1.0));
        }

        // Item Physics: the same drop, spinning and floating, then lying flat.
        final String[] loot = {"diamond_sword", "golden_apple", "ender_pearl", "totem_of_undying", "diamond",
            "enchanted_golden_apple", "iron_ingot", "emerald"};
        final double[][] spots = {{-1.3, 2.6}, {0, 2.9}, {1.3, 2.6}, {-0.7, 3.9}, {0.7, 4.0}, {-1.8, 4.6}, {1.9, 4.5}, {0, 5.2}};
        final Runnable drop = () -> {
            for (int i = 0; i < loot.length; i++) {
                cmd("/summon minecraft:item " + e(spots[i][0], 1.2, spots[i][1]) + " {Item:{id:\"minecraft:" + loot[i]
                        + "\",count:1},PickupDelay:32767}");
            }
        };
        steps.add(once(() -> cmd(look(0, 30), killAll("minecraft:item"))));
        steps.add(clip("fx-items-off", 3.4, () -> physics.setEnabled(false), new Cues(3.4).at(0.15, drop)));
        steps.add(once(() -> cmd(killAll("minecraft:item"))));
        steps.add(clip("fx-items-on", 3.4, () -> physics.setEnabled(true), new Cues(3.4).at(0.15, drop)));
        steps.add(once(() -> cmd(killAll("minecraft:item"))));

        // Hurt Cam: the same hits with the vanilla shake, then with none.
        final Runnable hurt = () -> cmd("/damage @s 2 minecraft:generic");
        steps.add(once(() -> cmd(look(0, 4))));
        steps.add(clip("fx-hurt-off", 3.0, () -> hurtCam.setEnabled(false), new Cues(3.0).every(0.2, 0.75, hurt)));
        steps.add(clip("fx-hurt-on", 3.0, () -> {
            hurtCam.strength.set(0.0);
            hurtCam.setEnabled(true);
        }, new Cues(3.0).every(0.2, 0.75, hurt)));
        steps.add(once(() -> hurtCam.setEnabled(false)));

        // Low Shield.
        steps.add(once(() -> cmd(look(0, 4), "/item replace entity @s weapon.offhand with minecraft:shield")));
        steps.add(clip("fx-shield-off", 2.4, () -> shield.setEnabled(false), null));
        steps.add(clip("fx-shield-on", 2.4, () -> {
            shield.lower.set(0.0);
            shield.scale.set(0.7);
            shield.setEnabled(true);
        }, null));
        steps.add(once(() -> {
            shield.setEnabled(false);
            cmd("/item replace entity @s weapon.offhand with minecraft:air");
        }));

        // Clear Weather + Time Changer: rain at midnight, then client-side noon and sunset.
        steps.add(once(() -> cmd(look(0, -8), "/weather rain", "/time set 18000")));
        steps.add(pause(6.0));
        steps.add(clip("fx-weather-off", 3.0, null, null));
        steps.add(clip("fx-weather-noon", 3.0, () -> {
            weather.setEnabled(true);
            time.time.set(6000.0);
            time.setEnabled(true);
        }, null));
        steps.add(clip("fx-weather-sunset", 3.0, () -> time.time.set(12500.0), null));
        steps.add(once(() -> {
            weather.setEnabled(false);
            time.setEnabled(false);
            cmd("/weather clear", "/time set 6000");
        }));
        steps.add(pause(6.0));

        // Crosshair: draw a heart in the editor, then custom and preset crosshairs on an armored zombie.
        steps.add(once(() -> cmd(zombie(0, 6), "/effect give @e[type=minecraft:zombie] minecraft:resistance infinite 4 true",
                look(0, 3))));
        steps.add(clip("fx-crosshair-draw", 4.5, () -> {
            crosshair.preset.set("Custom");
            crosshair.grid.clear();
            crosshair.color.set(0xFFFF4FA0);
            crosshair.scale.set(1.5);
            crosshair.setEnabled(true);
            a.platform().openScreen(ModMenuScreen.forModule(null, crosshair));
        }, draw(crosshair.grid, penOrder(HEART))));
        steps.add(clip("fx-crosshair-heart", 2.0, () -> {
            a.platform().openScreen(null);
            crosshair.grid.set(art(HEART));
        }, null));
        steps.add(clip("fx-crosshair-smiley", 1.6, () -> {
            crosshair.color.set(0xFFFFD23F);
            crosshair.grid.set(art(SMILEY));
        }, null));
        steps.add(clip("fx-crosshair-creeper", 1.6, () -> {
            crosshair.color.set(0xFF4CFF6A);
            crosshair.grid.set(art(CREEPER));
        }, null));
        final String[] presets = {"Cross", "Gap Cross", "Dot", "Plus", "X", "Circle"};
        steps.add(clip("fx-crosshair-presets", 3.0, () -> crosshair.color.set(0xFFFFFFFF), k -> {
            String p = presets[Math.min(presets.length - 1, (int) (k * presets.length))];
            if (!crosshair.preset.is(p)) crosshair.preset.set(p);
        }));
        steps.add(once(() -> crosshair.setEnabled(false)));

        // Hit Color: two hits per colour on the zombie, brought in close. Clip 0 is vanilla red.
        final Runnable smack = () -> cmd("/damage @e[type=minecraft:zombie,limit=1] 1 minecraft:generic");
        steps.add(once(() -> cmd("/tp @e[type=minecraft:zombie] " + e(0, 0, 3.2) + " 180 0", look(0, 9))));
        final int[] hits = {0, 0x9900E5FF, 0x99A855FF, 0x99FFC400, 0x9933FF66, 0x99FFFFFF};
        for (int i = 0; i < hits.length; i++) {
            final int c = hits[i];
            steps.add(clip("fx-hit-" + i, 1.6, () -> {
                if (c != 0) hitColor.color.set(c);
                hitColor.setEnabled(c != 0);
            }, new Cues(1.6).at(0.15, smack).at(0.9, smack)));
        }
        steps.add(once(() -> {
            hitColor.setEnabled(false);
            cmd(voidZombies(), look(0, 38));
        }));

        // Block Outline: vanilla, then four styles on the grass block ahead.
        final Object[][] outlines = {
            {0xFF2DE2C1, 2.0, false, 0}, {0xFFFF3B3B, 4.0, false, 0}, {0xFFB06CFF, 3.0, true, 0x55B06CFF},
            {0xFFFFD23F, 6.0, true, 0x33FFD23F},
        };
        steps.add(clip("fx-outline-0", 1.6, () -> outline.setEnabled(false), null));
        for (int i = 0; i < outlines.length; i++) {
            final Object[] o = outlines[i];
            steps.add(clip("fx-outline-" + (i + 1), 1.6, () -> {
                outline.color.set((Integer) o[0]);
                outline.width.set((Double) o[1]);
                outline.fill.set((Boolean) o[2]);
                outline.fillColor.set((Integer) o[3]);
                outline.setEnabled(true);
            }, null));
        }
        steps.add(once(() -> {
            outline.setEnabled(false);
            cmd(look(0, 4));
        }));

        // View Model: x, y, z, scale, rotation x/y/z.
        final double[][] hands = {
            {0.12, -0.12, -0.25, 1, 0, 0, 0}, {0, 0, 0, 0.6, 0, 0, 0}, {0, 0, 0, 1, 0, 25, -20}, {-0.3, -0.08, 0, 1, 0, 45, 0},
        };
        steps.add(clip("fx-vm-0", 1.6, () -> viewModel.setEnabled(false), null));
        for (int i = 0; i < hands.length; i++) {
            final double[] h = hands[i];
            steps.add(clip("fx-vm-" + (i + 1), 1.6, () -> {
                viewModel.x.set(h[0]);
                viewModel.y.set(h[1]);
                viewModel.z.set(h[2]);
                viewModel.scale.set(h[3]);
                viewModel.rotX.set(h[4]);
                viewModel.rotY.set(h[5]);
                viewModel.rotZ.set(h[6]);
                viewModel.setEnabled(true);
            }, null));
        }
        steps.add(once(() -> viewModel.setEnabled(false)));

        // Hand Chams on the sword, then on the bare arm.
        final Object[][] looks = {{"Tint", 0xB38A5CFF}, {"Flat", 0xE600E5FF}, {"Glow", 0xCCFF4FD8}, {"Wireframe", 0xFF39FF88}};
        steps.add(clip("fx-chams-0", 1.6, () -> chams.setEnabled(false), null));
        for (int i = 0; i < looks.length; i++) {
            final Object[] l = looks[i];
            steps.add(clip("fx-chams-" + (i + 1), 1.6, () -> {
                chams.mode.set((String) l[0]);
                chams.target.set("Both");
                chams.armColor.set((Integer) l[1]);
                chams.itemColor.set((Integer) l[1]);
                chams.setEnabled(true);
            }, null));
        }
        steps.add(clip("fx-chams-arm", 2.0, () -> {
            chams.mode.set("Glow");
            chams.armColor.set(0xCC00E5FF);
            cmd("/item replace entity @s weapon.mainhand with minecraft:air");
        }, null));
        steps.add(once(() -> {
            chams.setEnabled(false);
            kit();
        }));

        // Waypoints: the same pan without, then with three markers.
        final List<Waypoint> marks = new ArrayList<Waypoint>();
        steps.add(clip("fx-wp-off", 3.0, null, pan(-40, 40, -3)));
        steps.add(clip("fx-wp-on", 4.0, () -> {
            marks.add(a.waypoints().add("Base", bx + 40, by + 2, bz + 160));
            marks.add(a.waypoints().add("Diamonds", bx - 60, by - 40, bz + 90));
            marks.add(a.waypoints().add("Portal", bx + 70, by, bz + 110));
            waypoints.beam.set(true);
            waypoints.distance.set(true);
            waypoints.setEnabled(true);
        }, pan(-40, 40, -3)));
        steps.add(once(() -> {
            for (Waypoint w : marks) a.waypoints().remove(w);
            waypoints.setEnabled(false);
        }));

        // Nick Hider: third person with your own nametag, chatting as yourself, then as "You".
        steps.add(once(() -> {
            nametags.showOwn.set(true);
            nametags.setEnabled(true);
            a.platform().game().setPerspective(1);
            cmd(look(0, 10));
        }));
        steps.add(clip("fx-nick-off", 3.2, null,
                new Cues(3.2).at(0.2, () -> a.platform().sendChat("gg")).at(1.4, () -> a.platform().sendChat("anyone want to 1v1?"))));
        steps.add(clip("fx-nick-on", 3.2, () -> {
            nick.nick.set("You");
            nick.setEnabled(true);
        }, new Cues(3.2).at(0.4, () -> a.platform().sendChat("gg"))));
        steps.add(once(() -> {
            nick.setEnabled(false);
            nametags.setEnabled(false);
            a.platform().game().setPerspective(0);
        }));
        steps.add(pause(10.5)); // let the chat lines fade before the next clip

        // Crystal Tweaks: two crystals on obsidian.
        steps.add(once(() -> cmd(look(0, 12), "/setblock " + b(-1, -1, 4) + " minecraft:obsidian",
                "/setblock " + b(1, -1, 4) + " minecraft:obsidian",
                "/summon minecraft:end_crystal " + e(-1, 0, 4) + " {ShowBottom:1b}",
                "/summon minecraft:end_crystal " + e(1, 0, 4) + " {ShowBottom:1b}")));
        steps.add(clip("fx-crystal-off", 3.0, () -> crystals.setEnabled(false), null));
        steps.add(clip("fx-crystal-on", 3.0, () -> {
            crystals.scale.set(0.6);
            crystals.spin.set(0.3);
            crystals.bounce.set(0.2);
            crystals.setEnabled(true);
        }, null));
        steps.add(once(() -> {
            crystals.setEnabled(false);
            cmd("/kill @e[type=minecraft:end_crystal]", "/setblock " + b(-1, -1, 4) + " minecraft:grass_block",
                    "/setblock " + b(1, -1, 4) + " minecraft:grass_block");
        }));

        // Zoom: an armored zombie on a gold pillar 40 blocks out; toggle in, scroll further, toggle out.
        final Runnable zoomKey = () -> a.events().post(new Events.Key(zoom.keybind().key(), true));
        final Runnable zoomIn = () -> a.events().post(new Events.Scroll(1));
        steps.add(once(() -> cmd("/fill " + b(0, -3, 40) + " " + b(0, 5, 40) + " minecraft:gold_block", zombie(6, 40),
                "/effect give @e[type=minecraft:zombie] minecraft:resistance infinite 4 true", look(0, -7.5f))));
        steps.add(pause(1.0));
        steps.add(clip("fx-zoom", 5.0, () -> {
            zoom.mode.set("Toggle");
            zoom.factor.set(4.0);
            zoom.setEnabled(true);
        }, new Cues(5.0).at(0.9, zoomKey).at(2.1, zoomIn).at(2.5, zoomIn).at(2.9, zoomIn).at(4.2, zoomKey)));
        steps.add(once(() -> cmd(voidZombies(), "/fill " + b(0, -3, 40) + " " + b(0, 5, 40) + " air", look(0, 4))));

        // Potion Effects + Item Counter: timers run down while pearls and pots get used.
        steps.add(once(() -> {
            cmd("/effect clear @s", "/effect give @s minecraft:speed 95 1", "/effect give @s minecraft:strength 95 1",
                    "/effect give @s minecraft:fire_resistance 455 0", "/effect give @s minecraft:regeneration 22 1",
                    "/give @s minecraft:splash_potion[potion_contents={potion:\"minecraft:strong_healing\"}] 8",
                    "/give @s minecraft:ender_pearl 16", "/give @s minecraft:golden_apple 12", "/give @s minecraft:arrow 64",
                    "/give @s minecraft:totem_of_undying 3", "/give @s minecraft:experience_bottle 32");
            potions.setEnabled(true);
            counter.setEnabled(true);
            // Looking up keeps the effect particles (around the body) out of the shot; hidden ones skip the HUD.
            cmd(look(0, -25));
        }));
        steps.add(pause(1.0));
        steps.add(clip("fx-pots", 5.0, null, new Cues(5.0)
                .every(0.5, 0.8, () -> cmd("/clear @s minecraft:ender_pearl 1"))
                .every(0.9, 1.3, () -> cmd("/clear @s minecraft:splash_potion 1"))));

        // Armor Status + Durability Alerts: worn boots cross the 10% warning after two hits.
        steps.add(once(() -> {
            potions.setEnabled(false);
            counter.setEnabled(false);
            cmd("/clear @s", "/effect clear @s",
                    "/item replace entity @s armor.head with minecraft:diamond_helmet[damage=120]",
                    "/item replace entity @s armor.chest with minecraft:diamond_chestplate[damage=160]",
                    "/item replace entity @s armor.legs with minecraft:diamond_leggings[damage=140]",
                    "/item replace entity @s armor.feet with minecraft:diamond_boots[damage=384]",
                    "/item replace entity @s weapon.mainhand with minecraft:diamond_sword");
            safe();
            hurtCam.strength.set(0.0);
            hurtCam.setEnabled(true);
            armor.setEnabled(true);
            alerts.setEnabled(true);
            cmd(look(0, 4));
        }));
        steps.add(pause(1.0));
        // Generic damage bypasses armor, so it would never wear down; mob_attack does.
        steps.add(clip("fx-armor", 5.0, null, new Cues(5.0).every(0.5, 1.0, () -> cmd("/damage @s 8 minecraft:mob_attack"))));
        steps.add(once(() -> {
            armor.setEnabled(false);
            alerts.setEnabled(false);
            hurtCam.setEnabled(false);
            kit();
        }));

        // Chat: the same spam, plain, then stacked with timestamps (after the first lines fade).
        final String[] spam = {"<Steve> gg", "<Steve> gg", "<Alex> gg wp", "<Steve> gg", "<Steve> gg", "<Blaze> rematch?",
            "<Steve> gg"};
        steps.add(pause(10.5));
        steps.add(clip("fx-chat-off", 3.6, () -> chat.setEnabled(false), spamCues(spam)));
        steps.add(pause(10.5));
        steps.add(clip("fx-chat-on", 3.6, () -> {
            chat.timestamps.set(true);
            chat.stack.set(true);
            chat.setEnabled(true);
        }, spamCues(spam)));
        steps.add(once(() -> chat.setEnabled(false)));
        steps.add(pause(10.5));

        // A full setup in first person: synthwave sky, HUD, heart crosshair, glowing hand, lowered sword.
        steps.add(clip("fx-setup-fp", 3.5, () -> {
            sky.style.set(SkyStyle.SYNTHWAVE.displayName);
            sky.setEnabled(true);
            for (Class<? extends HudElement> c : hudSet()) {
                HudElement h = a.modules().get(c);
                if (h != null) h.setEnabled(true);
            }
            crosshair.preset.set("Custom");
            crosshair.grid.set(art(HEART));
            crosshair.color.set(0xFFFF4FA0);
            crosshair.scale.set(1.0);
            crosshair.setEnabled(true);
            chams.mode.set("Glow");
            chams.target.set("Both");
            chams.itemColor.set(0xCCFF4FD8);
            chams.setEnabled(true);
            viewModel.y.set(-0.08);
            viewModel.z.set(-0.12);
            viewModel.setEnabled(true);
            cmd(look(0, 2));
        }, null));

        // Motion Blur on a fast third-person orbit, then the setup's orbit with every cosmetic.
        steps.add(once(() -> {
            a.platform().game().setHudHidden(true);
            facing = a.platform().game().yaw();
            freelook.mode.set("Toggle");
            freelook.view.set("Back");
            freelook.setEnabled(true);
            a.events().post(new Events.Key(freelook.keybind().key(), true));
        }));
        final String[] hero = {"wings_energy", "halo_aurora", "aura_sparkle", "cape_aurora"};
        if (blur != null) { // 1.8.9 only
            steps.add(orbit("fx-blur-off", 2.6, 0, 360, hero));
            steps.add(once(() -> {
                blur.strength.set(6.0);
                blur.setEnabled(true);
            }));
            steps.add(orbit("fx-blur-on", 2.6, 0, 360, hero));
            steps.add(once(() -> blur.setEnabled(false)));
        }
        steps.add(orbit("fx-setup-orbit", 4.0, 180, 540, hero));
        steps.add(once(() -> {
            a.events().post(new Events.Key(freelook.keybind().key(), true));
            for (CosmeticType t : CosmeticType.values()) a.cosmetics().unequip(t);
            a.platform().game().setHudHidden(false);
            sky.setEnabled(false);
            for (Class<? extends HudElement> c : hudSet()) {
                HudElement h = a.modules().get(c);
                if (h != null) h.setEnabled(false);
            }
            crosshair.setEnabled(false);
            chams.setEnabled(false);
            viewModel.setEnabled(false);
        }));

        // Fullbright: a sealed, lightless deepslate room built over the camera, with ore in the walls.
        steps.add(once(() -> cmd("/fill " + b(-5, -1, -3) + " " + b(5, 5, 11) + " minecraft:deepslate hollow",
                "/setblock " + b(1, 2, 11) + " minecraft:deepslate_diamond_ore",
                "/setblock " + b(2, 1, 11) + " minecraft:deepslate_diamond_ore",
                "/setblock " + b(-2, 3, 11) + " minecraft:deepslate_gold_ore",
                "/setblock " + b(-3, 1, 11) + " minecraft:deepslate_iron_ore",
                "/setblock " + b(5, 2, 6) + " minecraft:deepslate_lapis_ore",
                "/setblock " + b(-5, 2, 5) + " minecraft:deepslate_emerald_ore",
                "/setblock " + b(0, 4, 11) + " minecraft:deepslate_redstone_ore", look(0, 12))));
        steps.add(pause(2.0));
        steps.add(clip("fx-dark-off", 2.6, () -> fullbright.setEnabled(false), null));
        steps.add(clip("fx-dark-on", 2.6, () -> fullbright.setEnabled(true), null));

        // Leave the profile as it was.
        steps.add(once(() -> {
            for (Map.Entry<Module, Boolean> e : before.entrySet()) e.getKey().setEnabled(e.getValue());
            for (Module m : tweaked) {
                for (Setting<?> s : m.settings()) s.reset();
                m.setEnabled(m.defaultEnabled());
            }
            a.platform().game().setPerspective(0);
            cmd("/fill " + b(-5, -1, -3) + " " + b(5, 5, 11) + " air", "/fill " + b(-5, -1, -3) + " " + b(5, -1, 11)
                    + " minecraft:grass_block", look(0, 4));
        }));
    }

    /** 1.8.9 only: block hitting with and without 1.7 animations, and motion blur on a fast orbit. */
    private void legacyFeatures(final CoolClient a) {
        final LegacyModules.OldAnimations old = a.modules().get(LegacyModules.OldAnimations.class);
        final VisualTweaks.MotionBlur blur = a.modules().get(VisualTweaks.MotionBlur.class);
        final Skybox sky = a.modules().get(Skybox.class);
        final Freelook freelook = a.modules().get(Freelook.class);
        final boolean oldBefore = old.isEnabledRaw(), blurBefore = blur.isEnabledRaw();

        steps.add(once(() -> {
            for (CosmeticType t : CosmeticType.values()) a.cosmetics().unequip(t);
            a.platform().openScreen(null);
            a.platform().game().setHudHidden(false);
            sky.setEnabled(false);
            // 1.8 command syntax: no @s, /replaceitem instead of /item.
            cmd("/gamemode 1", "/time set 6000", "/weather clear", "/gamerule doDaylightCycle false",
                    // Hides the "Press E to open your inventory" tutorial toast.
                    "/achievement give achievement.openInventory @p",
                    "/replaceitem entity @p slot.hotbar.0 minecraft:diamond_sword",
                    "/replaceitem entity @p slot.armor.head minecraft:diamond_helmet",
                    "/replaceitem entity @p slot.armor.chest minecraft:diamond_chestplate",
                    "/replaceitem entity @p slot.armor.legs minecraft:diamond_leggings",
                    "/replaceitem entity @p slot.armor.feet minecraft:diamond_boots", "/tp @p ~ ~ ~ 0 50");
        }));
        steps.add(pause(11.0)); // the modern commands earlier in the reel left red errors in chat

        // Block hitting: hold use (block) and attack while looking at the ground. Vanilla 1.8 can't swing while
        // blocking; with 1.7 animations the sword swings mid-block.
        steps.add(clip("fx18-anim-off", 3.0, () -> {
            old.setEnabled(false);
            a.platform().game().setUseHeld(true);
        }, new Cues(3.0).at(0.05, () -> a.platform().game().setAttackHeld(true))));
        steps.add(clip("fx18-anim-on", 3.0, () -> {
            old.blockHit.set(true);
            old.swordBlock.set(true);
            old.punchWhileUsing.set(true);
            old.setEnabled(true);
        }, null));
        steps.add(once(() -> {
            a.platform().game().setAttackHeld(false);
            a.platform().game().setUseHeld(false);
            a.platform().game().setHudHidden(true);
            sky.style.set(SkyStyle.SYNTHWAVE.displayName);
            sky.setEnabled(true);
            cmd("/tp @p ~ ~ ~ 0 0");
        }));
        steps.add(pause(1.0));

        // Motion Blur: the same fast orbit around a full cosmetic loadout, off then on.
        steps.add(once(() -> {
            facing = a.platform().game().yaw();
            freelook.mode.set("Toggle");
            freelook.view.set("Back");
            freelook.setEnabled(true);
            a.events().post(new Events.Key(freelook.keybind().key(), true));
        }));
        final String[] hero = {"wings_energy", "halo_aurora", "aura_sparkle", "cape_aurora"};
        // Fast enough that the trails show at 30 fps capture.
        steps.add(orbit("fx18-blur-off", 1.8, 0, 360, hero));
        steps.add(once(() -> {
            blur.strength.set(9.0);
            blur.setEnabled(true);
        }));
        steps.add(orbit("fx18-blur-on", 1.8, 0, 360, hero));
        steps.add(once(() -> {
            a.events().post(new Events.Key(freelook.keybind().key(), true));
            for (CosmeticType t : CosmeticType.values()) a.cosmetics().unequip(t);
            for (Setting<?> s : blur.settings()) s.reset();
            blur.setEnabled(blurBefore);
            old.setEnabled(oldBefore);
            sky.setEnabled(false);
            a.platform().game().setHudHidden(false);
        }));
    }

    private void cmd(String... commands) {
        for (String c : commands) CoolClient.get().platform().sendChat(c);
    }

    /** Studio block {@code (dx, dy, dz)} from the floor block under the camera. */
    private String b(int dx, int dy, int dz) {
        return (bx + dx) + " " + (by + dy) + " " + (bz + dz);
    }

    /** Entity position, relative to the centre of the camera's block. */
    private String e(double dx, double dy, double dz) {
        return String.format(Locale.ROOT, "%.2f %.2f %.2f", bx + 0.5 + dx, by + dy, bz + 0.5 + dz);
    }

    /** Puts the camera back on its spot, looking {@code yaw}/{@code pitch} (0 = south, positive pitch = down). */
    private String look(float yaw, float pitch) {
        return "/tp @s " + e(0, 0, 0) + String.format(Locale.ROOT, " %.1f %.1f", yaw, pitch);
    }

    private void kit() {
        cmd("/item replace entity @s armor.head with minecraft:diamond_helmet",
                "/item replace entity @s armor.chest with minecraft:diamond_chestplate",
                "/item replace entity @s armor.legs with minecraft:diamond_leggings",
                "/item replace entity @s armor.feet with minecraft:diamond_boots",
                "/item replace entity @s weapon.mainhand with minecraft:diamond_sword");
    }

    /** No damage and no hunger, without effect icons. */
    private void safe() {
        cmd("/effect give @s minecraft:resistance infinite 4 true", "/effect give @s minecraft:saturation infinite 0 true");
    }

    /** A still, silent, armored zombie facing the camera, {@code dz} blocks ahead. */
    private String zombie(double dy, double dz) {
        return "/summon minecraft:zombie " + e(0, dy, dz) + " {NoAI:1b,Silent:1b,PersistenceRequired:1b,Rotation:[180f,0f],"
                + "ArmorItems:[{id:\"minecraft:diamond_boots\",count:1},{id:\"minecraft:diamond_leggings\",count:1},"
                + "{id:\"minecraft:diamond_chestplate\",count:1},{id:\"minecraft:diamond_helmet\",count:1}],"
                + "HandItems:[{id:\"minecraft:diamond_sword\",count:1},{}],ArmorDropChances:[0f,0f,0f,0f],HandDropChances:[0f,0f]}";
    }

    /** Kills every entity of a type without an error message when there are none. */
    private static String killAll(String type) {
        return "/execute as @e[type=" + type + "] run kill @s";
    }

    /** Drops zombies into the void, out of sight (killing them would show the death animation). */
    private String voidZombies() {
        return "/tp @e[type=minecraft:zombie] " + bx + " -200 " + bz;
    }

    private Cues spamCues(String[] lines) {
        Cues c = new Cues(3.6);
        for (int i = 0; i < lines.length; i++) {
            final String line = lines[i];
            c.at(0.2 + i * 0.45, () -> cmd("/tellraw @s \"" + line + "\""));
        }
        return c;
    }

    /** Pans the first-person camera between two yaws (server teleports, so it moves at tick rate). */
    private DoubleConsumer pan(final float from, final float to, final float pitch) {
        return new DoubleConsumer() {
            private float last = Float.NaN;

            @Override
            public void accept(double k) {
                float yaw = (float) (from + (to - from) * (k * k * (3 - 2 * k)));
                if (Math.abs(yaw - last) < 0.25f) return;
                last = yaw;
                cmd(look(yaw, pitch));
            }
        };
    }

    private static String art(String[] rows) {
        StringBuilder sb = new StringBuilder();
        for (String row : rows) for (int x = 0; x < row.length(); x++) sb.append(row.charAt(x) == 'X' ? '1' : '0');
        return sb.toString();
    }

    /** The lit pixels in pen order: clockwise from straight above the centre, the centre itself last. */
    private static List<int[]> penOrder(String[] rows) {
        List<int[]> px = new ArrayList<int[]>();
        for (int y = 0; y < rows.length; y++)
            for (int x = 0; x < rows[y].length(); x++)
                if (rows[y].charAt(x) == 'X') px.add(new int[] {x, y});
        final double c = (rows.length - 1) / 2.0;
        Collections.sort(px, (p, q) -> Double.compare(penAngle(p, c), penAngle(q, c)));
        return px;
    }

    private static double penAngle(int[] p, double c) {
        double dx = p[0] - c, dy = p[1] - c;
        if (dx == 0 && dy == 0) return 10;
        double angle = Math.atan2(dx, -dy);
        return angle < 0 ? angle + 2 * Math.PI : angle;
    }

    /** Reveals {@code pen} pixel by pixel over the first 80% of the clip. */
    private static DoubleConsumer draw(final GridSetting grid, final List<int[]> pen) {
        return new DoubleConsumer() {
            private int shown = -1;

            @Override
            public void accept(double k) {
                int n = (int) Math.round(Math.min(1, k * 1.25) * pen.size());
                if (n == shown) return;
                shown = n;
                char[] c = new char[grid.size() * grid.size()];
                Arrays.fill(c, '0');
                for (int i = 0; i < n; i++) c[pen.get(i)[1] * grid.size() + pen.get(i)[0]] = '1';
                grid.set(new String(c));
            }
        };
    }

    private static Step pause(final double seconds) {
        return new Step() {
            private long until;

            @Override
            public boolean frame() {
                long now = System.nanoTime();
                if (until == 0) until = now + (long) (seconds * 1e9);
                return now >= until;
            }
        };
    }

    /** Runs each action once, when the clip reaches its time (seconds into the recording). */
    private static final class Cues implements DoubleConsumer {
        private final double length;
        private final List<Double> times = new ArrayList<Double>();
        private final List<Runnable> actions = new ArrayList<Runnable>();
        private final List<Boolean> done = new ArrayList<Boolean>();

        Cues(double length) {
            this.length = length;
        }

        Cues at(double t, Runnable r) {
            times.add(t);
            actions.add(r);
            done.add(false);
            return this;
        }

        Cues every(double from, double period, Runnable r) {
            for (double t = from; t < length; t += period) at(t, r);
            return this;
        }

        @Override
        public void accept(double progress) {
            if (progress <= 0) return;
            double t = progress * length;
            for (int i = 0; i < times.size(); i++) {
                if (!done.get(i) && times.get(i) <= t) {
                    done.set(i, true);
                    actions.get(i).run();
                }
            }
        }
    }

    private static List<Class<? extends HudElement>> hudSet() {
        List<Class<? extends HudElement>> out = new ArrayList<Class<? extends HudElement>>();
        out.add(FpsHud.class);
        out.add(CpsHud.class);
        out.add(PingHud.class);
        out.add(CoordinatesHud.class);
        out.add(ReachDisplayHud.class);
        out.add(ComboCounterHud.class);
        out.add(KeystrokesHud.class);
        out.add(ArmorStatusHud.class);
        out.add(PotionEffectsHud.class);
        out.add(DirectionHud.class);
        out.add(CpsGraphHud.class);
        return out;
    }

    private static String slug(String name) {
        return name.toLowerCase(Locale.ROOT).replace(' ', '-');
    }

    private static Step once(final Runnable r) {
        return () -> {
            r.run();
            return true;
        };
    }

    private Step clip(String name, double seconds, Runnable setup, DoubleConsumer during) {
        return clip(name, seconds, 30, setup, during);
    }

    /** Sets up, waits for transitions to settle, then saves {@code fps} frames a second for {@code seconds}. */
    private Step clip(final String name, final double seconds, final int fps, final Runnable setup,
            final DoubleConsumer during) {
        final long interval = 1_000_000_000L / fps;
        final boolean record = wanted(name);
        return new Step() {
            private long setupAt, start, next;

            @Override
            public boolean frame() {
                long now = System.nanoTime();
                if (setupAt == 0) {
                    setupAt = now;
                    if (setup != null) setup.run();
                    if (during != null) during.accept(0);
                    return !record;
                }
                if (start == 0) {
                    if (during != null) during.accept(0);
                    if (now - setupAt < SETTLE * 1e9) return false;
                    start = now;
                    next = now;
                    Log.info("Reel clip: " + name);
                }
                double t = (now - start) / 1e9;
                if (during != null) during.accept(Math.min(1, t / seconds));
                if (t > seconds) return true;
                if (now >= next) {
                    driver.screenshot(String.format(Locale.ROOT, "reel-%s-%05d", name, (now - start) / 1_000_000));
                    next = Math.max(next + interval, now - interval / 2);
                }
                return false;
            }
        };
    }

    /** {@code -Dcoolclient.selftest.reel.long=true}: cosmetic orbits as slow full turns, saved as {@code long-*} clips. */
    private static final boolean LONG = Boolean.getBoolean("coolclient.selftest.reel.long");

    /** Wears exactly {@code ids} while the camera swings from {@code from} to {@code to} degrees around the player (0 = behind). */
    private Step orbit(String name, double seconds, final float from, float to, final String... ids) {
        final boolean slow = LONG && name.startsWith("look-");
        final float end = slow ? from + 360 : to;
        final CoolClient a = CoolClient.get();
        return clip(slow ? "long-" + name : name, slow ? 8.0 : seconds, () -> {
            for (CosmeticType t : CosmeticType.values()) a.cosmetics().unequip(t);
            for (String id : ids) a.cosmetics().equip(CosmeticCatalog.get(id));
        }, t -> {
            Freelook fl = a.modules().get(Freelook.class);
            // Slow turns go at an even speed so any stretch of the clip works in the edit.
            double k = slow ? t : t * t * (3 - 2 * t);
            float yaw = facing + (float) (from + (end - from) * k);
            // turn() takes mouse units (0.15 degrees each); pitch 12 looks slightly down at the player.
            fl.turn((yaw - fl.yaw()) / 0.15f, (12f - fl.pitch()) / 0.15f);
        });
    }
}
