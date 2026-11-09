package dev.coolclient.core;

import dev.coolclient.core.cosmetic.CosmeticCatalog;
import dev.coolclient.core.gui.screen.CosmeticsScreen;
import dev.coolclient.core.gui.screen.HudEditorScreen;
import dev.coolclient.core.gui.screen.MainMenuScreen;
import dev.coolclient.core.gui.screen.ModMenuScreen;
import dev.coolclient.core.gui.screen.WelcomeScreen;
import dev.coolclient.core.modules.hud.FpsHud;
import dev.coolclient.core.modules.visual.HandChams;
import dev.coolclient.core.modules.visual.Skybox;
import dev.coolclient.core.render.gpu.SkyStyle;
import dev.coolclient.core.util.Log;

/**
 * Development smoke test, enabled with {@code -Dcoolclient.selftest=true}. Walks through every major
 * screen, joins a fresh creative world, and saves a screenshot at each step, then quits. Lets the
 * client be verified visually without a human at the keyboard.
 */
public final class SelfTest {
    /** Version-specific actions. */
    public interface Driver {
        void screenshot(String name);

        /** Creates and joins a new creative superflat world. */
        void createWorld(String name);

        /** Loads every mixin target now, so a broken hook fails the test even if the test never reaches it. */
        default void auditMixins() {}
    }

    private static Driver driver;
    private static int step;
    private static int frames;
    private static SelfTestReel reel;

    private SelfTest() {}

    public static boolean enabled() {
        return Boolean.getBoolean("coolclient.selftest");
    }

    public static void install(Driver d) {
        if (!enabled()) return;
        driver = d;
        Log.info("Self-test mode enabled");
    }

    private static void shot(String name) {
        Log.info("Self-test screenshot: " + name);
        driver.screenshot("coolclient-selftest-" + name);
    }

    /** Turns the camera (MC yaw: 0 = south, -90 = east; negative pitch looks up). */
    private static void look(CoolClient a, int yaw, int pitch) {
        a.platform().sendChat("/tp @s ~ ~ ~ " + yaw + " " + pitch);
    }

    private static void next() {
        step++;
        frames = 0;
    }

