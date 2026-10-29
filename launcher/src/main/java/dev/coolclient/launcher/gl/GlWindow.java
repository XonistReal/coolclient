package dev.coolclient.launcher.gl;

import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;

/** A borderless, fixed-size GLFW window with an OpenGL 3.3 core context. */
public final class GlWindow {
    public interface Listener {
        void cursor(double x, double y);

        void mouse(int button, boolean pressed, int mods);

        void scroll(double dy);

        void key(int key, int action, int mods);

        void character(int codepoint);
    }

    public final long handle;
    /** Monitor DPI scale (1.0 at 100%). */
    public final float contentScale;
    private double cursorX, cursorY;
    private boolean dragging;
    private double dragX, dragY;

    public GlWindow(String title, int logicalWidth, int logicalHeight, Listener listener) {
        if (!glfwInit()) throw new IllegalStateException("Could not start GLFW");
        long monitor = glfwGetPrimaryMonitor();
        float cs = 1f;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer sx = stack.mallocFloat(1), sy = stack.mallocFloat(1);
            glfwGetMonitorContentScale(monitor, sx, sy);
            cs = Math.max(1f, sx.get(0));
        }
        contentScale = cs;
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        glfwWindowHint(GLFW_DECORATED, GLFW_FALSE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        int w = Math.round(logicalWidth * cs), h = Math.round(logicalHeight * cs);
        handle = glfwCreateWindow(w, h, title, MemoryUtil.NULL, MemoryUtil.NULL);
        if (handle == MemoryUtil.NULL) throw new IllegalStateException("Could not open a window (OpenGL 3.3 required)");
        GLFWVidMode mode = glfwGetVideoMode(monitor);
        if (mode != null) glfwSetWindowPos(handle, (mode.width() - w) / 2, (mode.height() - h) / 2);
        setIcon();

        glfwSetCursorPosCallback(handle, (win, x, y) -> {
            cursorX = x;
            cursorY = y;
            if (dragging) {
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    IntBuffer px = stack.mallocInt(1), py = stack.mallocInt(1);
                    glfwGetWindowPos(handle, px, py);
                    glfwSetWindowPos(handle, px.get(0) + (int) Math.round(x - dragX), py.get(0) + (int) Math.round(y - dragY));
                }
                return;
            }
            listener.cursor(x, y);
        });
        glfwSetMouseButtonCallback(handle, (win, button, action, mods) -> {
            if (action == GLFW_RELEASE && dragging) {
                dragging = false;
                return;
            }
            listener.mouse(button, action == GLFW_PRESS, mods);
        });
        glfwSetScrollCallback(handle, (win, dx, dy) -> listener.scroll(dy));
        glfwSetKeyCallback(handle, (win, key, scancode, action, mods) -> listener.key(key, action, mods));
        glfwSetCharCallback(handle, (win, cp) -> listener.character(cp));

        glfwMakeContextCurrent(handle);
        GL.createCapabilities();
        glfwSwapInterval(1);
    }

    /** Starts moving the window with the cursor until the button is released. */
    public void beginDrag() {
        dragging = true;
        dragX = cursorX;
        dragY = cursorY;
    }

    public int framebufferWidth() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1), h = stack.mallocInt(1);
            glfwGetFramebufferSize(handle, w, h);
            return w.get(0);
        }
    }

    public int framebufferHeight() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1), h = stack.mallocInt(1);
            glfwGetFramebufferSize(handle, w, h);
            return h.get(0);
        }
    }

    /** Cursor position in framebuffer pixels. */
    public double cursorX() { return cursorX * framebufferWidth() / Math.max(1, windowWidth()); }

    public double cursorY() { return cursorY * framebufferHeight() / Math.max(1, windowHeight()); }

    private int windowWidth() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1), h = stack.mallocInt(1);
            glfwGetWindowSize(handle, w, h);
            return w.get(0);
        }
    }

    private int windowHeight() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1), h = stack.mallocInt(1);
            glfwGetWindowSize(handle, w, h);
            return h.get(0);
        }
    }

    public boolean shouldClose() { return glfwWindowShouldClose(handle); }

    public void requestClose() { glfwSetWindowShouldClose(handle, true); }

    public boolean iconified() { return glfwGetWindowAttrib(handle, GLFW_ICONIFIED) == GLFW_TRUE; }

    public boolean visible() { return glfwGetWindowAttrib(handle, GLFW_VISIBLE) == GLFW_TRUE; }

    public void show() {
        glfwShowWindow(handle);
        glfwRestoreWindow(handle);
        glfwFocusWindow(handle);
    }

    public void hide() { glfwHideWindow(handle); }

    public void minimize() { glfwIconifyWindow(handle); }

    public void swap() { glfwSwapBuffers(handle); }

    public void destroy() {
        glfwDestroyWindow(handle);
        glfwTerminate();
    }

    private void setIcon() {
        if (glfwGetPlatform() == GLFW_PLATFORM_COCOA) return; // macOS uses the app bundle icon
        try (InputStream in = GlWindow.class.getResourceAsStream("/assets/coolclient/launcher-icon.png")) {
            if (in == null) return;
            BufferedImage img = ImageIO.read(in);
            int w = img.getWidth(), h = img.getHeight();
            int[] px = img.getRGB(0, 0, w, h, null, 0, w);
            ByteBuffer buf = MemoryUtil.memAlloc(w * h * 4);
            for (int c : px) buf.put((byte) (c >> 16)).put((byte) (c >> 8)).put((byte) c).put((byte) (c >>> 24));
            buf.flip();
            try (GLFWImage.Buffer icons = GLFWImage.malloc(1)) {
                icons.position(0).width(w).height(h).pixels(buf);
                glfwSetWindowIcon(handle, icons);
            } finally {
                MemoryUtil.memFree(buf);
            }
        } catch (Exception ignored) {
            // cosmetic only
        }
    }
}
