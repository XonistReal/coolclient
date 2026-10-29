package dev.coolclient.core.render;

import dev.coolclient.core.api.GuiRenderer;

import java.util.ArrayDeque;

/** Nested scissor rectangles (intersected), in GUI coordinates. */
public final class Scissor {
    private static final ArrayDeque<float[]> STACK = new ArrayDeque<float[]>();

    private Scissor() {}

    public static void push(GuiRenderer r, float x, float y, float w, float h) {
        float x2 = x + w, y2 = y + h;
        float[] top = STACK.peek();
        if (top != null) {
            x = Math.max(x, top[0]);
            y = Math.max(y, top[1]);
            x2 = Math.min(x2, top[2]);
            y2 = Math.min(y2, top[3]);
        }
        float[] rect = {x, y, Math.max(x, x2), Math.max(y, y2)};
        STACK.push(rect);
        apply(r, rect);
    }

    public static void pop(GuiRenderer r) {
        STACK.poll();
        float[] top = STACK.peek();
        if (top == null) r.clearScissor(); else apply(r, top);
    }

    /** Safety net: called at the start of each frame. */
    public static void reset(GuiRenderer r) {
        if (!STACK.isEmpty()) {
            STACK.clear();
            r.clearScissor();
        }
    }

    private static void apply(GuiRenderer r, float[] rect) {
        r.scissor(rect[0], rect[1], rect[2] - rect[0], rect[3] - rect[1]);
    }
}
