package dev.coolclient.core.module;

public enum Category {
    HUD("HUD", 0xFF2DE2C1),
    COMBAT("Combat", 0xFFFF5470),
    VISUAL("Visual", 0xFF8A5CFF),
    MECHANIC("Mechanic", 0xFFFFB547),
    UTILITY("Utility", 0xFF4DA3FF),
    PERFORMANCE("Performance", 0xFF5BE37A),
    CLIENT("Client", 0xFFB0B8D0);

    public final String displayName;
    public final int color;

    Category(String displayName, int color) {
        this.displayName = displayName;
        this.color = color;
    }
}
