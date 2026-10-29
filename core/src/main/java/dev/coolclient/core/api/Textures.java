package dev.coolclient.core.api;

/** Upload ARGB pixel data to GPU textures. Handles are platform-defined positive ints. */
public interface Textures {
    /** The handle of a 1x1 white texture (used for untextured, vertex-coloured geometry). */
    int white();

    int create(String name, int width, int height, int[] argb, boolean linear);

    /** Replace pixels of an existing texture (same dimensions). Must run on the render thread. */
    void update(int handle, int[] argb);

    void delete(int handle);
}