    /** Called at the start of every frame (the framebuffer still holds the previous frame). */
    public static void frame() {
        if (driver == null) return;
        frames++;
        CoolClient a = CoolClient.get();
        try {
            switch (step) {
                case 0:
                    if (a.platform().isLoading()) frames = 0;
                    if (a.platform().currentScreen() instanceof WelcomeScreen && frames > 90) {
                        shot("00-welcome");
                        ((WelcomeScreen) a.platform().currentScreen()).accept();
                        frames = 0;
                        break;
                    }
                    if (a.platform().currentScreen() instanceof MainMenuScreen && frames > 120) {
                        driver.auditMixins();
                        Log.info("Self-test: every mixin applied");
                        a.cosmetics().equip(CosmeticCatalog.get("cape_aurora"));
                        a.cosmetics().equip(CosmeticCatalog.get("wings_energy"));
                        a.cosmetics().equip(CosmeticCatalog.get("halo_aurora"));
                        a.cosmetics().equip(CosmeticCatalog.get("aura_sparkle"));
                        HandChams hc = a.modules().get(HandChams.class);
                        if (hc != null) hc.setEnabled(true);
                        if (SelfTestReel.enabled()) {
                            reel = new SelfTestReel(driver);
                            step = 200;
                            break;
                        }
                        shot("01-main-menu");
                        next();
                        a.platform().openScreen(new ModMenuScreen(null));
                    }
                    break;
                case 1:
                    if (frames > 60) {
                        shot("02-mod-menu");
                        // The other layouts: Tabs and Compact mod menus, then the Showcase title screen.
                        step = 102;
                        frames = 0;
                        a.client().menuLayout.set("Tabs");
                        a.platform().openScreen(new ModMenuScreen(null));
                    }
                    break;
                case 102:
                    if (frames > 60) {
                        shot("02c-mod-menu-tabs");
                        step = 103;
                        frames = 0;
                        a.client().menuLayout.set("Compact");
                        a.platform().openScreen(new ModMenuScreen(null));
                    }
                    break;
                case 103:
                    if (frames > 60) {
                        shot("02d-mod-menu-compact");
                        // Newer modules (long names, many settings) in each layout.
                        step = 120;
                        frames = 0;
                        a.client().menuLayout.set("Sidebar");
                        a.platform().openScreen(ModMenuScreen.searching(null, "info"));
                    }
                    break;
                case 120:
                case 121:
                case 122:
                    if (frames > 60) {
                        String[] layouts = {"sidebar", "tabs", "compact"};
                        shot("02e-new-mods-" + layouts[step - 120]);
                        step++;
                        frames = 0;
                        if (step <= 122) {
                            a.client().menuLayout.set(step == 121 ? "Tabs" : "Compact");
                            a.platform().openScreen(ModMenuScreen.searching(null, "info"));
                        } else {
                            a.client().menuLayout.set("Tabs");
                            a.platform().openScreen(ModMenuScreen.forModule(null,
                                    a.modules().get(dev.coolclient.core.modules.utility.UtilityTweaks.Sounds.class)));
                        }
                    }
                    break;
                case 123:
                case 124:
                    if (frames > 60) {
                        shot("02f-new-settings-" + (step == 123 ? "tabs" : "compact"));
                        step++;
                        frames = 0;
                        if (step == 124) {
                            a.client().menuLayout.set("Compact");
                            a.platform().openScreen(ModMenuScreen.forModule(null,
                                    a.modules().get(dev.coolclient.core.modules.hud.TargetHud.class)));
                        } else {
                            step = 104;
                            a.client().menuLayout.set("Sidebar");
                            a.client().mainMenuLayout.set("Showcase");
                            a.platform().openScreen(new MainMenuScreen());
                        }
                    }
                    break;
                case 104:
                    if (frames > 90) {
                        shot("01b-main-menu-showcase");
                        a.client().showcaseSpotlight.set("Your Skin");
                        step = 105;
                        frames = 0;
                    }
                    break;
                case 105:
                    if (frames > 90) {
                        shot("01c-main-menu-skin");
                        a.client().showcaseSpotlight.set("New Drops");
                        a.client().mainMenuLayout.set("Centered");
                        a.cosmetics().equip(CosmeticCatalog.get("hat_turkey"));
                        step = 106;
                        frames = 0;
                        a.platform().openScreen(dev.coolclient.core.gui.screen.CosmeticsScreen.on(null, dev.coolclient.core.cosmetic.CosmeticType.HAT));
                    }
                    break;
                case 106:
                    if (frames > 60) {
                        shot("04b-cosmetics-hats");
                        step = 107;
                        frames = 0;
                        a.platform().openScreen(dev.coolclient.core.gui.screen.CosmeticsScreen.on(null, dev.coolclient.core.cosmetic.CosmeticType.WINGS));
                    }
                    break;
                case 107:
                    if (frames > 60) {
                        shot("04c-cosmetics-wings");
                        a.cosmetics().unequip(dev.coolclient.core.cosmetic.CosmeticType.HAT);
                        dev.coolclient.core.gui.screen.CosmeticsScreen.on(null, dev.coolclient.core.cosmetic.CosmeticType.CAPE);
                        step = 101;
                        frames = 0;
                        a.platform().openScreen(ModMenuScreen.themes(null));
                    }
                    break;
                case 101:
                    if (frames > 60) {
                        shot("02b-themes");
                        step = 2;
                        frames = 0;
                        a.platform().openScreen(ModMenuScreen.forModule(null, a.modules().get(HandChams.class)));
                    }
                    break;
                case 2:
                    if (frames > 60) {
                        shot("03-module-settings");
                        next();
                        a.platform().openScreen(new CosmeticsScreen(null));
                    }
                    break;
                case 3:
                    if (frames > 60) {
                        shot("04-cosmetics");
                        next();
                        a.platform().openScreen(null);
                        driver.createWorld("coolclient-selftest");
                    }
                    break;
                case 4:
                    if (a.platform().game().inWorld() && !a.platform().anyScreenOpen()) {
                        if (frames == 1) {
                            Skybox sky = a.modules().get(Skybox.class);
                            sky.style.set(SkyStyle.AURORA.displayName);
                            sky.setEnabled(true);
                            FpsHud fps = a.modules().get(FpsHud.class);
                            if (fps != null) {
                                fps.setEnabled(true);
                                fps.style.background.set(true);
                                fps.style.blur.set(true);
                            }
                        }
                        if (frames > 400) {
                            shot("05-ingame-hud");
                            next();
                            a.platform().game().setPerspective(2);
                        }
                    } else {
                        frames = 0;
                    }
                    break;
                case 5:
                    if (frames > 80) {
                        shot("06-cosmetics-front");
                        next();
                        a.platform().game().setPerspective(1);
                    }
                    break;
                case 6:
                    if (frames > 80) {
                        shot("07-cosmetics-back");
                        next();
                        a.platform().game().setPerspective(0);
                        a.platform().openScreen(new HudEditorScreen(null));
                    }
                    break;
                case 7:
                    if (frames > 60) {
                        shot("08-hud-editor");
                        next();
                        a.platform().openScreen(new ModMenuScreen(null));
                    }
                    break;
                case 8:
                    if (frames > 60) {
                        shot("09-mod-menu-ingame");
                        next();
                        a.client().theme.set("Synthwave");
                        a.modules().get(Skybox.class).style.set(SkyStyle.SYNTHWAVE.displayName);
                        a.platform().openScreen(ModMenuScreen.themes(null));
                    }
                    break;
                case 9:
                    if (frames > 60) {
                        shot("10-themes-ingame-synthwave");
                        next();
                        a.platform().openScreen(null);
                        a.modules().get(Skybox.class).style.set(SkyStyle.NEBULA.displayName);
                        // Clean sky shots: no HUD or chat, camera aimed at each sky's centrepiece.
                        a.platform().sendChat("/gamerule sendCommandFeedback false");
                        a.platform().game().setHudHidden(true);
                        // A floating spectator camera above the terrain for aerial sky shots.
                        a.platform().sendChat("/gamemode spectator");
                        a.platform().sendChat("/time set 18000");
                        a.platform().sendChat("/tp @s ~ ~40 ~");
                        look(a, 30, -20);
                    }
                    break;
                case 10:
                    if (frames > 60) {
                        shot("11-sky-nebula");
                        next();
                        a.modules().get(Skybox.class).style.set(SkyStyle.SUNSET.displayName);
                        look(a, -76, -8);
                    }
                    break;
                case 11:
                    if (frames > 60) {
                        shot("12-sky-sunset");
                        next();
                        a.modules().get(Skybox.class).style.set(SkyStyle.SYNTHWAVE.displayName);
                        look(a, 180, -6);
                    }
                    break;
                case 12:
                    if (frames > 60) {
                        shot("12b-sky-synthwave");
                        next();
                        a.modules().get(Skybox.class).style.set(SkyStyle.BLOOD_MOON.displayName);
                        look(a, 138, -20);
                    }
                    break;
                case 13:
                    if (frames > 60) {
                        shot("12c-sky-blood-moon");
                        next();
                        a.modules().get(Skybox.class).style.set(SkyStyle.SPACE.displayName);
                        look(a, -160, -25);
                    }
                    break;
                case 14:
                    if (frames > 60) {
                        shot("12d-sky-deep-space");
                        step = 114;
                        frames = 0;
                        a.modules().get(Skybox.class).style.set(SkyStyle.AURORA.displayName);
                        look(a, 0, -22);
                    }
                    break;
                case 114:
                    if (frames > 60) {
                        shot("12e-sky-aurora");
                        step = 15;
                        frames = 0;
                        a.platform().game().setHudHidden(false);
                        a.client().theme.set("Snow");
                        a.platform().openScreen(new ModMenuScreen(null));
                    }
                    break;
                case 15:
                    if (frames > 60) {
                        shot("13-theme-snow");
                        next();
                        a.client().theme.set("Aurora");
                        a.modules().get(Skybox.class).setEnabled(false);
                        a.platform().openScreen(null);
                    }
                    break;
                case 200:
                    if (reel.frame()) {
                        step = 16;
                        frames = 0;
                    }
                    break;
                case 16:
                    if (frames > 20) {
                        Log.info("Self-test complete");
                        driver = null;
                        a.platform().quit();
                    }
                    break;
                default:
                    break;
            }
        } catch (Throwable t) {
            Log.error("Self-test failed at step " + step, t);
            driver = null;
            a.platform().quit();
        }
    }
}
