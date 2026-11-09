package dev.coolclient.core.api;

import dev.coolclient.core.gui.UIScreen;

import java.io.File;

/** Everything core needs from a specific Minecraft version. */
public interface Platform {
    /** e.g. "1.8.9" or "1.21.4". */
    String minecraftVersion();

    boolean supports(Feature feature);

    /** The .minecraft-style game directory of this instance. */
    File gameDir();

    GameState game();

    Textures textures();

    Input input();

    /** Shows a core screen (wrapped in a platform screen), or closes the current one when null. */
    void openScreen(UIScreen screen);

    /** The core screen currently shown, or null. */
    UIScreen currentScreen();

    /** True when any screen (vanilla or core) is open. */
    boolean anyScreenOpen();

    void openVanilla(VanillaScreen screen, UIScreen parent);

    void quit();

    String clipboard();

    void setClipboard(String text);

    void openUrl(String url);

    void openFolder(File folder);

    /** Sends a chat message (or command, if it starts with '/') to the server. */
    void sendChat(String message);

    /** Shows a client-side chat line. Supports section-sign colour codes. */
    void showChat(String message);

    /**
     * Performs the Mojang session "join" for the current account with the given server id, so
     * the Cool Client API can verify account ownership. Returns false for offline accounts.
     */
    boolean joinSession(String serverId);

    void playClickSound();

    /** Borderless windowed fullscreen (only on platforms that support {@link Feature#BORDERLESS}). */
    default void setBorderless(boolean borderless) {}

    /** True while a resource (re)load overlay covers the screen. */
    default boolean isLoading() { return false; }

    /** Pause/resume animated block textures (platforms with {@link Feature#TEXTURE_ANIMATIONS}). */
    default void setTextureAnimations(boolean enabled) {}

    /** The graphics card as the driver names it (GL_RENDERER), or null. */
    default String gpuName() { return null; }

    /** Folder of the singleplayer world being played, or null on a server or in menus. */
    default File singleplayerWorldDir() { return null; }

    /** Joins the last multiplayer server again (from the disconnect screen). */
    default void reconnect() {}

    /** True if there is a last server to reconnect to. */
    default boolean canReconnect() { return false; }
}
