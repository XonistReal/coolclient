package dev.coolclient.v1_8_9;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;
import dev.coolclient.core.api.EffectInfo;
import dev.coolclient.core.api.GuiRenderer;
import dev.coolclient.core.api.ItemRef;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.Window;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;

/** GuiRenderer on 1.8.9's fixed-function pipeline. */
final class Renderer189 implements GuiRenderer {
    private static final Identifier INVENTORY = new Identifier("textures/gui/container/inventory.png");
    private final Textures189 textures;
    private final FloatBuffer matrix = BufferUtils.createFloatBuffer(16);
    private int width, height, scale;
    private boolean textured;

    Renderer189(Textures189 textures) {
        this.textures = textures;
    }

    /** Refresh scaled dimensions; call once per frame before use. */
    Renderer189 frame() {
        Window w = new Window(MinecraftClient.getInstance());
        width = w.getWidth();
        height = w.getHeight();
        scale = w.getScaleFactor();
        return this;
    }

    @Override public int width() { return width; }

    @Override public int height() { return height; }

    @Override public double scaleFactor() { return scale; }

    private static BufferBuilder buffer() { return Tessellator.getInstance().getBuffer(); }

    private void beginState(boolean tex) {
        GlStateManager.enableBlend();
        GlStateManager.blendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableAlphaTest();
        GlStateManager.disableCull();
        GlStateManager.color(1f, 1f, 1f, 1f);
        if (tex) GlStateManager.enableTexture(); else GlStateManager.disableTexture();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
    }

    private void endState() {
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableTexture();
        GlStateManager.enableAlphaTest();
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
    }

    @Override
    public void beginTriangles() {
        beginState(false);
        buffer().begin(GL11.GL_TRIANGLES, VertexFormats.POSITION_COLOR);
    }

    @Override
    public void vertex(float x, float y, int c) {
        buffer().vertex(x, y, 0).color((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF, (c >>> 24) & 0xFF).next();
    }

    @Override
    public void endTriangles() {
        Tessellator.getInstance().draw();
        endState();
    }

    @Override
    public void beginTextured(int texture, boolean linear) {
        beginState(true);
        int gl = textures.glId(texture);
        GlStateManager.bindTexture(gl);
        int f = linear ? GL11.GL_LINEAR : GL11.GL_NEAREST;
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, f);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, f);
        buffer().begin(GL11.GL_QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        textured = true;
    }

