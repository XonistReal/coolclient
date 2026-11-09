package dev.coolclient.core.modules.utility;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.event.Events;
import dev.coolclient.core.gui.Notifications;
import dev.coolclient.core.module.Category;
import dev.coolclient.core.module.Module;
import dev.coolclient.core.setting.BoolSetting;
import dev.coolclient.core.util.Log;
import dev.coolclient.core.util.Scheduler;

import java.io.File;
import java.util.Locale;

/**
 * Notices new screenshots (by watching the screenshots folder, so it works the same on every
 * version) and copies them to the clipboard as an image, ready to paste into Discord.
 */
public final class Screenshots extends Module {
    private final BoolSetting copy = add(new BoolSetting("Copy To Clipboard", true));
    private final BoolSetting notify = add(new BoolSetting("Notification", true));
    private long newest = -1;
    private int ticks;

    public Screenshots() {
        super("Screenshots", "Copies each screenshot to your clipboard so you can paste it anywhere.", Category.UTILITY);
        on(Events.Tick.class, e -> {
            if (ticks++ % 10 == 0) scan();
        });
    }

    @Override
    protected void onEnable() {
        newest = latest(); // only screenshots taken from now on
    }

    private File folder() { return new File(platform().gameDir(), "screenshots"); }

    private long latest() {
        long max = 0;
        File[] files = folder().listFiles((d, n) -> n.toLowerCase(Locale.ROOT).endsWith(".png"));
        if (files != null) for (File f : files) max = Math.max(max, f.lastModified());
        return max;
    }

    private void scan() {
        if (newest < 0) newest = latest();
        File[] files = folder().listFiles((d, n) -> n.toLowerCase(Locale.ROOT).endsWith(".png")
                && !n.startsWith("coolclient-selftest") && !n.startsWith("reel-"));
        if (files == null) return;
        File pick = null;
        for (File f : files) {
            if (f.lastModified() > newest && (pick == null || f.lastModified() > pick.lastModified())) pick = f;
        }
        if (pick == null) return;
        newest = pick.lastModified();
        final File shot = pick;
        final boolean tell = notify.on();
        if (!copy.on()) {
            if (tell) CoolClient.get().notifications().info("Screenshot saved", shot.getName());
            return;
        }
        Scheduler.async(() -> {
            boolean ok = copyImage(shot);
            if (tell) Scheduler.main(() -> CoolClient.get().notifications().post(ok ? "Screenshot copied" : "Screenshot saved",
                    ok ? "Paste it anywhere with Ctrl+V" : shot.getName(), ok ? Notifications.Level.SUCCESS : Notifications.Level.INFO));
        });
    }

    /** Puts a PNG on the system clipboard: AWT when available, otherwise the OS's own tool. */
    static boolean copyImage(File png) {
        try {
            if (!java.awt.GraphicsEnvironment.isHeadless()) {
                final java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(png);
                if (img != null) {
                    java.awt.datatransfer.Transferable t = new java.awt.datatransfer.Transferable() {
                        @Override
                        public java.awt.datatransfer.DataFlavor[] getTransferDataFlavors() {
                            return new java.awt.datatransfer.DataFlavor[] {java.awt.datatransfer.DataFlavor.imageFlavor};
                        }

                        @Override
                        public boolean isDataFlavorSupported(java.awt.datatransfer.DataFlavor f) {
                            return java.awt.datatransfer.DataFlavor.imageFlavor.equals(f);
                        }

                        @Override
                        public Object getTransferData(java.awt.datatransfer.DataFlavor f) { return img; }
                    };
                    java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t, null);
                    return true;
                }
            }
        } catch (Throwable t) {
            Log.warn("AWT clipboard unavailable, trying the system tool", t);
        }
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String path = png.getAbsolutePath();
        ProcessBuilder pb;
        if (os.contains("win")) {
            String ps = "Add-Type -AssemblyName System.Windows.Forms,System.Drawing;"
                    + "[System.Windows.Forms.Clipboard]::SetImage([System.Drawing.Image]::FromFile('" + path.replace("'", "''") + "'))";
            pb = new ProcessBuilder("powershell", "-NoProfile", "-STA", "-Command", ps);
        } else if (os.contains("mac")) {
            pb = new ProcessBuilder("osascript", "-e",
                    "set the clipboard to (read (POSIX file \"" + path.replace("\"", "\\\"") + "\") as «class PNGf»)");
        } else {
            pb = new ProcessBuilder("xclip", "-selection", "clipboard", "-t", "image/png", "-i", path);
        }
        try {
            Process p = pb.redirectErrorStream(true).start();
            p.getInputStream().close();
            return p.waitFor() == 0;
        } catch (Exception e) {
            Log.warn("Could not copy the screenshot to the clipboard", e);
            return false;
        }
    }
}
