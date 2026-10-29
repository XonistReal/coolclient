package dev.coolclient.core.util;

import java.util.HashMap;
import java.util.Map;

/**
 * GLFW key codes (the canonical key space in core) plus mouse buttons encoded as
 * {@code MOUSE_BASE + button}. The 1.8.9 platform translates LWJGL2 codes into this space.
 */
public final class Keys {
    public static final int NONE = -1;
    public static final int MOUSE_BASE = 400;

    public static final int SPACE = 32, APOSTROPHE = 39, COMMA = 44, MINUS = 45, PERIOD = 46, SLASH = 47;
    public static final int N0 = 48, N1 = 49, N2 = 50, N3 = 51, N4 = 52, N5 = 53, N6 = 54, N7 = 55, N8 = 56, N9 = 57;
    public static final int SEMICOLON = 59, EQUAL = 61;
    public static final int A = 65, B = 66, C = 67, D = 68, E = 69, F = 70, G = 71, H = 72, I = 73, J = 74, K = 75,
            L = 76, M = 77, N = 78, O = 79, P = 80, Q = 81, R = 82, S = 83, T = 84, U = 85, V = 86, W = 87, X = 88,
            Y = 89, Z = 90;
    public static final int LEFT_BRACKET = 91, BACKSLASH = 92, RIGHT_BRACKET = 93, GRAVE = 96;
    public static final int ESCAPE = 256, ENTER = 257, TAB = 258, BACKSPACE = 259, INSERT = 260, DELETE = 261;
    public static final int RIGHT = 262, LEFT = 263, DOWN = 264, UP = 265, PAGE_UP = 266, PAGE_DOWN = 267;
    public static final int HOME = 268, END = 269, CAPS_LOCK = 280, SCROLL_LOCK = 281, NUM_LOCK = 282;
    public static final int PRINT_SCREEN = 283, PAUSE = 284;
    public static final int F1 = 290, F2 = 291, F3 = 292, F4 = 293, F5 = 294, F6 = 295, F7 = 296, F8 = 297,
            F9 = 298, F10 = 299, F11 = 300, F12 = 301;
    public static final int KP_0 = 320, KP_9 = 329, KP_DECIMAL = 330, KP_DIVIDE = 331, KP_MULTIPLY = 332,
            KP_SUBTRACT = 333, KP_ADD = 334, KP_ENTER = 335, KP_EQUAL = 336;
    public static final int LEFT_SHIFT = 340, LEFT_CONTROL = 341, LEFT_ALT = 342, LEFT_SUPER = 343;
    public static final int RIGHT_SHIFT = 344, RIGHT_CONTROL = 345, RIGHT_ALT = 346, RIGHT_SUPER = 347, MENU = 348;

    private static final Map<Integer, String> NAMES = new HashMap<Integer, String>();

    static {
        for (int c = A; c <= Z; c++) NAMES.put(c, String.valueOf((char) c));
        for (int c = N0; c <= N9; c++) NAMES.put(c, String.valueOf((char) c));
        for (int i = 0; i < 12; i++) NAMES.put(F1 + i, "F" + (i + 1));
        for (int i = 0; i <= 9; i++) NAMES.put(KP_0 + i, "NUM " + i);
        NAMES.put(SPACE, "SPACE");
        NAMES.put(APOSTROPHE, "'");
        NAMES.put(COMMA, ",");
        NAMES.put(MINUS, "-");
        NAMES.put(PERIOD, ".");
        NAMES.put(SLASH, "/");
        NAMES.put(SEMICOLON, ";");
        NAMES.put(EQUAL, "=");
        NAMES.put(LEFT_BRACKET, "[");
        NAMES.put(BACKSLASH, "\\");
        NAMES.put(RIGHT_BRACKET, "]");
        NAMES.put(GRAVE, "`");
        NAMES.put(ESCAPE, "ESC");
        NAMES.put(ENTER, "ENTER");
        NAMES.put(TAB, "TAB");
        NAMES.put(BACKSPACE, "BACK");
        NAMES.put(INSERT, "INSERT");
        NAMES.put(DELETE, "DELETE");
        NAMES.put(RIGHT, "RIGHT");
        NAMES.put(LEFT, "LEFT");
        NAMES.put(DOWN, "DOWN");
        NAMES.put(UP, "UP");
        NAMES.put(PAGE_UP, "PG UP");
        NAMES.put(PAGE_DOWN, "PG DOWN");
        NAMES.put(HOME, "HOME");
        NAMES.put(END, "END");
        NAMES.put(CAPS_LOCK, "CAPS");
        NAMES.put(SCROLL_LOCK, "SCROLL");
        NAMES.put(NUM_LOCK, "NUMLOCK");
        NAMES.put(PRINT_SCREEN, "PRTSC");
        NAMES.put(PAUSE, "PAUSE");
        NAMES.put(KP_DECIMAL, "NUM .");
        NAMES.put(KP_DIVIDE, "NUM /");
        NAMES.put(KP_MULTIPLY, "NUM *");
        NAMES.put(KP_SUBTRACT, "NUM -");
        NAMES.put(KP_ADD, "NUM +");
        NAMES.put(KP_ENTER, "NUM ENTER");
        NAMES.put(LEFT_SHIFT, "LSHIFT");
        NAMES.put(LEFT_CONTROL, "LCTRL");
        NAMES.put(LEFT_ALT, "LALT");
        NAMES.put(LEFT_SUPER, "LWIN");
        NAMES.put(RIGHT_SHIFT, "RSHIFT");
        NAMES.put(RIGHT_CONTROL, "RCTRL");
        NAMES.put(RIGHT_ALT, "RALT");
        NAMES.put(RIGHT_SUPER, "RWIN");
        NAMES.put(MENU, "MENU");
    }

    private Keys() {}

    public static int mouse(int button) { return MOUSE_BASE + button; }

    public static boolean isMouse(int code) { return code >= MOUSE_BASE && code < MOUSE_BASE + 16; }

    public static String name(int code) {
        if (code == NONE) return "NONE";
        if (isMouse(code)) {
            int b = code - MOUSE_BASE;
            if (b == 0) return "LMB";
            if (b == 1) return "RMB";
            if (b == 2) return "MMB";
            return "MOUSE " + (b + 1);
        }
        String n = NAMES.get(code);
        return n != null ? n : ("KEY " + code);
    }
}
