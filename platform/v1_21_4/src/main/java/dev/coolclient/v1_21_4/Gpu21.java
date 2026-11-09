package dev.coolclient.v1_21_4;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import dev.coolclient.core.api.Gpu;
import dev.coolclient.core.util.Log;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.FloatBuffer;

/**
 * Core's shader access on 1.21.4 (OpenGL 3.2 core). Raw GL calls go through GlStateManager where
 * it caches state (textures, blend, depth), and BufferUploader is invalidated after our VAO is
 * bound, so vanilla rendering rebinds its own buffers afterwards.
 */
public final class Gpu21 implements Gpu {
    public static final Gpu21 INSTANCE = new Gpu21();

    private final FloatBuffer buf = BufferUtils.createFloatBuffer(256);
    private final Matrix4f matrix = new Matrix4f();
    private final float[] matrixArray = new float[16];
    private int vao, vbo;
    private int backdrop, backdropW, backdropH;
    /** The GUI being drawn, flushed before each shader draw; null for world drawing (sky). */
    private GuiGraphics gui;
    private boolean depthWasOn;

    private Gpu21() {}

    Gpu21 gui(GuiGraphics g) {
        this.gui = g;
        return this;
    }

    public Gpu21 world() {
        this.gui = null;
        return this;
    }

    @Override public int glslVersion() { return 150; }

    @Override
    public int createProgram(String name, String vertex, String fragment) {
        int vs = compile(name, GL20.GL_VERTEX_SHADER, vertex), fs = compile(name, GL20.GL_FRAGMENT_SHADER, fragment);
        if (vs == 0 || fs == 0) return 0;
        int p = GL20.glCreateProgram();
        GL20.glAttachShader(p, vs);
        GL20.glAttachShader(p, fs);
        GL20.glBindAttribLocation(p, 0, "aPos");
        GL20.glLinkProgram(p);
        GL20.glDeleteShader(vs);
        GL20.glDeleteShader(fs);
        if (GL20.glGetProgrami(p, GL20.GL_LINK_STATUS) == 0) {
            Log.error("Shader " + name + " failed to link: " + GL20.glGetProgramInfoLog(p), null);
            GL20.glDeleteProgram(p);
            return 0;
        }
        return p;
    }

    private static int compile(String name, int type, String src) {
        int s = GL20.glCreateShader(type);
        GL20.glShaderSource(s, src);
        GL20.glCompileShader(s);
        if (GL20.glGetShaderi(s, GL20.GL_COMPILE_STATUS) == 0) {
            Log.error("Shader " + name + " failed to compile: " + GL20.glGetShaderInfoLog(s), null);
            GL20.glDeleteShader(s);
            return 0;
        }
        return s;
    }

    @Override public int uniformLocation(int program, String name) { return GL20.glGetUniformLocation(program, name); }

    @Override
    public void begin(int program) {
        if (gui != null) gui.flush();
        if (vao == 0) {
            vao = GlStateManager._glGenVertexArrays();
            vbo = GlStateManager._glGenBuffers();
            GlStateManager._glBindVertexArray(vao);
            GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            GlStateManager._glBufferData(GL15.GL_ARRAY_BUFFER, 256L * 4, GL15.GL_STREAM_DRAW);
            GlStateManager._enableVertexAttribArray(0);
            GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 8, 0L);
        }
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableCull();
        depthWasOn = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        GlStateManager._glUseProgram(program);
        GlStateManager._glBindVertexArray(vao);
    }

    @Override
    public void end() {
        GlStateManager._glUseProgram(0);
        GlStateManager._glBindVertexArray(0);
        BufferUploader.invalidate();
        GlStateManager._activeTexture(GL20.GL_TEXTURE0);
        RenderSystem.depthMask(true);
        if (depthWasOn || gui != null) RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    @Override public void uniform(int location, float x) { GL20.glUniform1f(location, x); }

    @Override public void uniform(int location, float x, float y) { GL20.glUniform2f(location, x, y); }

    @Override public void uniform(int location, float x, float y, float z, float w) { GL20.glUniform4f(location, x, y, z, w); }

    @Override public void uniformInt(int location, int value) { GL20.glUniform1i(location, value); }

    @Override public void uniformMatrix(int location, float[] m) { GL20.glUniformMatrix4fv(location, false, m); }

    @Override
    public float[] guiMatrix() {
        matrix.set(RenderSystem.getProjectionMatrix()).mul(RenderSystem.getModelViewMatrix());
        if (gui != null) matrix.mul(gui.pose().last().pose());
        return matrix.get(matrixArray);
    }

    @Override
    public void draw(float[] xy, int vertexCount) {
        buf.clear();
        buf.put(xy, 0, vertexCount * 2).flip();
        GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0L, buf);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, vertexCount);
    }

    @Override
    public void bindTexture(int unit, int glTexture) {
        GlStateManager._activeTexture(GL20.GL_TEXTURE0 + unit);
        GlStateManager._bindTexture(glTexture);
        GlStateManager._activeTexture(GL20.GL_TEXTURE0);
    }

    @Override public int glTexture(int handle) { return Platform21.TEXTURES.glId(handle); }

    private final java.util.Map<Integer, int[]> frames = new java.util.HashMap<Integer, int[]>();

    @Override
    public int copyFrame(int slot) {
        if (gui != null) gui.flush();
        Window w = Minecraft.getInstance().getWindow();
        int fw = w.getWidth(), fh = w.getHeight();
        int[] f = frames.get(slot); // {texture, width, height}
        GlStateManager._activeTexture(GL20.GL_TEXTURE0);
        if (f == null || f[1] != fw || f[2] != fh) {
            if (f == null) {
                f = new int[] {GlStateManager._genTexture(), 0, 0};
                frames.put(slot, f);
            }
            GlStateManager._bindTexture(f[0]);
            GlStateManager._texImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, fw, fh, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, null);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL15.GL_CLAMP_TO_EDGE);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL15.GL_CLAMP_TO_EDGE);
            f[1] = fw;
            f[2] = fh;
        } else {
            GlStateManager._bindTexture(f[0]);
        }
        GlStateManager._glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, fw, fh);
        return f[0];
    }

    @Override
    public int keptFrame(int slot) {
        int[] f = frames.get(slot);
        Window w = Minecraft.getInstance().getWindow();
        // A resize leaves the old copy at the wrong size; treat it as missing.
        return f == null || f[1] != w.getWidth() || f[2] != w.getHeight() ? 0 : f[0];
    }

    @Override
    public int captureBackdrop() {
        if (gui != null) gui.flush();
        Window w = Minecraft.getInstance().getWindow();
        int fw = w.getWidth(), fh = w.getHeight();
        GlStateManager._activeTexture(GL20.GL_TEXTURE0);
        if (backdrop == 0 || backdropW != fw || backdropH != fh) {
            if (backdrop == 0) backdrop = GlStateManager._genTexture();
            GlStateManager._bindTexture(backdrop);
            GlStateManager._texImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, fw, fh, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, null);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL15.GL_CLAMP_TO_EDGE);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL15.GL_CLAMP_TO_EDGE);
            backdropW = fw;
            backdropH = fh;
        } else {
            GlStateManager._bindTexture(backdrop);
        }
        GlStateManager._glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, fw, fh);
        GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
        return backdrop;
    }

    @Override public int framebufferWidth() { return Minecraft.getInstance().getWindow().getWidth(); }

    @Override public int framebufferHeight() { return Minecraft.getInstance().getWindow().getHeight(); }
}
