package dev.coolclient.v1_8_9;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.modules.visual.VisualTweaks;
import dev.coolclient.core.util.Log;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderEffect;
import net.minecraft.client.resource.ResourceMetadataProvider;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Frame-blending motion blur using vanilla's phosphor shader program. The post-effect JSON is
 * generated in memory (so no resource pack is needed) and rebuilt when the strength changes.
 */
public final class MotionBlur189 {
    private static final Identifier ID = new Identifier("coolclient", "shaders/post/motion_blur.json");
    private static ShaderEffect shader;
    private static float builtFor = -1;
    private static int width, height;
    private static boolean failed;

    private MotionBlur189() {}

    public static void render(float tickDelta) {
        VisualTweaks.MotionBlur mb = CoolClient.get().modules().get(VisualTweaks.MotionBlur.class);
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mb == null || !mb.isEnabled() || mc.world == null || failed) {
            if (shader != null && (mb == null || !mb.isEnabled())) dispose();
            return;
        }
        float amount = mb.amount();
        if (shader == null || amount != builtFor) build(amount);
        if (shader == null) return;
        if (mc.width != width || mc.height != height) {
            width = mc.width;
            height = mc.height;
            shader.setupDimensions(width, height);
        }
        shader.render(tickDelta);
        mc.getFramebuffer().bind(true);
    }

    private static void build(float amount) {
        dispose();
        final String json = "{\"targets\":[\"swap\",\"previous\"],\"passes\":["
                + "{\"name\":\"phosphor\",\"intarget\":\"minecraft:main\",\"outtarget\":\"swap\","
                + "\"auxtargets\":[{\"name\":\"PrevSampler\",\"id\":\"previous\"}],"
                + "\"uniforms\":[{\"name\":\"Phosphor\",\"values\":[" + amount + "," + amount + "," + amount + "]}]},"
                + "{\"name\":\"blit\",\"intarget\":\"swap\",\"outtarget\":\"previous\"},"
                + "{\"name\":\"blit\",\"intarget\":\"swap\",\"outtarget\":\"minecraft:main\"}]}";
        final MinecraftClient mc = MinecraftClient.getInstance();
        final ResourceManager vanilla = mc.getResourceManager();
        ResourceManager rm = new ResourceManager() {
            @Override
            public Set<String> getAllNamespaces() { return vanilla.getAllNamespaces(); }

            @Override
            public Resource getResource(Identifier id) throws IOException {
                if (ID.equals(id)) return memory(id, json);
                return vanilla.getResource(id);
            }

            @Override
            public List<Resource> getAllResources(Identifier id) throws IOException {
                if (ID.equals(id)) return Collections.singletonList(memory(id, json));
                return vanilla.getAllResources(id);
            }
        };
        try {
            shader = new ShaderEffect(mc.getTextureManager(), rm, mc.getFramebuffer(), ID);
            width = mc.width;
            height = mc.height;
            shader.setupDimensions(width, height);
            builtFor = amount;
        } catch (Exception e) {
            Log.error("Motion blur shader failed to load; disabling", e);
            failed = true;
            shader = null;
        }
    }

    public static void dispose() {
        if (shader != null) shader.disable();
        shader = null;
        builtFor = -1;
    }

    private static Resource memory(final Identifier id, final String text) {
        return new Resource() {
            @Override public Identifier getId() { return id; }

            @Override public InputStream getInputStream() { return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)); }

            @Override public boolean hasMetadata() { return false; }

            @Override public <T extends ResourceMetadataProvider> T getMetadata(String key) { return null; }

            @Override public String getResourcePackName() { return "coolclient"; }
        };
    }
}
