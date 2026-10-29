package dev.coolclient.launcher.core;

import java.io.File;

/** Where the launcher keeps everything. */
public final class LauncherPaths {
    public static final File BASE = baseDir();
    public static final File VERSIONS = new File(BASE, "versions");
    public static final File LIBRARIES = new File(BASE, "libraries");
    public static final File ASSETS = new File(BASE, "assets");
    public static final File RUNTIMES = new File(BASE, "runtimes");
    public static final File MODS = new File(BASE, "mods");
    public static final File LOGS = new File(BASE, "logs");
    /** Cool Client settings and cosmetics, shared by the launcher and every game version. */
    public static final File CLIENT_DATA = new File(BASE, "coolclient");
    public static final File SETTINGS = new File(BASE, "launcher_settings.json");
    public static final File ACCOUNTS = new File(BASE, "launcher_accounts.json");

    private LauncherPaths() {}

    public static File instance(String version) { return new File(new File(BASE, "instances"), version); }

    public static File natives(String version) { return new File(new File(BASE, "natives"), version); }

    private static File baseDir() {
        String override = System.getProperty("coolclient.launcher.home");
        if (override != null && !override.isEmpty()) return new File(override);
        String os = System.getProperty("os.name").toLowerCase();
        String home = System.getProperty("user.home");
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            return new File(appData != null ? appData : home + "\\AppData\\Roaming", ".coolclient");
        }
        if (os.contains("mac")) return new File(home, "Library/Application Support/coolclient");
        return new File(home, ".coolclient");
    }
}
