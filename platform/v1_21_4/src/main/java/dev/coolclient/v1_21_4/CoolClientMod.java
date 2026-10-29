package dev.coolclient.v1_21_4;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.SelfTest;
import dev.coolclient.core.util.Log;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoolClientMod implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("CoolClient");

    @Override
    public void onInitializeClient() {
        if (System.getProperty("java.awt.headless") == null) System.setProperty("java.awt.headless", "true");
        Log.setSink(new Log.Sink() {
            @Override
            public void log(String level, String message, Throwable t) {
                if ("ERROR".equals(level)) LOGGER.error(message, t);
                else if ("WARN".equals(level)) LOGGER.warn(message, t);
                else LOGGER.info(message);
            }
        });
        CoolClient.init(new Platform21());
        SelfTest.install(new SelfTest.Driver() {
            @Override
            public void screenshot(String name) {
                Minecraft mc = Minecraft.getInstance();
                Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), msg -> {});
            }

            @Override
            public void createWorld(String name) {
                String level = name + "-" + System.currentTimeMillis();
                LevelSettings settings = new LevelSettings(level, GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                        new GameRules(WorldDataConfiguration.DEFAULT.enabledFeatures()), WorldDataConfiguration.DEFAULT);
                Minecraft.getInstance().createWorldOpenFlows().createFreshLevel(level, settings,
                        new WorldOptions(4242L, false, false), WorldPresets::createFlatWorldDimensions, null);
            }
        });
    }
}
