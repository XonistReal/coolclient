package dev.coolclient.core.api;

/** Polling input state. All key codes are GLFW codes (see {@link dev.coolclient.core.util.Keys}). */
public interface Input {
    boolean isKeyDown(int glfwKey);

    boolean isMouseDown(int button);

    default boolean shift() { return isKeyDown(340) || isKeyDown(344); }

    default boolean ctrl() { return isKeyDown(341) || isKeyDown(345); }

    default boolean alt() { return isKeyDown(342) || isKeyDown(346); }
}
