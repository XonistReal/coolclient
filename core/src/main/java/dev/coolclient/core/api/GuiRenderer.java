package dev.coolclient.core.api;

/**
 * Immediate-mode 2D renderer. Coordinates are in scaled GUI pixels. The platform implements
 * only primitives (coloured triangles, textured quads, items, vanilla text, scissor and a
 * matrix stack); everything else (rounded rects, fonts, icons) is built on top in core.
 */
public interface GuiRenderer {
    int width();

    int height();

    /** Current GUI scale factor (window pixels per GUI pixel). */
    double scaleFactor();

    // ------------------------------------------------------------- coloured geometry

    /** Begin a batch of triangles; every three vertices form one triangle. */
    void beginTriangles();

    void vertex(float x, float y, int argb);

    void endTriangles();

    // ------------------------------------------------------------- textured geometry

    /** Begin textured quads; every four vertices form one quad (counter-clockwise). */
    void beginTextured(int texture, boolean linear);

    void vertexUV(float x, float y, float u, float v, int argb);

    void endTextured();

    // ------------------------------------------------------------- vanilla content

    void item(ItemRef item, float x, float y, boolean decorations);

    void effectIcon(EffectInfo effect, float x, float y, float size);

    void vanillaText(String text, float x, float y, int argb, boolean shadow);

    int vanillaTextWidth(String text);

    int vanillaFontHeight();

    /**
     * Draws the local player model for the cosmetics locker. (x, y) is the point between the
     * feet; {@code size} is the vanilla inventory scale (30 ~ 60px tall). Does nothing outside
     * a world.
     */
    void playerPreview(float x, float y, float size, float yaw, float pitch);

    // ------------------------------------------------------------- state

    void push();

    void pop();

    void translate(float x, float y);

    void scale(float sx, float sy);

    /** Restricts drawing to a rectangle in GUI coordinates (already intersected by core). */
    void scissor(float x, float y, float w, float h);

    void clearScissor();

    // ------------------------------------------------------------- shaders

    /** Shader access for SDF shapes, glass and animated backgrounds, or null to use geometry only. */
    default Gpu gpu() { return null; }
}
