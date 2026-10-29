package dev.coolclient.v1_8_9;

import dev.coolclient.core.util.Keys;

/** Translates LWJGL2 key codes (1.8.9) to GLFW key codes (core) and back. */
public final class KeyMap {
    private static final int[] TO_GLFW = new int[256];
    private static final int[] TO_LWJGL = new int[512];

    private static void map(int lwjgl, int glfw) {
        TO_GLFW[lwjgl] = glfw;
        TO_LWJGL[glfw] = lwjgl;
    }

    static {
        java.util.Arrays.fill(TO_GLFW, Keys.NONE);
        map(1, Keys.ESCAPE);
        map(2, Keys.N1); map(3, Keys.N2); map(4, Keys.N3); map(5, Keys.N4); map(6, Keys.N5);
        map(7, Keys.N6); map(8, Keys.N7); map(9, Keys.N8); map(10, Keys.N9); map(11, Keys.N0);
        map(12, Keys.MINUS); map(13, Keys.EQUAL); map(14, Keys.BACKSPACE); map(15, Keys.TAB);
        map(16, Keys.Q); map(17, Keys.W); map(18, Keys.E); map(19, Keys.R); map(20, Keys.T);
        map(21, Keys.Y); map(22, Keys.U); map(23, Keys.I); map(24, Keys.O); map(25, Keys.P);
        map(26, Keys.LEFT_BRACKET); map(27, Keys.RIGHT_BRACKET); map(28, Keys.ENTER); map(29, Keys.LEFT_CONTROL);
        map(30, Keys.A); map(31, Keys.S); map(32, Keys.D); map(33, Keys.F); map(34, Keys.G);
        map(35, Keys.H); map(36, Keys.J); map(37, Keys.K); map(38, Keys.L); map(39, Keys.SEMICOLON);
        map(40, Keys.APOSTROPHE); map(41, Keys.GRAVE); map(42, Keys.LEFT_SHIFT); map(43, Keys.BACKSLASH);
        map(44, Keys.Z); map(45, Keys.X); map(46, Keys.C); map(47, Keys.V); map(48, Keys.B);
        map(49, Keys.N); map(50, Keys.M); map(51, Keys.COMMA); map(52, Keys.PERIOD); map(53, Keys.SLASH);
        map(54, Keys.RIGHT_SHIFT); map(55, Keys.KP_MULTIPLY); map(56, Keys.LEFT_ALT); map(57, Keys.SPACE);
        map(58, Keys.CAPS_LOCK);
        for (int i = 0; i < 10; i++) map(59 + i, Keys.F1 + i);
        map(69, Keys.NUM_LOCK); map(70, Keys.SCROLL_LOCK);
        map(71, Keys.KP_0 + 7); map(72, Keys.KP_0 + 8); map(73, Keys.KP_0 + 9); map(74, Keys.KP_SUBTRACT);
        map(75, Keys.KP_0 + 4); map(76, Keys.KP_0 + 5); map(77, Keys.KP_0 + 6); map(78, Keys.KP_ADD);
        map(79, Keys.KP_0 + 1); map(80, Keys.KP_0 + 2); map(81, Keys.KP_0 + 3); map(82, Keys.KP_0);
        map(83, Keys.KP_DECIMAL); map(87, Keys.F11); map(88, Keys.F12);
        map(156, Keys.KP_ENTER); map(157, Keys.RIGHT_CONTROL); map(181, Keys.KP_DIVIDE);
        map(183, Keys.PRINT_SCREEN); map(184, Keys.RIGHT_ALT); map(197, Keys.PAUSE);
        map(199, Keys.HOME); map(200, Keys.UP); map(201, Keys.PAGE_UP); map(203, Keys.LEFT);
        map(205, Keys.RIGHT); map(207, Keys.END); map(208, Keys.DOWN); map(209, Keys.PAGE_DOWN);
        map(210, Keys.INSERT); map(211, Keys.DELETE); map(219, Keys.LEFT_SUPER); map(220, Keys.RIGHT_SUPER);
    }

    private KeyMap() {}

    public static int toGlfw(int lwjgl) {
        return lwjgl >= 0 && lwjgl < TO_GLFW.length ? TO_GLFW[lwjgl] : Keys.NONE;
    }

    public static int toLwjgl(int glfw) {
        return glfw >= 0 && glfw < TO_LWJGL.length ? TO_LWJGL[glfw] : 0;
    }

    /** Converts a 1.8.9 KeyBinding code (mouse buttons are negative, button - 100). */
    public static int bindingToCore(int code) {
        if (code < 0) return Keys.mouse(code + 100);
        return toGlfw(code);
    }
}
