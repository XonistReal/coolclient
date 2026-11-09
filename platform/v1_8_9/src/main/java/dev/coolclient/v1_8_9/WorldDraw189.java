package dev.coolclient.v1_8_9;

import com.mojang.blaze3d.platform.GlStateManager;
import dev.coolclient.core.CoolClient;
import dev.coolclient.core.api.WorldDraw;
import dev.coolclient.core.event.Events;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import org.lwjgl.opengl.GL11;

/** {@link WorldDraw} for 1.8.9: fixed-function geometry relative to the render camera. */
public final class WorldDraw189 implements WorldDraw {
    private static final WorldDraw189 INSTANCE = new WorldDraw189();

    private double cx, cy, cz;
    private float yaw, pitch;
    private boolean open, throughWalls;
    private int shadeModel;

    private WorldDraw189() {}

    /** Posts {@link Events.RenderWorld}; called from the world pass, before the hand. */
    public static void post(float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (CoolClient.get() == null || mc.world == null) return;
        EntityRenderDispatcher d = mc.getEntityRenderManager();
        WorldDraw189 w = INSTANCE;
        w.cx = d.cameraX;
        w.cy = d.cameraY;
        w.cz = d.cameraZ;
        w.yaw = d.yaw;
        w.pitch = d.pitch;
        try {
            CoolClient.get().events().post(new Events.RenderWorld(w, tickDelta));
        } finally {
            if (w.open) w.end();
        }
    }

    @Override public double cameraX() { return cx; }

    @Override public double cameraY() { return cy; }

    @Override public double cameraZ() { return cz; }

    @Override
    public void lines(boolean throughWalls) { begin(GL11.GL_LINES, throughWalls); }

    @Override
    public void quads(boolean throughWalls) { begin(GL11.GL_QUADS, throughWalls); }

    private void begin(int mode, boolean throughWalls) {
        if (open) end();
        this.throughWalls = throughWalls;
        GlStateManager.disableTexture();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.blendFuncSeparate(770, 771, 1, 0);
        GlStateManager.depthMask(false);
        if (throughWalls) GlStateManager.disableDepthTest();
        GL11.glLineWidth(2f);
        // Gradients (loot beams fade upward) need smooth shading; flat would paint the last vertex's colour.
        shadeModel = GL11.glGetInteger(GL11.GL_SHADE_MODEL);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        Tessellator.getInstance().getBuffer().begin(mode, VertexFormats.POSITION_COLOR);
        open = true;
    }

    @Override
    public void vertex(double x, double y, double z, int c) {
        if (!open) return;
        BufferBuilder b = Tessellator.getInstance().getBuffer();
        b.vertex(x - cx, y - cy, z - cz).color((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF, (c >>> 24) & 0xFF).next();
    }

    @Override
    public void end() {
        if (!open) return;
        open = false;
        Tessellator.getInstance().draw();
        GL11.glLineWidth(1f);
        GlStateManager.shadeModel(shadeModel);
        GlStateManager.enableDepthTest();
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.enableCull();
        GlStateManager.enableTexture();
        GlStateManager.enableLighting();
    }

    @Override
    public void text(String text, double x, double y, double z, float scale, int argb, boolean throughWalls, int background) {
        if (open) end();
        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x - cx, y - cy, z - cz);
        GlStateManager.rotate(-yaw, 0f, 1f, 0f);
        GlStateManager.rotate(pitch, 1f, 0f, 0f);
        GlStateManager.scale(-scale, -scale, scale);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        if (throughWalls) GlStateManager.disableDepthTest();
        GlStateManager.enableBlend();
        GlStateManager.blendFuncSeparate(770, 771, 1, 0);
        int w = font.getStringWidth(text) / 2;
        if ((background >>> 24) != 0) {
            GlStateManager.disableTexture();
            Tessellator t = Tessellator.getInstance();
            BufferBuilder b = t.getBuffer();
            b.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR);
            int r = (background >> 16) & 0xFF, g = (background >> 8) & 0xFF, bl = background & 0xFF, a = (background >>> 24) & 0xFF;
            b.vertex(-w - 1, -5, 0).color(r, g, bl, a).next();
            b.vertex(-w - 1, 4, 0).color(r, g, bl, a).next();
            b.vertex(w + 1, 4, 0).color(r, g, bl, a).next();
            b.vertex(w + 1, -5, 0).color(r, g, bl, a).next();
            t.draw();
            GlStateManager.enableTexture();
        }
        font.draw(text, -w, -4, argb);
        GlStateManager.enableDepthTest();
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.popMatrix();
    }
}
