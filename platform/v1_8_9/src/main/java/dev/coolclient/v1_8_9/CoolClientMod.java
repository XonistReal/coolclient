package dev.coolclient.v1_8_9;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.SelfTest;
import dev.coolclient.core.util.Log;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotUtils;
import net.minecraft.world.level.LevelGeneratorType;
import net.minecraft.world.level.LevelInfo;

public class CoolClientMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // 1.8.9 ships log4j 2.0-beta9, which can't parse Loom's dev log config, so log to stdout.
        Log.setSink(new Log.Sink() {
            @Override
            public void log(String level, String message, Throwable t) {
                System.out.println("[CoolClient/" + level + "] " + message);
                if (t != null) t.printStackTrace(System.out);
            }
        });
        CoolClient.init(new Platform189());
        SelfTest.install(new SelfTest.Driver() {
            @Override
            public void screenshot(String name) {
                MinecraftClient mc = MinecraftClient.getInstance();
                ScreenshotUtils.saveScreenshot(mc.runDirectory, name + ".png", mc.width, mc.height, mc.getFramebuffer());
            }

            @Override
            public void createWorld(String name) {
                MinecraftClient.getInstance().startIntegratedServer(name, name,
                        new LevelInfo(4242L, LevelInfo.GameMode.CREATIVE, false, false, LevelGeneratorType.FLAT));
            }
        });
    }
}
