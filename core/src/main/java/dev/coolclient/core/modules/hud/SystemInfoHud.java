package dev.coolclient.core.modules.hud;

import dev.coolclient.core.hud.LinesElement;
import dev.coolclient.core.setting.BoolSetting;
import dev.coolclient.core.util.Time;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

/** CPU load of the game, RAM, graphics card and Java version. */
public final class SystemInfoHud extends LinesElement {
    private final BoolSetting cpu = add(new BoolSetting("CPU", true));
    private final BoolSetting ram = add(new BoolSetting("RAM", true));
    private final BoolSetting gpu = add(new BoolSetting("GPU", true));
    private final BoolSetting java = add(new BoolSetting("Java", false));
    private long sampledAt;
    private String cpuText = "...";
    private String gpuName;

    public SystemInfoHud() {
        super("System Info", "CPU, RAM and graphics card at a glance.");
        defaultPosition(Anchor.TOP_RIGHT, -4, 60);
    }

    @Override
    protected List<String> lines(boolean editing) {
        long now = Time.millis();
        if (now - sampledAt > 1000) {
            sampledAt = now;
            cpuText = cpuLoad();
        }
        if (gpuName == null) {
            String n = platform().gpuName();
            gpuName = n == null ? "Unknown" : shorten(n);
        }
        List<String> out = new ArrayList<String>();
        if (cpu.on()) out.add("CPU: " + cpuText);
        if (ram.on()) {
            Runtime rt = Runtime.getRuntime();
            long used = (rt.totalMemory() - rt.freeMemory()) >> 20, max = rt.maxMemory() >> 20;
            out.add("RAM: " + used + " / " + max + " MB");
        }
        if (gpu.on()) out.add("GPU: " + gpuName);
        if (java.on()) out.add("Java: " + System.getProperty("java.version"));
        return out;
    }

    private static String cpuLoad() {
        try {
            java.lang.management.OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
            if (os instanceof com.sun.management.OperatingSystemMXBean) {
                double load = ((com.sun.management.OperatingSystemMXBean) os).getProcessCpuLoad();
                if (load >= 0) return Math.round(load * 100) + "%";
            }
        } catch (Throwable ignored) {
            // Not every JVM has the com.sun extension.
        }
        return "n/a";
    }

    /** "NVIDIA GeForce RTX 3060/PCIe/SSE2" becomes "NVIDIA GeForce RTX 3060". */
    private static String shorten(String renderer) {
        int slash = renderer.indexOf('/');
        String s = slash > 0 ? renderer.substring(0, slash) : renderer;
        return s.length() > 34 ? s.substring(0, 33) + "..." : s;
    }
}
