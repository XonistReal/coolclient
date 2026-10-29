package dev.coolclient.launcher.gl;

import dev.coolclient.core.api.EffectInfo;
import dev.coolclient.core.api.Gpu;
import dev.coolclient.core.api.GuiRenderer;
import dev.coolclient.core.api.ItemRef;
import dev.coolclient.core.font.FontRenderer;
import dev.coolclient.core.font.Fonts;
import dev.coolclient.core.util.Log;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayDeque;

import static org.lwjgl.opengl.GL33C.*;

/**
 * Core's 2D renderer on OpenGL 3.3. Geometry is batched until the texture or scissor changes,
 * so the many small shapes the UI toolkit emits become a handful of draw calls per frame.
 * Transforms are applied on the CPU. Coordinates are GUI units; {@link #scaleFactor()} maps them
 * to framebuffer pixels. It is also the launcher's {@link Gpu}, so the UI's SDF shapes, glass and
 * animated backgrounds run as shaders here exactly as in game.
 */
public final class GlRenderer implements GuiRenderer, Gpu {
    private static final int STRIDE = 20; // pos(2f) uv(2f) color(4ub)
    private static final int CAPACITY = 1 << 16;

    private final GlTextures textures;
    private final ByteBuffer buf = MemoryUtil.memAlloc(CAPACITY * STRIDE);
    private final int program, vao, vbo, uScale, uOffset;
    private int vertices;

    private int fbWidth, fbHeight;
    private float scale = 1f;

    /** Current 2D affine transform: x' = a*x + c*y + tx, y' = b*x + d*y + ty. */
    private float a = 1, b = 0, c = 0, d = 1, tx = 0, ty = 0;
    private final ArrayDeque<float[]> stack = new ArrayDeque<float[]>();

    private int batchTexture = -1;
    private boolean batchLinear;
    private final float[] pending = new float[4 * 5];
    private int pendingCount;
    private boolean textured;

    // Shader draws (Gpu): a separate position-only stream and a backdrop copy for glass.
    private final int shaderVao, shaderVbo;
    private final FloatBuffer shaderBuf = MemoryUtil.memAllocFloat(1024);
    private final float[] guiMatrix = new float[16];
    private int backdrop, backdropW, backdropH;

