package dev.coolclient.v1_21_4;

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
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.LanguageSelectScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.io.File;
import java.util.EnumSet;
import java.util.Set;

public final class Platform21 implements Platform {
    private static final Set<Feature> FEATURES = EnumSet.of(Feature.OFFHAND, Feature.TOTEMS, Feature.SHIELDS,
            Feature.COOLDOWNS, Feature.END_CRYSTALS, Feature.ELYTRA, Feature.TEXTURE_ANIMATIONS);

    public static final Textures21 TEXTURES = new Textures21();
    public static final Renderer21 RENDERER = new Renderer21(TEXTURES);
    public static boolean textureAnimations = true;
    private final GameState21 game = new GameState21();
    private final Input21 input = new Input21();

    private static Minecraft mc() { return Minecraft.getInstance(); }

    @Override public String minecraftVersion() { return "1.21.4"; }

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
    public boolean isLoading() {
        return mc().getOverlay() != null;
    }

    @Override
    public UIScreen currentScreen() {
        Screen s = mc().screen;
        return s instanceof CoolScreen ? ((CoolScreen) s).ui : null;
    }

    @Override public boolean anyScreenOpen() { return mc().screen != null; }

    @Override
    public void openVanilla(VanillaScreen screen, UIScreen parent) {
        Screen p = parent == null ? null : new CoolScreen(parent);
        Minecraft mc = mc();
        switch (screen) {
            case SINGLEPLAYER: mc.setScreen(new SelectWorldScreen(p)); break;
            case MULTIPLAYER: mc.setScreen(new JoinMultiplayerScreen(p)); break;
            case OPTIONS: mc.setScreen(new OptionsScreen(p, mc.options)); break;
            case LANGUAGE: mc.setScreen(new LanguageSelectScreen(p, mc.options, mc.getLanguageManager())); break;
            case CONTROLS: mc.setScreen(new KeyBindsScreen(p, mc.options)); break;
            case RESOURCE_PACKS: mc.setScreen(new OptionsScreen(p, mc.options)); break;
            default: mc.setScreen(null); break;
        }
    }

    @Override public void quit() { mc().stop(); }

    @Override public String clipboard() { return mc().keyboardHandler.getClipboard(); }

    @Override public void setClipboard(String text) { mc().keyboardHandler.setClipboard(text); }

    @Override public void openUrl(String url) { Util.getPlatform().openUri(url); }

    @Override public void openFolder(File folder) { Util.getPlatform().openFile(folder); }

    @Override
    public void sendChat(String message) {
        if (mc().player == null) return;
        if (message.startsWith("/")) mc().player.connection.sendCommand(message.substring(1));
        else mc().player.connection.sendChat(message);
    }

    @Override
    public void showChat(String message) {
        mc().gui.getChat().addMessage(Component.literal(message));
    }

    @Override
    public boolean joinSession(String serverId) {
        User u = mc().getUser();
        String token = u.getAccessToken();
        if (token == null || token.length() < 20) return false;
        try {
            mc().getMinecraftSessionService().joinServer(u.getProfileId(), token, serverId);
            return true;
        } catch (AuthenticationException e) {
            Log.warn("Mojang session join failed: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void playClickSound() {
        mc().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override public void setTextureAnimations(boolean enabled) { textureAnimations = enabled; }
}
