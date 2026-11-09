package dev.coolclient.core.modules.utility;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.event.Events;
import dev.coolclient.core.gui.Notifications;
import dev.coolclient.core.module.Category;
import dev.coolclient.core.module.Module;
import dev.coolclient.core.setting.BoolSetting;
import dev.coolclient.core.setting.NumberSetting;
import dev.coolclient.core.util.Log;
import dev.coolclient.core.util.Scheduler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Zips a singleplayer world when you leave it (once the game has saved it) into
 * {@code coolclient/backups/<world>/}, keeping the newest few.
 */
public final class WorldBackups extends Module {
    private final NumberSetting keep = add(new NumberSetting("Keep Last", 5, 1, 30, 1).suffix(" backups"));
    private final NumberSetting maxSize = add(new NumberSetting("Skip Worlds Over", 2048, 128, 16384, 128).suffix(" MB"));
    private final BoolSetting notify = add(new BoolSetting("Notify When Done", true));
    private File world;
    private static volatile boolean running;

    public WorldBackups() {
        super("World Backups", "Backs up singleplayer worlds to a zip each time you leave one.", Category.UTILITY);
        on(Events.WorldJoin.class, e -> world = e.server == null ? platform().singleplayerWorldDir() : null);
        on(Events.WorldLeave.class, e -> {
            final File dir = world;
            world = null;
            if (dir != null && dir.isDirectory()) start(dir);
        });
    }

    public static File backupRoot() {
        return new File(CoolClient.get().platform().gameDir(), "coolclient/backups");
    }

    private void start(final File dir) {
        if (running) return;
        running = true;
        final int keepN = keep.i();
        final long limit = (long) maxSize.d() << 20;
        final boolean tell = notify.on();
        Scheduler.async(() -> {
            try {
                // The integrated server finishes saving as the world closes; give it a moment.
                Thread.sleep(1500);
                long size = size(dir);
                if (size > limit) {
                    Log.info("World backup skipped: " + dir.getName() + " is " + (size >> 20) + " MB");
                    return;
                }
                File out = new File(new File(backupRoot(), dir.getName()),
                        new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()) + ".zip");
                out.getParentFile().mkdirs();
                File tmp = new File(out.getPath() + ".part");
                try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(tmp))) {
                    add(zip, dir, dir.getName() + "/");
                }
                if (!tmp.renameTo(out)) throw new IOException("Could not rename " + tmp);
                prune(out.getParentFile(), keepN);
                Log.info("World backup written: " + out);
                if (tell) {
                    final String msg = dir.getName() + " (" + Math.max(1, out.length() >> 20) + " MB)";
                    Scheduler.main(() -> CoolClient.get().notifications().post("World backed up", msg, Notifications.Level.SUCCESS));
                }
            } catch (Throwable t) {
                Log.error("World backup failed for " + dir.getName(), t);
                Scheduler.main(() -> CoolClient.get().notifications().post("Backup failed", dir.getName(), Notifications.Level.ERROR));
            } finally {
                running = false;
            }
        });
    }

    private static long size(File f) {
        if (f.isFile()) return f.length();
        long total = 0;
        File[] kids = f.listFiles();
        if (kids != null) for (File k : kids) total += size(k);
        return total;
    }

    private static void add(ZipOutputStream zip, File dir, String prefix) throws IOException {
        File[] kids = dir.listFiles();
        if (kids == null) return;
        byte[] buf = new byte[64 * 1024];
        for (File k : kids) {
            if (k.isDirectory()) {
                add(zip, k, prefix + k.getName() + "/");
                continue;
            }
            if (k.getName().equals("session.lock")) continue;
            zip.putNextEntry(new ZipEntry(prefix + k.getName()));
            try (InputStream in = new FileInputStream(k)) {
                int n;
                while ((n = in.read(buf)) > 0) zip.write(buf, 0, n);
            }
            zip.closeEntry();
        }
    }

    private static void prune(File folder, int keep) {
        File[] zips = folder.listFiles((d, n) -> n.endsWith(".zip"));
        if (zips == null || zips.length <= keep) return;
        Arrays.sort(zips, Comparator.comparing(File::getName)); // timestamps sort by name
        for (int i = 0; i < zips.length - keep; i++) {
            if (!zips[i].delete()) Log.warn("Could not delete old backup " + zips[i]);
        }
    }
}
