package dev.coolclient.launcher;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.gui.UIScreen;
import dev.coolclient.core.gui.screen.CosmeticsScreen;
import dev.coolclient.core.render.Scissor;
import dev.coolclient.core.util.Log;
import dev.coolclient.core.util.Scheduler;
import dev.coolclient.launcher.core.Accounts;
import dev.coolclient.launcher.core.LauncherInfo;
import dev.coolclient.launcher.core.LauncherPaths;
import dev.coolclient.launcher.gl.GlRenderer;
import dev.coolclient.launcher.gl.GlTextures;
import dev.coolclient.launcher.gl.GlWindow;
import org.lwjgl.system.MemoryUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33C.*;

/** The windowed launcher (GLFW + OpenGL, drawn with the client's UI toolkit). */
final class LauncherApp {
    /** Window actions the title bar needs. */
    interface WindowControls {
        void close();

        void minimize();

        void beginDrag();
    }

    private static final int LOGICAL_W = 1120, LOGICAL_H = 700;
    /** GUI units per logical pixel; 1120x700 becomes a 640x400 layout. */
    private static final float UI_SCALE = 1.75f;

    private GlWindow window;
    private LauncherPlatform platform;
    private LauncherScreen root;
    private float scale = 1f;
    private float mouseX, mouseY;

    private LauncherApp() {}

    static void run() {
        new LauncherApp().loop();
    }

    private UIScreen screen() { return platform.currentScreen(); }

