package dev.coolclient.v1_8_9;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;
import dev.coolclient.core.CoolClient;
import dev.coolclient.core.SelfTest;
import dev.coolclient.core.util.Log;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.util.ScreenshotUtils;
import net.minecraft.world.level.LevelGeneratorType;
import net.minecraft.world.level.LevelInfo;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
            /** PNG encoding takes longer than a frame, so it runs here; the render thread only copies pixels. */
            private ExecutorService writers;

            @Override
            public void auditMixins() {
                org.spongepowered.asm.mixin.MixinEnvironment.getCurrentEnvironment().audit();
            }

            @Override
            public void screenshot(String name) {
                MinecraftClient mc = MinecraftClient.getInstance();
                Framebuffer fb = mc.getFramebuffer();
                if (!GLX.supportsFbo() || fb == null) {
                    ScreenshotUtils.saveScreenshot(mc.runDirectory, name + ".png", mc.width, mc.height, fb);
                    return;
                }
                final int tw = fb.textureWidth, vw = fb.viewportWidth, vh = fb.viewportHeight;
                IntBuffer buf = BufferUtils.createIntBuffer(tw * fb.textureHeight);
                GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
                GlStateManager.bindTexture(fb.colorAttachment);
                GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, buf);
                final int[] px = new int[buf.remaining()];
                buf.get(px);
                final File out = new File(new File(mc.runDirectory, "screenshots"), name + ".png");
                if (writers == null) {
                    writers = Executors.newFixedThreadPool(6);
                    // Finish writing before the test's quit ends the JVM.
                    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                        writers.shutdown();
                        try {
                            writers.awaitTermination(60, TimeUnit.SECONDS);
                        } catch (InterruptedException ignored) {
                        }
                    }));
                }
                writers.submit(() -> {
                    // Texture rows run bottom-up.
                    BufferedImage img = new BufferedImage(vw, vh, BufferedImage.TYPE_INT_RGB);
                    for (int y = 0; y < vh; y++) img.setRGB(0, y, vw, 1, px, (vh - 1 - y) * tw, tw);
                    out.getParentFile().mkdirs();
                    try {
                        ImageIO.write(img, "png", out);
                    } catch (IOException e) {
                        Log.warn("Screenshot " + name + " failed: " + e.getMessage());
                    }
                });
            }

            @Override
            public void createWorld(String name) {
                // The test runs unattended, usually without window focus; 1.8.9 would open the pause menu.
                MinecraftClient.getInstance().options.pauseOnLostFocus = false;
                // Cheats on: the test runs commands, and without op 1.8.9 kicks the player for chat spam.
                // A fresh folder each run, or an old world's settings (no cheats) would be loaded instead.
                String level = name + "-" + System.currentTimeMillis();
                MinecraftClient.getInstance().startIntegratedServer(level, level,
                        new LevelInfo(4242L, LevelInfo.GameMode.CREATIVE, false, false, LevelGeneratorType.FLAT).enableCommands());
            }
        });
    }
}
