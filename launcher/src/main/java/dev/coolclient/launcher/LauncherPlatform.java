package dev.coolclient.launcher;

import dev.coolclient.core.api.Feature;
import dev.coolclient.core.api.GameState;
import dev.coolclient.core.api.Input;
import dev.coolclient.core.api.Platform;
import dev.coolclient.core.api.Textures;
import dev.coolclient.core.api.VanillaScreen;
import dev.coolclient.core.gui.UIScreen;
import dev.coolclient.core.util.Log;
import dev.coolclient.launcher.core.LauncherPaths;
import dev.coolclient.launcher.gl.GlTextures;
import dev.coolclient.launcher.gl.GlWindow;

import java.io.File;

import static org.lwjgl.glfw.GLFW.*;

/** Hosts the client's UI toolkit outside of Minecraft. */
final class LauncherPlatform implements Platform {
    private final GlWindow window;
    private final GlTextures textures;
    private final LauncherGame game;
    private final Input input;
    UIScreen root;
    private UIScreen current;
    float guiWidth, guiHeight;

    LauncherPlatform(GlWindow window, GlTextures textures, LauncherGame game) {
        this.window = window;
        this.textures = textures;
        this.game = game;
        this.input = new Input() {
            @Override
            public boolean isKeyDown(int key) { return glfwGetKey(window.handle, key) == GLFW_PRESS; }

            @Override
            public boolean isMouseDown(int button) { return glfwGetMouseButton(window.handle, button) == GLFW_PRESS; }
        };
    }

    @Override public String minecraftVersion() { return "Launcher"; }

    @Override public boolean supports(Feature feature) { return false; }

    @Override public File gameDir() { return LauncherPaths.BASE; }

    @Override public GameState game() { return game; }

    @Override public Textures textures() { return textures; }

    @Override public Input input() { return input; }

    /** Opening "no screen" returns to the launcher's main screen. */
    @Override
    public void openScreen(UIScreen screen) {
        if (screen == null) screen = root;
        if (current != null && current != screen) current.onClose();
        current = screen;
        if (screen != null) screen.init(guiWidth, guiHeight);
    }

    @Override public UIScreen currentScreen() { return current; }

    @Override public boolean anyScreenOpen() { return true; }

    @Override public void openVanilla(VanillaScreen screen, UIScreen parent) {}

    @Override public void quit() { window.requestClose(); }

    @Override
    public String clipboard() {
        String s = glfwGetClipboardString(window.handle);
        return s == null ? "" : s;
    }

    @Override public void setClipboard(String text) { glfwSetClipboardString(window.handle, text); }

    @Override
    public void openUrl(String url) {
        if (url == null || !url.matches("https?://.+")) return;
        open(url);
    }

    @Override
    public void openFolder(File folder) {
        folder.mkdirs();
        open(folder.getAbsolutePath());
    }

    private static void open(String target) {
        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (os.contains("win")) new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", target).start();
            else if (os.contains("mac")) new ProcessBuilder("open", target).start();
            else new ProcessBuilder("xdg-open", target).start();
        } catch (Exception e) {
            Log.error("Could not open " + target, e);
        }
    }

    @Override public void sendChat(String message) {}

    @Override public void showChat(String message) {}

    @Override public boolean joinSession(String serverId) { return false; }

    @Override public void playClickSound() {}
}
