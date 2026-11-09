package dev.coolclient.core.modules.utility;

import dev.coolclient.core.module.Category;
import dev.coolclient.core.module.Module;
import dev.coolclient.core.setting.BoolSetting;
import dev.coolclient.core.setting.NumberSetting;
import dev.coolclient.core.util.Log;
import dev.coolclient.core.util.Scheduler;

import java.io.File;

/** Deletes old game logs (and optionally crash reports) when the game starts. latest.log is never touched. */
public final class LogCleaner extends Module {
    private final NumberSetting days = add(new NumberSetting("Delete Older Than", 7, 1, 90, 1).suffix(" days"));
    private final BoolSetting crashReports = add(new BoolSetting("Crash Reports Too", false));

    public LogCleaner() {
        super("Log Cleaner", "Clears out old log files each time the game starts.", Category.UTILITY);
    }

    @Override
    protected void onEnable() {
        final File game = platform().gameDir();
        final long cutoff = System.currentTimeMillis() - (long) (days.d() * 86_400_000L);
        final boolean crashes = crashReports.on();
        Scheduler.async(() -> {
            int n = clean(new File(game, "logs"), cutoff, true);
            if (crashes) n += clean(new File(game, "crash-reports"), cutoff, false);
            if (n > 0) Log.info("Log Cleaner removed " + n + " old file(s)");
        });
    }

    private static int clean(File dir, long cutoff, boolean logs) {
        File[] files = dir.listFiles();
        if (files == null) return 0;
        int n = 0;
        for (File f : files) {
            String name = f.getName();
            boolean match = logs ? name.endsWith(".log.gz") : name.endsWith(".txt");
            if (!f.isFile() || !match || f.lastModified() > cutoff) continue;
            if (f.delete()) n++;
        }
        return n;
    }
}