    @Override
    public void vertexUV(float x, float y, float u, float v, int c) {
        buffer().vertex(x, y, 0).texture(u, v).color((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF, (c >>> 24) & 0xFF).next();
    }

    @Override
    public void endTextured() {
        if (!textured) return;
        textured = false;
        Tessellator.getInstance().draw();
        endState();
    }

    @Override
    public void item(ItemRef item, float x, float y, boolean decorations) {
        Object h = item.handle();
        if (!(h instanceof ItemStack)) return;
        ItemStack stack = (ItemStack) h;
        MinecraftClient mc = MinecraftClient.getInstance();
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableBlend();
        GlStateManager.blendFuncSeparate(770, 771, 1, 0);
        DiffuseLighting.enable();
        mc.getItemRenderer().renderInGuiWithOverrides(stack, 0, 0);
        if (decorations) mc.getItemRenderer().renderGuiItemOverlay(mc.textRenderer, stack, 0, 0);
        DiffuseLighting.disable();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableDepthTest();
        GlStateManager.popMatrix();
    }

    @Override
    public void effectIcon(EffectInfo effect, float x, float y, float size) {
        if (!(effect.handle instanceof Integer)) return;
        int idx = (Integer) effect.handle;
        MinecraftClient.getInstance().getTextureManager().bindTexture(INVENTORY);
        beginState(true);
        float u = (idx % 8) * 18 / 256f, v = (198 + (idx / 8) * 18) / 256f, s = 18 / 256f;
        BufferBuilder b = buffer();
        b.begin(GL11.GL_QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        b.vertex(x, y, 0).texture(u, v).color(255, 255, 255, 255).next();
        b.vertex(x, y + size, 0).texture(u, v + s).color(255, 255, 255, 255).next();
        b.vertex(x + size, y + size, 0).texture(u + s, v + s).color(255, 255, 255, 255).next();
        b.vertex(x + size, y, 0).texture(u + s, v).color(255, 255, 255, 255).next();
        Tessellator.getInstance().draw();
        endState();
    }

    @Override
    public void vanillaText(String text, float x, float y, int argb, boolean shadow) {
        // 1.8.9 treats alpha < 4 as opaque, so skip nearly invisible text instead.
        if (((argb >>> 24) & 0xFF) < 5) return;
        GlStateManager.enableBlend();
        GlStateManager.blendFuncSeparate(770, 771, 1, 0);
        GlStateManager.enableTexture();
        MinecraftClient.getInstance().textRenderer.draw(text, x, y, argb, shadow);
        GlStateManager.disableBlend();
    }

    @Override
    public int vanillaTextWidth(String text) {
        return MinecraftClient.getInstance().textRenderer.getStringWidth(text);
    }

    @Override
    public int vanillaFontHeight() { return 9; }

    @Override
    public void playerPreview(float x, float y, float size, float yaw, float pitch) {
        LivingEntity e = MinecraftClient.getInstance().player;
        if (e == null) return;
        GlStateManager.enableColorMaterial();
        GlStateManager.enableDepthTest();
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 50f);
        GlStateManager.scale(-size, size, size);
        GlStateManager.rotate(180f, 0f, 0f, 1f);
        float bodyYaw = e.bodyYaw, prevBodyYaw = e.prevBodyYaw, eyaw = e.yaw, prevYaw = e.prevYaw;
        float epitch = e.pitch, prevPitch = e.prevPitch, headYaw = e.headYaw, prevHeadYaw = e.prevHeadYaw;
        GlStateManager.rotate(135f, 0f, 1f, 0f);
        DiffuseLighting.enableNormally();
        GlStateManager.rotate(-135f, 0f, 1f, 0f);
        GlStateManager.rotate(yaw, 0f, 1f, 0f);
        e.bodyYaw = e.prevBodyYaw = 0f;
        e.yaw = e.prevYaw = 0f;
        e.pitch = e.prevPitch = pitch;
        e.headYaw = e.prevHeadYaw = 0f;
        EntityRenderDispatcher d = MinecraftClient.getInstance().getEntityRenderManager();
        d.setYaw(180f);
        d.setRenderShadows(false);
        try {
            d.render(e, 0, 0, 0, 0f, 1f);
        } finally {
            d.setRenderShadows(true);
            e.bodyYaw = bodyYaw;
            e.prevBodyYaw = prevBodyYaw;
            e.yaw = eyaw;
            e.prevYaw = prevYaw;
            e.pitch = epitch;
            e.prevPitch = prevPitch;
            e.headYaw = headYaw;
            e.prevHeadYaw = prevHeadYaw;
            GlStateManager.popMatrix();
            DiffuseLighting.disable();
            GlStateManager.disableRescaleNormal();
            GlStateManager.activeTexture(GLX.lightmapTextureUnit);
            GlStateManager.disableTexture();
            GlStateManager.activeTexture(GLX.textureUnit);
            GlStateManager.disableDepthTest();
        }
    }

    @Override
    public void drawModel(float x, float y, float scale, float yaw, float pitch,
                          java.util.function.Consumer<dev.coolclient.core.api.ModelRenderer> draw) {
        GlStateManager.pushMatrix();
        GlStateManager.enableDepthTest();
        GlStateManager.enableColorMaterial();
        GlStateManager.translate(x, y, 100f);
        GlStateManager.scale(scale, scale, -scale);
        GlStateManager.rotate(pitch, 1f, 0f, 0f);
        GlStateManager.rotate(yaw, 0f, 1f, 0f);
        GlStateManager.pushMatrix();
        GlStateManager.rotate(160f, 1f, 0f, 0f);
        DiffuseLighting.enableNormally();
        GlStateManager.popMatrix();
        ModelRenderer189 mr = new ModelRenderer189();
        try {
            draw.accept(mr);
        } finally {
            mr.end();
            DiffuseLighting.disable();
            GlStateManager.disableRescaleNormal();
            GlStateManager.disableColorMaterial();
            GlStateManager.disableDepthTest();
            GlStateManager.enableTexture();
            GlStateManager.color(1f, 1f, 1f, 1f);
            GlStateManager.popMatrix();
        }
    }

    @Override public void push() { GlStateManager.pushMatrix(); }

    @Override public void pop() { GlStateManager.popMatrix(); }

    @Override public void translate(float x, float y) { GlStateManager.translate(x, y, 0f); }

    @Override public void scale(float sx, float sy) { GlStateManager.scale(sx, sy, 1f); }

    @Override
    public void scissor(float x, float y, float w, float h) {
        // Apply the current model-view transform so scissors inside scaled HUD elements line up.
        matrix.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, matrix);
        float sx = matrix.get(0), sy = matrix.get(5), tx = matrix.get(12), ty = matrix.get(13);
        float x1 = x * sx + tx, y1 = y * sy + ty, x2 = (x + w) * sx + tx, y2 = (y + h) * sy + ty;
        int displayH = MinecraftClient.getInstance().height;
        int px = (int) Math.floor(Math.min(x1, x2) * scale);
        int py = (int) Math.floor(displayH - Math.max(y1, y2) * scale);
        int pw = (int) Math.ceil(Math.abs(x2 - x1) * scale);
        int ph = (int) Math.ceil(Math.abs(y2 - y1) * scale);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(px, py, Math.max(0, pw), Math.max(0, ph));
    }

    @Override
    public void clearScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    @Override
    public dev.coolclient.core.api.Gpu gpu() { return Gpu189.INSTANCE.gui(); }
}
