package dev.coolclient.v1_21_4;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.coolclient.core.CoolClient;
import dev.coolclient.core.modules.utility.UtilityModules;
import dev.coolclient.core.waypoint.WaypointManager;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/** Waypoint beams and labels, drawn after the level and before the hand. */
public final class World3D21 {
    private World3D21() {}

    public static void waypoints(Camera camera) {
        if (CoolClient.get() == null) return;
        UtilityModules.Waypoints mod = CoolClient.get().modules().get(UtilityModules.Waypoints.class);
        Minecraft mc = Minecraft.getInstance();
        if (mod == null || !mod.isEnabled() || mc.level == null) return;
        CoolClient.get().waypoints().setDimension(CoolClient.get().platform().game().dimension());
        java.util.List<WaypointManager.Waypoint> list = CoolClient.get().waypoints().current();
        if (list.isEmpty()) return;

        Vec3 cam = camera.getPosition();
        Matrix4f view = new Matrix4f().rotation(camera.rotation().conjugate(new Quaternionf()));
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        Font font = mc.font;

        for (WaypointManager.Waypoint w : list) {
            double dx = w.x + 0.5 - cam.x, dy = w.y - cam.y, dz = w.z + 0.5 - cam.z;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > mod.maxDistance.d()) continue;

            if (mod.beam.on()) {
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                        com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
                RenderSystem.disableCull();
                RenderSystem.depthMask(false);
                RenderSystem.setShader(CoreShaders.POSITION_COLOR);
                BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                int c = (w.color & 0xFFFFFF) | 0x60000000, top = w.color & 0xFFFFFF;
                float r = 0.2f, y0 = (float) (mc.level.getMinY() - cam.y), y1 = (float) (mc.level.getMaxY() - cam.y);
                float x = (float) dx, z = (float) dz;
                b.addVertex(view, x - r, y0, z).setColor(c);
                b.addVertex(view, x + r, y0, z).setColor(c);
                b.addVertex(view, x + r, y1, z).setColor(top);
                b.addVertex(view, x - r, y1, z).setColor(top);
                b.addVertex(view, x, y0, z - r).setColor(c);
                b.addVertex(view, x, y0, z + r).setColor(c);
                b.addVertex(view, x, y1, z + r).setColor(top);
                b.addVertex(view, x, y1, z - r).setColor(top);
                BufferUploader.drawWithShader(b.buildOrThrow());
                RenderSystem.depthMask(true);
                RenderSystem.enableCull();
                RenderSystem.defaultBlendFunc();
                RenderSystem.disableBlend();
            }

            double ld = Math.min(dist, 48);
            double k = dist < 1e-3 ? 0 : ld / dist;
            float scale = (float) (0.02 + ld * 0.0025);
            Matrix4f m = new Matrix4f(view)
                    .translate((float) (dx * k), (float) (dy * k + 1.5 * k), (float) (dz * k))
                    .rotate(camera.rotation())
                    .scale(scale, -scale, scale);
            String text = w.name + (mod.distance.on() ? " §7" + Math.round(dist) + "m" : "");
            float tw = font.width(text) / 2f;
            font.drawInBatch(text, -tw, 0, w.color | 0xFF000000, false, m, buffers, Font.DisplayMode.SEE_THROUGH, 0x90000000, 0xF000F0);
        }
        buffers.endBatch();
    }
}
