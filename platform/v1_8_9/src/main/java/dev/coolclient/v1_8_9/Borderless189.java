package dev.coolclient.v1_8_9;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.win32.StdCallLibrary;
import dev.coolclient.core.CoolClient;
import dev.coolclient.core.gui.Notifications;
import dev.coolclient.core.util.Log;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

/** Borderless fullscreen on Windows, by restyling the LWJGL2 window through user32. */
final class Borderless189 {
    interface User32 extends StdCallLibrary {
        Pointer FindWindowA(String className, String title);

        int GetWindowLongA(Pointer hwnd, int index);

        int SetWindowLongA(Pointer hwnd, int index, int value);

        boolean SetWindowPos(Pointer hwnd, Pointer after, int x, int y, int cx, int cy, int flags);
    }

    private static final int GWL_STYLE = -16;
    private static final int WS_BORDER_BITS = 0x00C00000 | 0x00040000 | 0x00080000 | 0x00020000 | 0x00010000;
    private static final int SWP_NOZORDER = 0x0004, SWP_FRAMECHANGED = 0x0020, SWP_SHOWWINDOW = 0x0040;
    private static User32 user32;
    private static int savedStyle;
    private static boolean active;

    private Borderless189() {}

    static void set(boolean on) {
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) {
            if (on) CoolClient.get().notifications().post("Borderless Window", "Only supported on Windows", Notifications.Level.WARNING);
            return;
        }
        if (on == active) return;
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (on && mc.isFullscreen()) mc.toggleFullscreen();
            if (user32 == null) user32 = (User32) Native.loadLibrary("user32", User32.class);
            Pointer hwnd = user32.FindWindowA(null, Display.getTitle());
            if (hwnd == null) {
                Log.warn("Borderless: window not found");
                return;
            }
            if (on) {
                savedStyle = user32.GetWindowLongA(hwnd, GWL_STYLE);
                user32.SetWindowLongA(hwnd, GWL_STYLE, savedStyle & ~WS_BORDER_BITS);
                DisplayMode d = Display.getDesktopDisplayMode();
                user32.SetWindowPos(hwnd, null, 0, 0, d.getWidth(), d.getHeight(), SWP_NOZORDER | SWP_FRAMECHANGED | SWP_SHOWWINDOW);
            } else {
                user32.SetWindowLongA(hwnd, GWL_STYLE, savedStyle == 0 ? 0x16CF0000 : savedStyle);
                DisplayMode d = Display.getDesktopDisplayMode();
                int w = Math.min(1280, d.getWidth()), h = Math.min(720, d.getHeight());
                user32.SetWindowPos(hwnd, null, (d.getWidth() - w) / 2, (d.getHeight() - h) / 2, w, h,
                        SWP_NOZORDER | SWP_FRAMECHANGED | SWP_SHOWWINDOW);
            }
            active = on;
        } catch (Throwable t) {
            Log.error("Borderless window toggle failed", t);
        }
    }
}
