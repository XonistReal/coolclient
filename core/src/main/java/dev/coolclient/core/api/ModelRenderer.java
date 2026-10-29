package dev.coolclient.core.api;

/**
 * Draws cosmetic meshes in entity model space. The platform positions the pose at the
 * relevant body part before handing it to core; core works in model pixels after calling
 * {@code scale(1/16f, ...)}. +Y points down and +Z points out of the player's back.
 */
public interface ModelRenderer {
    void push();

    void pop();

    void translate(float x, float y, float z);

    void rotate(float degrees, float x, float y, float z);

    void scale(float x, float y, float z);

    /**
     * Starts a quad batch.
     *
     * @param texture  texture handle ({@link Textures#white()} for flat colour)
     * @param emissive render at full brightness (glowing)
     */
    void begin(int texture, boolean emissive);

    /** One vertex of a quad; four consecutive vertices form a quad. */
    void vertex(float x, float y, float z, float u, float v, int argb, float nx, float ny, float nz);

    void end();
}
