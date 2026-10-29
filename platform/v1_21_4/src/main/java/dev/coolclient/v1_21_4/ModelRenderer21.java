package dev.coolclient.v1_21_4;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.coolclient.core.api.ModelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;

/** Cosmetic mesh rendering into the entity's buffer source. */
final class ModelRenderer21 implements ModelRenderer {
    private PoseStack pose;
    private MultiBufferSource buffers;
    private int light;
    private VertexConsumer consumer;
    private boolean emissive;

    void setup(PoseStack pose, MultiBufferSource buffers, int light) {
        this.pose = pose;
        this.buffers = buffers;
        this.light = light;
    }

    @Override public void push() { pose.pushPose(); }

    @Override public void pop() { pose.popPose(); }

    @Override public void translate(float x, float y, float z) { pose.translate(x, y, z); }

    @Override
    public void rotate(float degrees, float x, float y, float z) {
        pose.mulPose(new Quaternionf().rotationAxis((float) Math.toRadians(degrees), x, y, z));
    }

    @Override public void scale(float x, float y, float z) { pose.scale(x, y, z); }

    @Override
    public void begin(int texture, boolean emissive) {
        this.emissive = emissive;
        ResourceLocation rl = Platform21.TEXTURES.location(texture);
        consumer = buffers.getBuffer(emissive ? RenderType.entityTranslucentEmissive(rl) : RenderType.entityTranslucent(rl));
    }

    @Override
    public void vertex(float x, float y, float z, float u, float v, int argb, float nx, float ny, float nz) {
        PoseStack.Pose p = pose.last();
        consumer.addVertex(p, x, y, z).setColor(argb).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(emissive ? 0xF000F0 : light).setNormal(p, nx, ny, nz);
    }

    @Override
    public void end() {
        consumer = null;
    }
}