    private void loop() {
        LauncherPaths.BASE.mkdirs();
        // The launcher and every game version share one Cool Client data folder (cosmetics, settings).
        System.setProperty("coolclient.dataDir", LauncherPaths.CLIENT_DATA.getAbsolutePath());
        LauncherController controller = new LauncherController();

        window = new GlWindow(LauncherInfo.NAME, LOGICAL_W, LOGICAL_H, new GlWindow.Listener() {
            @Override
            public void cursor(double x, double y) {
                mouseX = (float) (window.cursorX() / scale);
                mouseY = (float) (window.cursorY() / scale);
            }

            @Override
            public void mouse(int button, boolean pressed, int mods) {
                UIScreen s = screen();
                if (s == null) return;
                if (pressed) {
                    if (root.titleBarPress(mouseX, mouseY, button)) return;
                    s.mouseClicked(mouseX, mouseY, button);
                } else {
                    s.mouseReleased(mouseX, mouseY, button);
                }
            }

            @Override
            public void scroll(double dy) {
                UIScreen s = screen();
                if (s != null) s.mouseScrolled(mouseX, mouseY, dy);
            }

            @Override
            public void key(int key, int action, int mods) {
                UIScreen s = screen();
                if (s != null && action != GLFW_RELEASE) s.keyPressed(key, mods);
            }

            @Override
            public void character(int codepoint) {
                UIScreen s = screen();
                if (s != null && Character.isBmpCodePoint(codepoint)) s.charTyped((char) codepoint);
            }
        });
        scale = UI_SCALE * window.contentScale;

        GlTextures textures = new GlTextures();
        GlRenderer renderer = new GlRenderer(textures);
        LauncherGame game = new LauncherGame(controller.accounts);
        platform = new LauncherPlatform(window, textures, game);
        platform.guiWidth = window.framebufferWidth() / scale;
        platform.guiHeight = window.framebufferHeight() / scale;
        CoolClient.init(platform);

        root = new LauncherScreen(controller, new WindowControls() {
            @Override public void close() { window.requestClose(); }

            @Override public void minimize() { window.minimize(); }

            @Override public void beginDrag() { window.beginDrag(); }
        });
        platform.root = root;
        platform.openScreen(root);
        controller.setWindow(new LauncherController.WindowActions() {
            @Override public void minimize() { window.minimize(); }

            @Override public void hide() { window.hide(); }

            @Override public void show() { window.show(); }
        });
        window.show();
        Log.info(LauncherInfo.NAME + " " + LauncherInfo.VERSION + " started (data in " + LauncherPaths.BASE + ")");

        SelfTest selfTest = SelfTest.create();
        long lastTick = System.nanoTime(), fpsTimer = System.nanoTime();
        int frames = 0;
        while (!window.shouldClose()) {
            glfwPollEvents();
            long now = System.nanoTime();
            while (now - lastTick >= 50_000_000L) {
                lastTick += 50_000_000L;
                Scheduler.runMainTasks();
                CoolClient.get().config().tick();
                CoolClient.get().cosmetics().tick();
                if (screen() != null) screen().tick();
            }
            controller.drainMainTasks();
            if (!window.visible() || window.iconified()) {
                sleep(50);
                continue;
            }
            CoolClient.get().onFrame();

            int fbW = window.framebufferWidth(), fbH = window.framebufferHeight();
            glClearColor(0.027f, 0.039f, 0.071f, 1f);
            glClear(GL_COLOR_BUFFER_BIT);
            renderer.beginFrame(fbW, fbH, scale);
            UIScreen s = screen();
            if (s != root) {
                // Sub-screens (the cosmetics locker) draw over the launcher like a dialog.
                Scissor.reset(renderer);
                root.render(renderer, -1, -1, 0);
            }
            if (s != null) CoolClient.get().renderScreen(s, renderer, mouseX, mouseY, 0);
            renderer.endFrame();
            if (selfTest != null && selfTest.frame(root, platform, controller, fbW, fbH)) window.requestClose();
            window.swap();

            frames++;
            if (now - fpsTimer >= 1_000_000_000L) {
                game.fps = frames;
                frames = 0;
                fpsTimer = now;
            }
        }
        CoolClient.get().onShutdown();
        window.destroy();
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Development tour (-Dcoolclient.launcher.selftest[=dir]): screenshots every view, then quits. */
    private static final class SelfTest {
        private final File dir;
        /** With -Dcoolclient.launcher.selftest.play=<version>, also presses Play and waits for the game. */
        private final String playVersion = System.getProperty("coolclient.launcher.selftest.play");
        private int frame, step;
        private Accounts.Account tempAccount;
        private String prevVersion;
        private boolean shotLaunching, sawRunning;
        private int playFrames;

        private SelfTest(File dir) {
            this.dir = dir;
        }

        static SelfTest create() {
            String v = System.getProperty("coolclient.launcher.selftest");
            if (v == null) return null;
            File dir = new File(v.isEmpty() ? "selftest-screenshots" : v);
            dir.mkdirs();
            Log.info("Launcher self-test: screenshots go to " + dir.getAbsolutePath());
            return new SelfTest(dir);
        }

        /** Returns true when finished. */
        boolean frame(LauncherScreen root, LauncherPlatform platform, LauncherController c, int w, int h) {
            if (step == 5) return playFrame(root, c, w, h);
            if (++frame < 75) return false;
            frame = 0;
            switch (step++) {
                case 0:
                    shot("01-play", w, h);
                    root.setTab(LauncherScreen.Tab.SETTINGS);
                    return false;
                case 1:
                    shot("02-settings", w, h);
                    c.appendLog("[Launcher] Self-test log line");
                    c.appendLog("[12:00:00] [Render thread/INFO] (CoolClient) Example game output");
                    c.appendLog("[12:00:01] [Render thread/WARN] (Minecraft) Example warning");
                    root.setTab(LauncherScreen.Tab.CONSOLE);
                    return false;
                case 2:
                    shot("03-console", w, h);
                    root.setTab(LauncherScreen.Tab.PLAY);
                    platform.openScreen(new CosmeticsScreen(root));
                    return false;
                case 3:
                    shot("04-cosmetics", w, h);
                    platform.openScreen(null);
                    root.openAccounts(true);
                    return false;
                case 4:
                    shot("05-accounts", w, h);
                    root.openAccounts(false);
                    if (playVersion == null) {
                        Log.info("Launcher self-test complete");
                        return true;
                    }
                    if (c.accounts.selected() == null) tempAccount = c.accounts.put(Accounts.offline("CoolDev"));
                    prevVersion = c.settings.version;
                    c.settings.version = playVersion;
                    System.setProperty("coolclient.launcher.gameSelfTest", "true");
                    c.play(null);
                    return false;
                default:
                    return true;
            }
        }

        /** Waits for the game started by Play to run its own self-test and exit. */
        private boolean playFrame(LauncherScreen root, LauncherController c, int w, int h) {
            playFrames++;
            if (c.launching && !shotLaunching && playFrames > 3) {
                shot("06-launching", w, h);
                shotLaunching = true;
            }
            // The launcher is minimized while the game runs, so judge by the recorded exit instead.
            if (c.lastExitCode != null) sawRunning = true;
            boolean finished = sawRunning && !c.running() && !c.launching;
            if (!finished && !(playFrames > 600 && !c.launching && !c.running())) return false;
            if (frame++ == 0) {
                root.setTab(LauncherScreen.Tab.CONSOLE);
                return false;
            }
            if (frame < 60) return false;
            shot("07-console-after-game", w, h);
            c.settings.version = prevVersion;
            if (tempAccount != null) c.accounts.remove(tempAccount);
            Log.info(sawRunning ? "Launcher self-test complete (game exited with code " + c.lastExitCode + ")" : "Launcher self-test: the game never started");
            return true;
        }

        private void shot(String name, int w, int h) {
            ByteBuffer buf = MemoryUtil.memAlloc(w * h * 4);
            try {
                glReadPixels(0, 0, w, h, GL_RGBA, GL_UNSIGNED_BYTE, buf);
                BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int i = ((h - 1 - y) * w + x) * 4;
                        img.setRGB(x, y, ((buf.get(i) & 0xFF) << 16) | ((buf.get(i + 1) & 0xFF) << 8) | (buf.get(i + 2) & 0xFF));
                    }
                }
                ImageIO.write(img, "png", new File(dir, "launcher-" + name + ".png"));
                Log.info("Launcher self-test screenshot: " + name);
            } catch (Exception e) {
                Log.error("Screenshot failed", e);
            } finally {
                MemoryUtil.memFree(buf);
            }
        }
    }
}
