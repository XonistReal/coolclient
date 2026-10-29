package dev.coolclient.v1_8_9;

import com.mojang.blaze3d.platform.GlStateManager;
import dev.coolclient.core.CoolClient;
import dev.coolclient.core.modules.utility.UtilityModules;
import dev.coolclient.core.modules.visual.VisualTweaks;
import dev.coolclient.core.render.gpu.SkyRenderer;
import dev.coolclient.core.waypoint.WaypointManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;

/** World-space drawing: skybox, waypoints, custom hitboxes and filled block outlines. */
public final class World3D189 {
    private static final FloatBuffer MATRIX = BufferUtils.createFloatBuffer(16);
    private static final float[] PROJECTION = new float[16];
    private static final float[] MODEL_VIEW = new float[16];

    private World3D189() {}

    /** Draws the custom overworld sky; true means the vanilla sky should be skipped. */
    public static boolean sky() {
        if (SkyRenderer.active() == null || MinecraftClient.getInstance().world == null) return false;
        if (!"overworld".equals(CoolClient.get().platform().game().dimension())) return false;
        Gpu189 gpu = Gpu189.INSTANCE.world();
        if (gpu == null) return false;
        read(GL11.GL_PROJECTION_MATRIX, PROJECTION);
        read(GL11.GL_MODELVIEW_MATRIX, MODEL_VIEW);
        return SkyRenderer.render(gpu, PROJECTION, MODEL_VIEW, CoolClient.get().platform().game().worldTime());
    }

    private static void read(int which, float[] out) {
        MATRIX.clear();
        GL11.glGetFloat(which, MATRIX);
        MATRIX.get(out);
    }

    private static void color(BufferBuilder b, double x, double y, double z, int c) {
        b.vertex(x, y, z).color((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF, (c >>> 24) & 0xFF).next();
    }

    /** Filled box faces (used by Block Outline's fill option). Assumes GL state is set by caller. */
    public static void fillBox(Box bb, int c) {
        Tessellator t = Tessellator.getInstance();
        BufferBuilder b = t.getBuffer();
        b.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR);
        double x1 = bb.minX, y1 = bb.minY, z1 = bb.minZ, x2 = bb.maxX, y2 = bb.maxY, z2 = bb.maxZ;
        color(b, x1, y1, z1, c); color(b, x2, y1, z1, c); color(b, x2, y1, z2, c); color(b, x1, y1, z2, c);
        color(b, x1, y2, z1, c); color(b, x1, y2, z2, c); color(b, x2, y2, z2, c); color(b, x2, y2, z1, c);
        color(b, x1, y1, z1, c); color(b, x1, y2, z1, c); color(b, x2, y2, z1, c); color(b, x2, y1, z1, c);
        color(b, x1, y1, z2, c); color(b, x2, y1, z2, c); color(b, x2, y2, z2, c); color(b, x1, y2, z2, c);
        color(b, x1, y1, z1, c); color(b, x1, y1, z2, c); color(b, x1, y2, z2, c); color(b, x1, y2, z1, c);
        color(b, x2, y1, z1, c); color(b, x2, y2, z1, c); color(b, x2, y2, z2, c); color(b, x2, y1, z2, c);
        t.draw();
    }

    private static void outlineBox(Box bb, int c) {
        Tessellator t = Tessellator.getInstance();
        BufferBuilder b = t.getBuffer();
        b.begin(GL11.GL_LINES, VertexFormats.POSITION_COLOR);
        double[] xs = {bb.minX, bb.maxX}, ys = {bb.minY, bb.maxY}, zs = {bb.minZ, bb.maxZ};
        for (int i = 0; i < 2; i++)
            for (int j = 0; j < 2; j++) {
                color(b, xs[0], ys[i], zs[j], c); color(b, xs[1], ys[i], zs[j], c);
                color(b, xs[i], ys[0], zs[j], c); color(b, xs[i], ys[1], zs[j], c);
                color(b, xs[i], ys[j], zs[0], c); color(b, xs[i], ys[j], zs[1], c);
            }
        t.draw();
    }

