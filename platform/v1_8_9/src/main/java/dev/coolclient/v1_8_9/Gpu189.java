package dev.coolclient.v1_8_9;

import com.mojang.blaze3d.platform.GlStateManager;
import dev.coolclient.core.api.Gpu;
import dev.coolclient.core.util.Log;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/**
 * Core's shader access on 1.8.9 (legacy LWJGL 2 context, GLSL 120). GUI shaders read the
 * fixed-function matrices, so no matrix maths is needed here. Vanilla draws from client-side
 * arrays, so the array buffer and our attribute array are unbound again after every draw.
 */
final class Gpu189 implements Gpu {
    static final Gpu189 INSTANCE = new Gpu189();

    private final FloatBuffer buf = BufferUtils.createFloatBuffer(256);
    private final FloatBuffer matrix = BufferUtils.createFloatBuffer(16);
    private int vbo;
    private int backdrop, backdropW, backdropH;
    private Boolean supported;
    private boolean world;

    private Gpu189() {}

    /** Null when the driver lacks OpenGL 2.0 (then core draws geometry instead). */
    Gpu189 gui() {
        world = false;
        return supported() ? this : null;
    }

    /** For world drawing (sky): depth test and writes are off for the draw. */
    Gpu189 world() {
        world = true;
        return supported() ? this : null;
    }

    private boolean supported() {
        if (supported == null) {
            ContextCapabilities caps = GLContext.getCapabilities();
            supported = caps.OpenGL20;
            if (!supported) Log.warn("OpenGL 2.0 not available; using the geometry UI renderer");
        }
        return supported;
    }

    @Override public int glslVersion() { return 120; }

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
            Log.error("Shader " + name + " failed to link: " + GL20.glGetProgramInfoLog(p, 4096), null);
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
            Log.error("Shader " + name + " failed to compile: " + GL20.glGetShaderInfoLog(s, 4096), null);
            GL20.glDeleteShader(s);
            return 0;
        }
        return s;
    }

    @Override public int uniformLocation(int program, String name) { return GL20.glGetUniformLocation(program, name); }

    @Override
    public void begin(int program) {
        if (vbo == 0) vbo = GL15.glGenBuffers();
        GlStateManager.enableBlend();
        GlStateManager.blendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableAlphaTest();
        GlStateManager.disableCull();
        if (world) {
            GlStateManager.disableDepthTest();
            GlStateManager.depthMask(false);
        }
        GL20.glUseProgram(program);
    }

    @Override
    public void end() {
        GL20.glUseProgram(0);
        if (world) {
            GlStateManager.depthMask(true);
            GlStateManager.enableDepthTest();
        }
        GlStateManager.enableCull();
        GlStateManager.enableAlphaTest();
        GlStateManager.disableBlend();
    }

    @Override public void uniform(int location, float x) { GL20.glUniform1f(location, x); }

    @Override public void uniform(int location, float x, float y) { GL20.glUniform2f(location, x, y); }

    @Override public void uniform(int location, float x, float y, float z, float w) { GL20.glUniform4f(location, x, y, z, w); }

    @Override public void uniformInt(int location, int value) { GL20.glUniform1i(location, value); }

    @Override
    public void uniformMatrix(int location, float[] m) {
        matrix.clear();
        matrix.put(m).flip();
        GL20.glUniformMatrix4(location, false, matrix);
    }

    /** GLSL 120 shaders use gl_ModelViewProjectionMatrix. */
    @Override public float[] guiMatrix() { return null; }

    @Override
    public void draw(float[] xy, int vertexCount) {
        buf.clear();
        buf.put(xy, 0, vertexCount * 2).flip();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buf, GL15.GL_STREAM_DRAW);
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 8, 0L);
        GL20.glEnableVertexAttribArray(0);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, vertexCount);
        GL20.glDisableVertexAttribArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    @Override
    public void bindTexture(int unit, int glTexture) {
        GlStateManager.activeTexture(GL13.GL_TEXTURE0 + unit);
        GlStateManager.bindTexture(glTexture);
        GlStateManager.activeTexture(GL13.GL_TEXTURE0);
    }

    @Override public int glTexture(int handle) { return Platform189.TEXTURES.glId(handle); }

    @Override
    public int captureBackdrop() {
        ContextCapabilities caps = GLContext.getCapabilities();
        if (!caps.OpenGL30 && !caps.GL_EXT_framebuffer_object) return 0;
        MinecraftClient mc = MinecraftClient.getInstance();
        int fw = mc.width, fh = mc.height;
        GlStateManager.activeTexture(GL13.GL_TEXTURE0);
        if (backdrop == 0 || backdropW != fw || backdropH != fh) {
            if (backdrop == 0) backdrop = GL11.glGenTextures();
            GlStateManager.bindTexture(backdrop);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, fw, fh, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            backdropW = fw;
            backdropH = fh;
        } else {
            GlStateManager.bindTexture(backdrop);
        }
        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, fw, fh);
        if (caps.OpenGL30) GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
        else EXTFramebufferObject.glGenerateMipmapEXT(GL11.GL_TEXTURE_2D);
        return backdrop;
    }

    @Override public int framebufferWidth() { return MinecraftClient.getInstance().width; }

    @Override public int framebufferHeight() { return MinecraftClient.getInstance().height; }
}
