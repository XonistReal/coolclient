package dev.coolclient.v1_21_4;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.coolclient.core.CoolClient;
import dev.coolclient.core.api.WorldDraw;
import dev.coolclient.core.event.Events;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/** {@link WorldDraw} for 1.21.4: camera-relative immediate geometry with the position-colour shader. */
public final class WorldDraw21 implements WorldDraw {
    private static final WorldDraw21 INSTANCE = new WorldDraw21();

    private Camera camera;
    private Matrix4f view;
    private Vec3 cam;
    private BufferBuilder builder;
    private boolean lines, throughWalls;

    private WorldDraw21() {}

    /** Posts {@link Events.RenderWorld}; called from the level pass, before the hand. */
    public static void post(Camera camera, float partialTicks) {
        if (CoolClient.get() == null || Minecraft.getInstance().level == null) return;
        WorldDraw21 d = INSTANCE;
        d.camera = camera;
        d.cam = camera.getPosition();
        d.view = new Matrix4f().rotation(camera.rotation().conjugate(new Quaternionf()));
        try {
            CoolClient.get().events().post(new Events.RenderWorld(d, partialTicks));
        } finally {
            if (d.builder != null) d.end();
            Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
        }
    }

    @Override public double cameraX() { return cam.x; }

    @Override public double cameraY() { return cam.y; }

    @Override public double cameraZ() { return cam.z; }

    @Override
    public void lines(boolean throughWalls) { begin(true, throughWalls); }

    @Override
    public void quads(boolean throughWalls) { begin(false, throughWalls); }

    private void begin(boolean lines, boolean throughWalls) {
        if (builder != null) end();
        this.lines = lines;
        this.throughWalls = throughWalls;
        builder = Tesselator.getInstance().begin(lines ? VertexFormat.Mode.DEBUG_LINES : VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_COLOR);
    }

    @Override
    public void vertex(double x, double y, double z, int argb) {
        if (builder == null) return;
        builder.addVertex(view, (float) (x - cam.x), (float) (y - cam.y), (float) (z - cam.z)).setColor(argb);
    }

    @Override
    public void end() {
        BufferBuilder b = builder;
        builder = null;
        if (b == null) return;
        MeshData mesh = b.build();
        if (mesh == null) return;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        if (throughWalls) RenderSystem.disableDepthTest();
        else RenderSystem.enableDepthTest();
        if (lines) RenderSystem.lineWidth(2f);
        RenderSystem.setShader(CoreShaders.POSITION_COLOR);
        BufferUploader.drawWithShader(mesh);
        RenderSystem.lineWidth(1f);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableBlend();
    }

    @Override
    public void text(String text, double x, double y, double z, float scale, int argb, boolean throughWalls, int background) {
        if (builder != null) end();
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        Matrix4f m = new Matrix4f(view)
                .translate((float) (x - cam.x), (float) (y - cam.y), (float) (z - cam.z))
                .rotate(camera.rotation())
                .scale(scale, -scale, scale);
        float w = font.width(text) / 2f;
        font.drawInBatch(text, -w, -font.lineHeight / 2f, argb, false, m, buffers,
                throughWalls ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL, background, 0xF000F0);
        buffers.endBatch();
    }
}