    /** Replacement for vanilla F3+B hitbox rendering. */
    public static void hitbox(Entity e, double x, double y, double z, float tickDelta, VisualTweaks.Hitboxes hb) {
        if (hb.playersOnly.on() && !(e instanceof PlayerEntity)) return;
        GlStateManager.depthMask(false);
        GlStateManager.disableTexture();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.blendFuncSeparate(770, 771, 1, 0);
        GL11.glLineWidth(hb.lineWidth.f());
        Box bb = e.getBoundingBox();
        Box rel = new Box(bb.minX - e.x + x, bb.minY - e.y + y, bb.minZ - e.z + z, bb.maxX - e.x + x, bb.maxY - e.y + y, bb.maxZ - e.z + z);
        outlineBox(rel, hb.color.color());
        if (hb.showEyes.on() && e instanceof net.minecraft.entity.LivingEntity) {
            double ey = y + e.getEyeHeight();
            outlineBox(new Box(rel.minX, ey - 0.01, rel.minZ, rel.maxX, ey + 0.01, rel.maxZ), 0xFFFF3030);
        }
        if (hb.showLook.on()) {
            Vec3d look = e.getRotationVector(tickDelta);
            Tessellator t = Tessellator.getInstance();
            BufferBuilder b = t.getBuffer();
            b.begin(GL11.GL_LINES, VertexFormats.POSITION_COLOR);
            color(b, x, y + e.getEyeHeight(), z, 0xFF3060FF);
            color(b, x + look.x * 2, y + e.getEyeHeight() + look.y * 2, z + look.z * 2, 0xFF3060FF);
            t.draw();
        }
        GL11.glLineWidth(1f);
        GlStateManager.disableBlend();
        GlStateManager.enableTexture();
        GlStateManager.enableLighting();
        GlStateManager.enableCull();
        GlStateManager.depthMask(true);
    }

    /** Draws waypoint beams and labels; called from the world pass before the hand renders. */
    public static void waypoints(float tickDelta) {
        UtilityModules.Waypoints mod = CoolClient.get().modules().get(UtilityModules.Waypoints.class);
        if (mod == null || !mod.isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;
        CoolClient.get().waypoints().setDimension(CoolClient.get().platform().game().dimension());
        EntityRenderDispatcher d = mc.getEntityRenderManager();
        double cx = d.cameraX, cy = d.cameraY, cz = d.cameraZ;
        TextRenderer font = mc.textRenderer;
        for (WaypointManager.Waypoint w : CoolClient.get().waypoints().current()) {
            double dx = w.x + 0.5 - cx, dy = w.y - cy, dz = w.z + 0.5 - cz;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > mod.maxDistance.d()) continue;
            GlStateManager.pushMatrix();
            GlStateManager.disableTexture();
            GlStateManager.disableLighting();
            GlStateManager.disableCull();
            GlStateManager.enableBlend();
            GlStateManager.blendFuncSeparate(770, 1, 1, 0);
            GlStateManager.depthMask(false);
            if (mod.beam.on()) {
                Tessellator t = Tessellator.getInstance();
                BufferBuilder b = t.getBuffer();
                b.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR);
                int c = (w.color & 0xFFFFFF) | 0x50000000, top = w.color & 0xFFFFFF;
                double r = 0.2, y0 = -cy, y1 = 256 - cy;
                color(b, dx - r, y0, dz, c); color(b, dx + r, y0, dz, c); color(b, dx + r, y1, dz, top); color(b, dx - r, y1, dz, top);
                color(b, dx, y0, dz - r, c); color(b, dx, y0, dz + r, c); color(b, dx, y1, dz + r, top); color(b, dx, y1, dz - r, top);
                t.draw();
            }
            // Label, pulled in to a readable distance and scaled with distance.
            double ld = Math.min(dist, 48);
            double k = dist < 1e-3 ? 0 : ld / dist;
            float scale = (float) (0.02 + ld * 0.0025);
            GlStateManager.translate(dx * k, dy * k + 1.5 * k, dz * k);
            GlStateManager.rotate(-d.yaw, 0f, 1f, 0f);
            GlStateManager.rotate(d.pitch, 1f, 0f, 0f);
            GlStateManager.scale(-scale, -scale, scale);
            GlStateManager.disableDepthTest();
            GlStateManager.blendFuncSeparate(770, 771, 1, 0);
            String text = w.name + (mod.distance.on() ? " §7" + Math.round(dist) + "m" : "");
            int tw = font.getStringWidth(text) / 2;
            Tessellator t = Tessellator.getInstance();
            BufferBuilder b = t.getBuffer();
            b.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR);
            color(b, -tw - 3, -2, 0, 0x90000000); color(b, -tw - 3, 9, 0, 0x90000000);
            color(b, tw + 3, 9, 0, 0x90000000); color(b, tw + 3, -2, 0, 0x90000000);
            t.draw();
            GlStateManager.enableTexture();
            font.draw(text, -tw, 0, w.color | 0xFF000000);
            GlStateManager.enableDepthTest();
            GlStateManager.depthMask(true);
            GlStateManager.disableBlend();
            GlStateManager.enableCull();
            GlStateManager.enableLighting();
            GlStateManager.popMatrix();
        }
    }
}