    public GlRenderer(GlTextures textures) {
        this.textures = textures;
        program = link(
                "#version 330 core\n"
                        + "layout(location=0) in vec2 aPos; layout(location=1) in vec2 aUv; layout(location=2) in vec4 aColor;\n"
                        + "uniform vec2 uScale; uniform vec2 uOffset; out vec2 vUv; out vec4 vColor;\n"
                        + "void main() { gl_Position = vec4(aPos * uScale + uOffset, 0.0, 1.0); vUv = aUv; vColor = aColor; }\n",
                "#version 330 core\n"
                        + "in vec2 vUv; in vec4 vColor; uniform sampler2D uTex; out vec4 frag;\n"
                        + "void main() { frag = texture(uTex, vUv) * vColor; }\n");
        uScale = glGetUniformLocation(program, "uScale");
        uOffset = glGetUniformLocation(program, "uOffset");
        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, (long) CAPACITY * STRIDE, GL_STREAM_DRAW);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, STRIDE, 0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, STRIDE, 8);
        glVertexAttribPointer(2, 4, GL_UNSIGNED_BYTE, true, STRIDE, 16);
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        glEnableVertexAttribArray(2);

        shaderVao = glGenVertexArrays();
        shaderVbo = glGenBuffers();
        glBindVertexArray(shaderVao);
        glBindBuffer(GL_ARRAY_BUFFER, shaderVbo);
        glBufferData(GL_ARRAY_BUFFER, 1024L * 4, GL_STREAM_DRAW);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 8, 0);
        glEnableVertexAttribArray(0);
        glBindVertexArray(vao);
    }

    @Override public Gpu gpu() { return this; }

    /** Starts a frame for a framebuffer of the given size and GUI scale. */
    public void beginFrame(int fbWidth, int fbHeight, float scale) {
        this.fbWidth = fbWidth;
        this.fbHeight = fbHeight;
        this.scale = scale;
        a = d = 1;
        b = c = tx = ty = 0;
        stack.clear();
        glViewport(0, 0, fbWidth, fbHeight);
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glDisable(GL_SCISSOR_TEST);
        glEnable(GL_BLEND);
        glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
        glUseProgram(program);
        glUniform2f(uScale, 2f * scale / fbWidth, -2f * scale / fbHeight);
        glUniform2f(uOffset, -1f, 1f);
        glBindVertexArray(vao);
        glActiveTexture(GL_TEXTURE0);
        batchTexture = -1;
    }

    public void endFrame() {
        flush();
    }

    @Override public int width() { return (int) Math.ceil(fbWidth / scale); }

    @Override public int height() { return (int) Math.ceil(fbHeight / scale); }

    @Override public double scaleFactor() { return scale; }

    // ------------------------------------------------------------------ geometry

    private void use(int texture, boolean linear) {
        if (texture != batchTexture || linear != batchLinear) {
            flush();
            batchTexture = texture;
            batchLinear = linear;
        }
    }

    @Override
    public void beginTriangles() {
        use(textures.white(), false);
        textured = false;
        pendingCount = 0;
    }

    @Override
    public void vertex(float x, float y, int argb) {
        put(x, y, 0.5f, 0.5f, argb);
        if (pendingCount == 3) emit(0, 1, 2);
    }

    @Override
    public void endTriangles() {
        pendingCount = 0;
    }

    @Override
    public void beginTextured(int texture, boolean linear) {
        use(texture, linear);
        textured = true;
        pendingCount = 0;
    }

    @Override
    public void vertexUV(float x, float y, float u, float v, int argb) {
        put(x, y, u, v, argb);
        if (pendingCount == 4) {
            emit(0, 1, 2);
            pendingCount = 4;
            emit(0, 2, 3);
        }
    }

    @Override
    public void endTextured() {
        pendingCount = 0;
    }

    private void put(float x, float y, float u, float v, int argb) {
        int o = pendingCount * 5;
        pending[o] = a * x + c * y + tx;
        pending[o + 1] = b * x + d * y + ty;
        pending[o + 2] = u;
        pending[o + 3] = v;
        pending[o + 4] = Float.intBitsToFloat(argb);
        pendingCount++;
    }

    /** Writes one triangle from the pending vertices and resets the pending list. */
    private void emit(int i0, int i1, int i2) {
        if (vertices + 3 > CAPACITY) flush();
        writeVertex(i0);
        writeVertex(i1);
        writeVertex(i2);
        vertices += 3;
        pendingCount = 0;
    }

    private void writeVertex(int i) {
        int o = i * 5;
        int argb = Float.floatToRawIntBits(pending[o + 4]);
        buf.putFloat(pending[o]).putFloat(pending[o + 1]).putFloat(pending[o + 2]).putFloat(pending[o + 3])
                .put((byte) (argb >> 16)).put((byte) (argb >> 8)).put((byte) argb).put((byte) (argb >>> 24));
    }

    private void flush() {
        if (vertices == 0) return;
        // Always rebind: textures created mid-frame (font atlases, icons) change the binding.
        glBindTexture(GL_TEXTURE_2D, batchTexture);
        int f = batchLinear ? GL_LINEAR : GL_NEAREST;
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, f);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, f);
        buf.flip();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, buf);
        glDrawArrays(GL_TRIANGLES, 0, vertices);
        buf.clear();
        vertices = 0;
    }

    // ------------------------------------------------------------------ game content (none here)

    @Override public void item(ItemRef item, float x, float y, boolean decorations) {}

    @Override public void effectIcon(EffectInfo effect, float x, float y, float size) {}

    @Override public void playerPreview(float x, float y, float size, float yaw, float pitch) {}

    // The launcher has no Minecraft font; "vanilla" text uses the smooth UI font.
    private static FontRenderer textFont() { return Fonts.get(8f); }

    @Override
    public void vanillaText(String text, float x, float y, int argb, boolean shadow) {
        textFont().draw(this, text, x, y, argb, shadow);
    }

    @Override public int vanillaTextWidth(String text) { return (int) Math.ceil(textFont().width(text)); }

    @Override public int vanillaFontHeight() { return 9; }

    // ------------------------------------------------------------------ state

    @Override
    public void push() {
        stack.push(new float[]{a, b, c, d, tx, ty});
    }

    @Override
    public void pop() {
        if (stack.isEmpty()) return;
        float[] m = stack.pop();
        a = m[0]; b = m[1]; c = m[2]; d = m[3]; tx = m[4]; ty = m[5];
    }

    @Override
    public void translate(float x, float y) {
        tx += a * x + c * y;
        ty += b * x + d * y;
    }

    @Override
    public void scale(float sx, float sy) {
        a *= sx;
        b *= sx;
        c *= sy;
        d *= sy;
    }

    @Override
    public void scissor(float x, float y, float w, float h) {
        flush();
        float x0 = a * x + c * y + tx, y0 = b * x + d * y + ty;
        float x1 = a * (x + w) + c * (y + h) + tx, y1 = b * (x + w) + d * (y + h) + ty;
        int px0 = Math.round(Math.min(x0, x1) * scale), px1 = Math.round(Math.max(x0, x1) * scale);
        int py0 = Math.round(Math.min(y0, y1) * scale), py1 = Math.round(Math.max(y0, y1) * scale);
        glEnable(GL_SCISSOR_TEST);
        glScissor(px0, fbHeight - py1, Math.max(0, px1 - px0), Math.max(0, py1 - py0));
    }

    @Override
    public void clearScissor() {
        flush();
        glDisable(GL_SCISSOR_TEST);
    }

    // ------------------------------------------------------------------ Gpu

    @Override public int glslVersion() { return 150; }

    @Override
    public int createProgram(String name, String vertex, String fragment) {
        try {
            return link(vertex, fragment);
        } catch (IllegalStateException e) {
            Log.error("Shader " + name + ": " + e.getMessage(), null);
            return 0;
        }
    }

    @Override public int uniformLocation(int program, String name) { return glGetUniformLocation(program, name); }

    @Override
    public void begin(int program) {
        flush();
        glUseProgram(program);
        glBindVertexArray(shaderVao);
    }

    @Override
    public void end() {
        glUseProgram(this.program);
        glBindVertexArray(vao);
        glActiveTexture(GL_TEXTURE0);
    }

    @Override public void uniform(int location, float x) { glUniform1f(location, x); }

    @Override public void uniform(int location, float x, float y) { glUniform2f(location, x, y); }

    @Override public void uniform(int location, float x, float y, float z, float w) { glUniform4f(location, x, y, z, w); }

    @Override public void uniformInt(int location, int value) { glUniform1i(location, value); }

    @Override public void uniformMatrix(int location, float[] m) { glUniformMatrix4fv(location, false, m); }

    @Override
    public float[] guiMatrix() {
        // clip = ((affine * p) * scale / fb) * 2 - 1, with y flipped.
        float sx = 2f * scale / fbWidth, sy = -2f * scale / fbHeight;
        float[] m = guiMatrix;
        m[0] = a * sx; m[1] = b * sy; m[2] = 0; m[3] = 0;
        m[4] = c * sx; m[5] = d * sy; m[6] = 0; m[7] = 0;
        m[8] = 0; m[9] = 0; m[10] = 1; m[11] = 0;
        m[12] = tx * sx - 1f; m[13] = ty * sy + 1f; m[14] = 0; m[15] = 1;
        return m;
    }

    @Override
    public void draw(float[] xy, int vertexCount) {
        shaderBuf.clear();
        shaderBuf.put(xy, 0, vertexCount * 2).flip();
        glBindBuffer(GL_ARRAY_BUFFER, shaderVbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, shaderBuf);
        glDrawArrays(GL_TRIANGLES, 0, vertexCount);
    }

    @Override
    public void bindTexture(int unit, int glTexture) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, glTexture);
        if (unit != 0) glActiveTexture(GL_TEXTURE0);
        batchTexture = -1;
    }

    @Override public int glTexture(int handle) { return handle; }

    @Override
    public int captureBackdrop() {
        flush();
        if (backdrop == 0 || backdropW != fbWidth || backdropH != fbHeight) {
            if (backdrop == 0) backdrop = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, backdrop);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, fbWidth, fbHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            backdropW = fbWidth;
            backdropH = fbHeight;
        } else {
            glBindTexture(GL_TEXTURE_2D, backdrop);
        }
        glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, fbWidth, fbHeight);
        glGenerateMipmap(GL_TEXTURE_2D);
        batchTexture = -1;
        return backdrop;
    }

    @Override public int framebufferWidth() { return fbWidth; }

    @Override public int framebufferHeight() { return fbHeight; }

    // ------------------------------------------------------------------ shaders

    private static int link(String vs, String fs) {
        int v = compile(GL_VERTEX_SHADER, vs), f = compile(GL_FRAGMENT_SHADER, fs);
        int p = glCreateProgram();
        glAttachShader(p, v);
        glAttachShader(p, f);
        glLinkProgram(p);
        if (glGetProgrami(p, GL_LINK_STATUS) == 0) throw new IllegalStateException("Shader link failed: " + glGetProgramInfoLog(p));
        glDeleteShader(v);
        glDeleteShader(f);
        return p;
    }

    private static int compile(int type, String src) {
        int s = glCreateShader(type);
        glShaderSource(s, src);
        glCompileShader(s);
        if (glGetShaderi(s, GL_COMPILE_STATUS) == 0) throw new IllegalStateException("Shader compile failed: " + glGetShaderInfoLog(s));
        return s;
    }
}
