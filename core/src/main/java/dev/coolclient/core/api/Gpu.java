package dev.coolclient.core.api;

/**
 * Raw shader access for the GPU-drawn UI and skies. Platforms implement this thin layer over
 * their OpenGL binding; every program, uniform and draw decision lives in core
 * ({@code dev.coolclient.core.render.gpu}). All calls happen on the render thread.
 */
public interface Gpu {
    /** GLSL dialect: 120 (legacy context, fixed-function matrices) or 150 (core profile). */
    int glslVersion();

    /** Compiles and links a program with attribute 0 bound to {@code aPos}. Returns 0 (and logs) on failure. */
    int createProgram(String name, String vertex, String fragment);

    int uniformLocation(int program, String name);

    /** Starts a shader draw: flushes pending platform geometry, binds the program, enables blending. */
    void begin(int program);

    /** Restores the platform's own GL state after {@link #begin}. */
    void end();

    void uniform(int location, float x);

    void uniform(int location, float x, float y);

    void uniform(int location, float x, float y, float z, float w);

    void uniformInt(int location, int value);

    /** Column-major 4x4 matrix. */
    void uniformMatrix(int location, float[] m);

    /**
     * The matrix taking current GUI coordinates (including the renderer's push/translate/scale
     * stack) to clip space, column-major. Null on GLSL 120, where shaders read
     * {@code gl_ModelViewProjectionMatrix} instead.
     */
    float[] guiMatrix();

    /** Draws {@code vertexCount} vertices of {@code xy} pairs (attribute 0) as triangles. */
    void draw(float[] xy, int vertexCount);

    /** Binds a GL texture name to a texture unit (and leaves unit 0 active). */
    void bindTexture(int unit, int glTexture);

    /** The GL texture name behind a core {@link Textures} handle. */
    int glTexture(int handle);

    /**
     * Copies what has been drawn so far this frame into a mipmapped texture (for frosted glass)
     * and returns its GL name, or 0 when the driver can't. Called at most once per frame.
     */
    int captureBackdrop();

    int framebufferWidth();

    int framebufferHeight();

    /**
     * Copies the frame so far into a plain texture kept for {@code slot} (no mipmaps), for post
     * effects that need an earlier frame (motion blur). Returns its GL name, or 0 if unsupported.
     */
    default int copyFrame(int slot) { return 0; }

    /** The texture {@link #copyFrame} last filled for {@code slot}, without copying again (0 if none). */
    default int keptFrame(int slot) { return 0; }
}
