package dev.coolclient.launcher;

import dev.coolclient.launcher.core.Accounts;
import dev.coolclient.launcher.core.GameLauncher;
import dev.coolclient.launcher.core.LauncherSettings;

public final class Main {
    private Main() {}

    public static void main(String[] args) throws Exception {
        // AWT is only used off-screen (font and icon rasterising); never let it open a display.
        System.setProperty("java.awt.headless", "true");
        if (args.length >= 2 && args[0].equals("--headless-launch")) {
            System.exit(headlessLaunch(args[1]));
        }
        // Development flags, also accepted by the packaged exe.
        for (String a : args) {
            if (a.startsWith("--selftest=")) System.setProperty("coolclient.launcher.selftest", a.substring(11));
            else if (a.startsWith("--selftest-play=")) System.setProperty("coolclient.launcher.selftest.play", a.substring(16));
        }
        LauncherApp.run();
    }

    /** Installs and launches a version with an offline account and the client self-test. */
    private static int headlessLaunch(String version) throws Exception {
        GameLauncher.Options o = new GameLauncher.Options();
        o.version = version;
        o.account = Accounts.offline("CoolDev");
        o.settings = LauncherSettings.load();
        o.selfTest = true;
        long[] last = {0};
        Process p = GameLauncher.launch(o, (stage, f) -> {
            long now = System.currentTimeMillis();
            if (now - last[0] > 1500 || f >= 1f) {
                last[0] = now;
                System.out.printf("[progress] %s %d%%%n", stage, Math.round(f * 100));
            }
        }, System.out::println);
        int code = p.waitFor();
        System.out.println("[Launcher] Game exited with code " + code);
        return code;
    }
}
