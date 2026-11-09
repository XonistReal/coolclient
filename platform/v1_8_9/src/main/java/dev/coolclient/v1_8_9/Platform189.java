package dev.coolclient.v1_8_9;

import com.mojang.authlib.exceptions.AuthenticationException;
import dev.coolclient.core.api.Feature;
import dev.coolclient.core.api.GameState;
import dev.coolclient.core.api.Input;
import dev.coolclient.core.api.Platform;
import dev.coolclient.core.api.Textures;
import dev.coolclient.core.api.VanillaScreen;
import dev.coolclient.core.gui.UIScreen;
import dev.coolclient.core.util.Log;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ResourcePackScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.SettingsScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.options.ControlsOptionsScreen;
import net.minecraft.client.gui.screen.options.LanguageOptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.Session;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import org.lwjgl.Sys;

import java.io.File;
import java.util.EnumSet;
import java.util.Set;

public final class Platform189 implements Platform {
    private static final Set<Feature> FEATURES = EnumSet.of(Feature.OLD_ANIMATIONS, Feature.MOUSE_DELAY_FIX,
            Feature.BORDERLESS, Feature.TEXTURE_ANIMATIONS, Feature.MOTION_BLUR);

    /** The multiplayer server last joined, for Reconnect. */
    public static net.minecraft.client.network.ServerInfo lastServer;
    private static String gpu;

    public static final Textures189 TEXTURES = new Textures189();
    public static final Renderer189 RENDERER = new Renderer189(TEXTURES);
    private final GameState189 game = new GameState189();
    private final Input189 input = new Input189();
    public static boolean textureAnimations = true;

    private static MinecraftClient mc() { return MinecraftClient.getInstance(); }

    @Override public String minecraftVersion() { return "1.8.9"; }

    @Override public boolean supports(Feature feature) { return FEATURES.contains(feature); }

    @Override public File gameDir() { return FabricLoader.getInstance().getGameDir().toFile(); }

    @Override public GameState game() { return game; }

    @Override public Textures textures() { return TEXTURES; }

    @Override public Input input() { return input; }

    @Override
    public void openScreen(UIScreen screen) {
        mc().setScreen(screen == null ? null : new CoolScreen(screen));
    }

    @Override
    public UIScreen currentScreen() {
        Screen s = mc().currentScreen;
        return s instanceof CoolScreen ? ((CoolScreen) s).ui : null;
    }

    @Override public boolean anyScreenOpen() { return mc().currentScreen != null; }

    @Override
    public void openVanilla(VanillaScreen screen, UIScreen parent) {
        Screen p = parent == null ? null : new CoolScreen(parent);
        MinecraftClient mc = mc();
        switch (screen) {
            case SINGLEPLAYER: mc.setScreen(new SelectWorldScreen(p)); break;
            case MULTIPLAYER: mc.setScreen(new MultiplayerScreen(p)); break;
            case OPTIONS: mc.setScreen(new SettingsScreen(p, mc.options)); break;
            case LANGUAGE: mc.setScreen(new LanguageOptionsScreen(p, mc.options, mc.getLanguageManager())); break;
            case RESOURCE_PACKS: mc.setScreen(new ResourcePackScreen(p)); break;
            case CONTROLS: mc.setScreen(new ControlsOptionsScreen(p, mc.options)); break;
            default: mc.setScreen(null); break;
        }
    }

    @Override public void quit() { mc().scheduleStop(); }

    @Override public String clipboard() { return Screen.getClipboard(); }

    @Override public void setClipboard(String text) { Screen.setClipboard(text); }

    @Override public void openUrl(String url) { Sys.openURL(url); }

    @Override public void openFolder(File folder) { Sys.openURL(folder.toURI().toString()); }

    @Override
    public void sendChat(String message) {
        if (mc().player != null) mc().player.sendChatMessage(message);
    }

    @Override
    public void showChat(String message) {
        if (mc().inGameHud != null) mc().inGameHud.getChatHud().addMessage(new LiteralText(message));
    }

    @Override
    public boolean joinSession(String serverId) {
        Session s = mc().getSession();
        String token = s.getAccessToken();
        if (token == null || token.length() < 20) return false;
        try {
            mc().getSessionService().joinServer(s.getProfile(), token, serverId);
            return true;
        } catch (AuthenticationException e) {
            Log.warn("Mojang session join failed: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void playClickSound() {
        mc().getSoundManager().play(PositionedSoundInstance.master(new Identifier("gui.button.press"), 1.0F));
    }

    @Override public void setBorderless(boolean borderless) { Borderless189.set(borderless); }

    @Override public void setTextureAnimations(boolean enabled) { textureAnimations = enabled; }

    /** The local player's selected stack. */
    public static dev.coolclient.core.api.ItemRef held() {
        return mc().player == null ? dev.coolclient.core.api.ItemRef.EMPTY : ItemRef189.of(mc().player.inventory.getMainHandStack());
    }

    @Override
    public String gpuName() {
        // Read once on the render thread (the HUD asks from there).
        if (gpu == null) gpu = org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_RENDERER);
        return gpu;
    }

    @Override
    public File singleplayerWorldDir() {
        net.minecraft.server.integrated.IntegratedServer s = mc().getServer();
        if (s == null || !mc().isIntegratedServerRunning()) return null;
        return new File(new File(mc().runDirectory, "saves"), s.getLevelName());
    }

    @Override public boolean canReconnect() { return lastServer != null; }

    @Override
    public void reconnect() {
        net.minecraft.client.network.ServerInfo d = lastServer;
        if (d == null) return;
        mc().setScreen(new net.minecraft.client.gui.screen.ConnectScreen(
                new MultiplayerScreen(new net.minecraft.client.gui.screen.TitleScreen()), mc(), d));
    }
}
