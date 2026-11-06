package dev.coolclient.v1_21_4;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.coolclient.core.api.EffectInfo;
import dev.coolclient.core.api.GuiRenderer;
import dev.coolclient.core.api.ItemRef;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** GuiRenderer on top of GuiGraphics plus immediate-mode Tesselator drawing. */
public final class Renderer21 implements GuiRenderer {
    private final Textures21 textures;
    private GuiGraphics g;
    private BufferBuilder buffer;
    private int vertices;
    private final Vector3f tmp = new Vector3f();

    Renderer21(Textures21 textures) {
        this.textures = textures;
    }

    public Renderer21 frame(GuiGraphics graphics) {
        this.g = graphics;
        return this;
    }

    public GuiGraphics graphics() { return g; }

    @Override public int width() { return g.guiWidth(); }

    @Override public int height() { return g.guiHeight(); }

    @Override public double scaleFactor() { return Minecraft.getInstance().getWindow().getGuiScale(); }

    private Matrix4f pose() { return g.pose().last().pose(); }

    private void beginState() {
        g.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private void endState() {
        if (vertices > 0) {
            MeshData mesh = buffer.buildOrThrow();
            BufferUploader.drawWithShader(mesh);
        } else {
            MeshData mesh = buffer.build();
            if (mesh != null) mesh.close();
        }
        buffer = null;
        vertices = 0;
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    @Override
    public void beginTriangles() {
        beginState();
        RenderSystem.setShader(CoreShaders.POSITION_COLOR);
        buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        vertices = 0;
    }

    @Override
    public void vertex(float x, float y, int argb) {
        buffer.addVertex(pose(), x, y, 0f).setColor(argb);
        vertices++;
    }

    @Override
    public void endTriangles() {
        if (buffer != null) endState();
    }

    @Override
    public void beginTextured(int texture, boolean linear) {
        beginState();
        RenderSystem.setShader(CoreShaders.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, textures.glId(texture));
        buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        vertices = 0;
    }

    @Override
    public void vertexUV(float x, float y, float u, float v, int argb) {
        buffer.addVertex(pose(), x, y, 0f).setUv(u, v).setColor(argb);
        vertices++;
    }

    @Override
    public void endTextured() {
        if (buffer != null) endState();
    }

    @Override
    public void item(ItemRef item, float x, float y, boolean decorations) {
        if (!(item.handle() instanceof ItemStack)) return;
        ItemStack stack = (ItemStack) item.handle();
        g.pose().pushPose();
        g.pose().translate(x, y, 0f);
        g.renderItem(stack, 0, 0);
        if (decorations) g.renderItemDecorations(Minecraft.getInstance().font, stack, 0, 0);
        g.pose().popPose();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void effectIcon(EffectInfo effect, float x, float y, float size) {
        if (!(effect.handle instanceof Holder)) return;
        TextureAtlasSprite sprite = Minecraft.getInstance().getMobEffectTextures().get((Holder<MobEffect>) effect.handle);
        g.pose().pushPose();
        g.pose().translate(x, y, 0f);
        g.pose().scale(size / 18f, size / 18f, 1f);
        g.blitSprite(RenderType::guiTextured, sprite, 0, 0, 18, 18);
        g.pose().popPose();
    }

    @Override
    public void vanillaText(String text, float x, float y, int argb, boolean shadow) {
        if (((argb >>> 24) & 0xFF) < 5) return;
        int ix = (int) Math.floor(x), iy = (int) Math.floor(y);
        if (ix != x || iy != y) {
            g.pose().pushPose();
            g.pose().translate(x - ix, y - iy, 0f);
            g.drawString(Minecraft.getInstance().font, text, ix, iy, argb, shadow);
            g.pose().popPose();
        } else {
            g.drawString(Minecraft.getInstance().font, text, ix, iy, argb, shadow);
        }
    }

    @Override
    public int vanillaTextWidth(String text) {
        return Minecraft.getInstance().font.width(text);
    }

    @Override
    public int vanillaFontHeight() { return 9; }

    @Override
    public void playerPreview(float x, float y, float size, float yaw, float pitch) {
        LivingEntity e = Minecraft.getInstance().player;
        if (e == null) return;
        float bodyRot = e.yBodyRot, bodyRotO = e.yBodyRotO, yRot = e.getYRot(), yRotO = e.yRotO;
        float xRot = e.getXRot(), xRotO = e.xRotO, headRot = e.yHeadRot, headRotO = e.yHeadRotO;
        e.yBodyRot = e.yBodyRotO = 180f + yaw;
        e.setYRot(180f + yaw);
        e.yRotO = 180f + yaw;
        e.setXRot(pitch);
        e.xRotO = pitch;
        e.yHeadRot = e.yHeadRotO = 180f + yaw;
        try {
            float s = size / e.getScale();
            Vector3f offset = new Vector3f(0f, e.getBbHeight() / 2f, 0f);
            g.flush();
            InventoryScreen.renderEntityInInventory(g, x, y - e.getBbHeight() * s / 2f, s, offset,
                    new Quaternionf().rotateZ((float) Math.PI), null, e);
        } finally {
            e.yBodyRot = bodyRot;
            e.yBodyRotO = bodyRotO;
            e.setYRot(yRot);
            e.yRotO = yRotO;
            e.setXRot(xRot);
            e.xRotO = xRotO;
            e.yHeadRot = headRot;
            e.yHeadRotO = headRotO;
        }
    }

    @Override
    public void drawModel(float x, float y, float scale, float yaw, float pitch,
                          java.util.function.Consumer<dev.coolclient.core.api.ModelRenderer> draw) {
        g.flush();
        com.mojang.blaze3d.vertex.PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(x, y, 150f);
        ps.scale(scale, scale, -scale);
        ps.mulPose(new Quaternionf().rotateX((float) Math.toRadians(pitch)));
        ps.mulPose(new Quaternionf().rotateY((float) Math.toRadians(yaw)));
        com.mojang.blaze3d.platform.Lighting.setupForEntityInInventory();
        net.minecraft.client.renderer.MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        ModelRenderer21 mr = new ModelRenderer21();
        mr.setup(ps, buffers, 0xF000F0);
        try {
            draw.accept(mr);
        } finally {
            buffers.endBatch();
            com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
            ps.popPose();
        }
    }

    @Override public void push() { g.pose().pushPose(); }

    @Override public void pop() { g.pose().popPose(); }

    @Override public void translate(float x, float y) { g.pose().translate(x, y, 0f); }

    @Override public void scale(float sx, float sy) { g.pose().scale(sx, sy, 1f); }

    @Override
    public void scissor(float x, float y, float w, float h) {
        g.flush();
        Matrix4f m = pose();
        m.transformPosition(x, y, 0f, tmp);
        float x1 = tmp.x, y1 = tmp.y;
        m.transformPosition(x + w, y + h, 0f, tmp);
        float x2 = tmp.x, y2 = tmp.y;
        double s = scaleFactor();
        int fbH = Minecraft.getInstance().getWindow().getHeight();
        int px = (int) Math.floor(Math.min(x1, x2) * s);
        int py = (int) Math.floor(fbH - Math.max(y1, y2) * s);
        int pw = (int) Math.ceil(Math.abs(x2 - x1) * s);
        int ph = (int) Math.ceil(Math.abs(y2 - y1) * s);
        RenderSystem.enableScissor(px, py, Math.max(0, pw), Math.max(0, ph));
    }

    @Override
    public void clearScissor() {
        g.flush();
        RenderSystem.disableScissor();
    }

    @Override
    public dev.coolclient.core.api.Gpu gpu() { return Gpu21.INSTANCE.gui(g); }
}
