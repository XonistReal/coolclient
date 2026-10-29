package dev.coolclient.launcher.core;

import java.io.InputStream;
import java.util.Properties;

public final class LauncherInfo {
    public static final String NAME = "Cool Client Launcher";
    public static final String VERSION = readVersion();

    private LauncherInfo() {}

    private static String readVersion() {
        try (InputStream in = LauncherInfo.class.getResourceAsStream("/launcher.properties")) {
            Properties p = new Properties();
            if (in != null) p.load(in);
            return p.getProperty("version", "dev");
        } catch (Exception e) {
            return "dev";
        }
    }
}
