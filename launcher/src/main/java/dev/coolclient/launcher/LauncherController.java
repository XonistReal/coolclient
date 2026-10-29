package dev.coolclient.launcher;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.gui.Notifications;
import dev.coolclient.core.util.Log;
import dev.coolclient.launcher.core.Accounts;
import dev.coolclient.launcher.core.GameLauncher;
import dev.coolclient.launcher.core.LauncherPaths;
import dev.coolclient.launcher.core.LauncherSettings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/** Launcher state shared by the UI thread and background work (installs, sign-in, the game). */
final class LauncherController {
    interface WindowActions {
        void minimize();

        void hide();

        void show();
    }

    final LauncherSettings settings = LauncherSettings.load();
    final Accounts accounts = Accounts.load();
    private final ConcurrentLinkedQueue<Runnable> mainTasks = new ConcurrentLinkedQueue<Runnable>();
    private WindowActions window;

    // Launch state (written by the worker, read by the UI).
    volatile boolean launching;
    volatile String stage = "";
    volatile float fraction;
    volatile Process game;
    volatile String runningVersion;
    /** Exit code of the last game session, or null if none finished yet. */
    volatile Integer lastExitCode;

    // Microsoft sign-in state.
    volatile Accounts.DeviceCode deviceCode;
    volatile boolean signingIn;
    private final AtomicBoolean cancelSignIn = new AtomicBoolean();

    private final ArrayDeque<String> log = new ArrayDeque<String>();
    private int logVersion;
    private PrintWriter logFile;

    void setWindow(WindowActions w) { this.window = w; }

    /** Runs queued work on the UI thread; called once per frame. */
    void drainMainTasks() {
        for (Runnable r; (r = mainTasks.poll()) != null; ) r.run();
    }

    private void onMain(Runnable r) { mainTasks.add(r); }

    private static void toast(String title, String message, Notifications.Level level) {
        CoolClient.get().notifications().post(title, message, level);
    }

    boolean running() { return game != null && game.isAlive(); }

    // ------------------------------------------------------------------ play

    void play(String server) {
        if (launching || running()) return;
        Accounts.Account account = accounts.selected();
        if (account == null) {
            toast("Add an account", "Sign in with Microsoft or add an offline name first.", Notifications.Level.WARNING);
            return;
        }
        final String version = settings.version;
        launching = true;
        stage = "Preparing";
        fraction = 0f;
        clearLog();
        Thread t = new Thread(() -> {
            try {
                GameLauncher.Options o = new GameLauncher.Options();
                o.version = version;
                o.account = accounts.fresh(account, settings.msClientId);
                o.settings = settings;
                o.server = server;
                o.selfTest = Boolean.getBoolean("coolclient.launcher.gameSelfTest");
                openLogFile(version);
                Process p = GameLauncher.launch(o, (s, f) -> {
                    stage = s;
                    fraction = f;
                }, this::appendLog);
                game = p;
                runningVersion = version;
                onMain(() -> {
                    toast("Starting Minecraft " + version, "Have fun! Logs are in the Console tab.", Notifications.Level.SUCCESS);
                    if (window != null) {
                        if (settings.afterLaunch.equals("minimize")) window.minimize();
                        else if (settings.afterLaunch.equals("close")) window.hide();
                    }
                });
                int code = p.waitFor();
                lastExitCode = code;
                appendLog("[Launcher] Minecraft exited with code " + code);
                onMain(() -> {
                    if (window != null) window.show();
                    if (code != 0) toast("Minecraft closed", "Exit code " + code + ". Check the Console tab.", Notifications.Level.ERROR);
                });
            } catch (Throwable e) {
                Log.error("Launch failed", e);
                appendLog("[Launcher] Launch failed: " + e);
                String msg = e.getMessage() == null ? e.toString() : e.getMessage();
                onMain(() -> toast("Launch failed", msg, Notifications.Level.ERROR));
            } finally {
                game = null;
                runningVersion = null;
                launching = false;
                closeLogFile();
            }
        }, "launch");
        t.setDaemon(true);
        t.start();
    }

    void kill() {
        Process p = game;
        if (p != null) p.destroyForcibly();
    }

    // ------------------------------------------------------------------ accounts

    boolean addOffline(String name) {
        try {
            accounts.put(Accounts.offline(name.trim()));
            toast("Account added", name.trim() + " (offline)", Notifications.Level.SUCCESS);
            return true;
        } catch (IllegalArgumentException e) {
            toast("Invalid name", e.getMessage(), Notifications.Level.ERROR);
            return false;
        }
    }

    void startMicrosoft() {
        if (signingIn) return;
        signingIn = true;
        cancelSignIn.set(false);
        deviceCode = null;
        final String clientId = settings.msClientId;
        Thread t = new Thread(() -> {
            try {
                Accounts.DeviceCode code = Accounts.startDeviceCode(clientId);
                deviceCode = code;
                onMain(() -> CoolClient.get().platform().setClipboard(code.userCode));
                Accounts.Account a = Accounts.finishDeviceCode(clientId, code, cancelSignIn::get);
                accounts.put(a);
                onMain(() -> toast("Signed in", "Welcome, " + a.name + "!", Notifications.Level.SUCCESS));
            } catch (Throwable e) {
                String msg = e.getMessage() == null ? e.toString() : e.getMessage();
                if (!cancelSignIn.get()) onMain(() -> toast("Microsoft sign-in", msg, Notifications.Level.ERROR));
            } finally {
                deviceCode = null;
                signingIn = false;
            }
        }, "microsoft-sign-in");
        t.setDaemon(true);
        t.start();
    }

    void cancelMicrosoft() {
        cancelSignIn.set(true);
    }

    // ------------------------------------------------------------------ log

    synchronized void appendLog(String line) {
        log.addLast(line);
        while (log.size() > 4000) log.removeFirst();
        logVersion++;
        if (logFile != null) logFile.println(line);
    }

    synchronized List<String> logLines() { return new ArrayList<String>(log); }

    synchronized int logVersion() { return logVersion; }

    synchronized void clearLog() {
        log.clear();
        logVersion++;
    }

    private synchronized void openLogFile(String version) {
        try {
            LauncherPaths.LOGS.mkdirs();
            logFile = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(LauncherPaths.LOGS, "game-" + version + ".log")), StandardCharsets.UTF_8), true);
        } catch (Exception e) {
            logFile = null;
        }
    }

    private synchronized void closeLogFile() {
        if (logFile != null) logFile.close();
        logFile = null;
    }
}
