package dev.coolclient.v1_8_9;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;
import dev.coolclient.core.api.ModelRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import org.lwjgl.opengl.GL11;

import java.util.ArrayDeque;

/**
 * Immediate-mode cosmetic mesh rendering inside an entity render pass.
 *
 * The Tessellator only applies the GL matrix when the batch is drawn, so push/translate/rotate
 * between begin() and end() would be lost. Transforms are therefore kept on the CPU and applied
 * to each vertex as it is added; the GL matrix stays at the body part's pose.
 */
final class ModelRenderer189 implements ModelRenderer {
    private final ArrayDeque<float[]> stack = new ArrayDeque<float[]>();
    /** Column-major 4x4, like OpenGL. */
    private float[] m = identity();
    private boolean emissive;
    private boolean drawing;

    @Override public void push() { stack.push(m.clone()); }

    @Override public void pop() { m = stack.isEmpty() ? identity() : stack.pop(); }

    @Override
    public void translate(float x, float y, float z) {
        m[12] += m[0] * x + m[4] * y + m[8] * z;
        m[13] += m[1] * x + m[5] * y + m[9] * z;
        m[14] += m[2] * x + m[6] * y + m[10] * z;
    }

    @Override
    public void rotate(float degrees, float x, float y, float z) {
        float len = (float) Math.sqrt(x * x + y * y + z * z);
        if (len == 0) return;
        x /= len; y /= len; z /= len;
        double rad = Math.toRadians(degrees);
        float c = (float) Math.cos(rad), s = (float) Math.sin(rad), t = 1 - c;
        float[] r = {
                t * x * x + c, t * x * y + s * z, t * x * z - s * y, 0,
                t * x * y - s * z, t * y * y + c, t * y * z + s * x, 0,
                t * x * z + s * y, t * y * z - s * x, t * z * z + c, 0,
                0, 0, 0, 1};
        float[] o = new float[16];
        for (int col = 0; col < 4; col++) {
            for (int row = 0; row < 4; row++) {
                o[col * 4 + row] = m[row] * r[col * 4] + m[4 + row] * r[col * 4 + 1]
                        + m[8 + row] * r[col * 4 + 2] + m[12 + row] * r[col * 4 + 3];
            }
        }
        m = o;
    }

    @Override
    public void scale(float x, float y, float z) {
        for (int i = 0; i < 3; i++) {
            m[i] *= x;
            m[4 + i] *= y;
            m[8 + i] *= z;
        }
    }

    @Override
    public void begin(int texture, boolean emissive) {
        this.emissive = emissive;
        GlStateManager.bindTexture(Platform189.TEXTURES.glId(texture));
        GlStateManager.enableBlend();
        GlStateManager.blendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableCull();
        GlStateManager.enableRescaleNormal();
        GlStateManager.depthMask(true);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GlStateManager.color(1f, 1f, 1f, 1f);
        if (emissive) {
            GlStateManager.disableLighting();
            GLX.gl13MultiTexCoord2f(GLX.lightmapTextureUnit, 240f, 240f);
        }
        Tessellator.getInstance().getBuffer().begin(GL11.GL_QUADS, VertexFormats.POSITION_TEXTURE_COLOR_NORMAL);
        drawing = true;
    }

    @Override
    public void vertex(float x, float y, float z, float u, float v, int c, float nx, float ny, float nz) {
        float px = m[0] * x + m[4] * y + m[8] * z + m[12];
        float py = m[1] * x + m[5] * y + m[9] * z + m[13];
        float pz = m[2] * x + m[6] * y + m[10] * z + m[14];
        float tx = m[0] * nx + m[4] * ny + m[8] * nz;
        float ty = m[1] * nx + m[5] * ny + m[9] * nz;
        float tz = m[2] * nx + m[6] * ny + m[10] * nz;
        float nl = (float) Math.sqrt(tx * tx + ty * ty + tz * tz);
        if (nl > 0) { tx /= nl; ty /= nl; tz /= nl; }
        BufferBuilder b = Tessellator.getInstance().getBuffer();
        b.vertex(px, py, pz).texture(u, v).color((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF, (c >>> 24) & 0xFF)
                .normal(tx, ty, tz).next();
    }

    @Override
    public void end() {
        if (!drawing) return;
        drawing = false;
        Tessellator.getInstance().draw();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        if (emissive) GlStateManager.enableLighting();
    }

    private static float[] identity() {
        return new float[]{1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};
    }
}
