package dev.coolclient.core.cosmetic.cape;

/** A procedural cape design. */
public interface CapeStyle {
    /**
     * Colour of the cape's outer face at (u, v), where u runs left to right and v top (shoulders)
     * to bottom, both in [0,1], at time t (seconds). {@code tint} is the player's chosen colour.
     */
    int sample(float u, float v, float t, int tint);

    /** Bumped when the design changes outside of time (an image finished downloading), so still capes get repainted. */
    default int revision() { return 0; }
}
