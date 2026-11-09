package dev.coolclient.core.api;

/**
 * Immediate-mode drawing in the world, in world coordinates (the platform subtracts the camera).
 * Used from {@link dev.coolclient.core.event.Events.RenderWorld}, after the level and before the hand.
 * Batches: begin with {@link #lines} or {@link #quads}, add vertices, then {@link #end}.
 */
public interface WorldDraw {
    double cameraX();

    double cameraY();

    double cameraZ();

    /** Starts a batch of line segments (two vertices each). */
    void lines(boolean throughWalls);

    /** Starts a batch of quads (four vertices each), blended, both faces drawn. */
    void quads(boolean throughWalls);

    void vertex(double x, double y, double z, int argb);

    void end();

    /**
     * Text facing the camera, centred on (x, y, z). {@code scale} is world units per font pixel
     * (vanilla nametags use 0.025). {@code background} is an ARGB plate colour (0 for none).
     */
    void text(String text, double x, double y, double z, float scale, int argb, boolean throughWalls, int background);
}
