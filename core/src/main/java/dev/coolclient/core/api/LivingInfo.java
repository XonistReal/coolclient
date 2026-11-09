package dev.coolclient.core.api;

/** Snapshot of a living entity, for HUDs that show what you're fighting. */
public final class LivingInfo {
    public final String name;
    public final float health, maxHealth, absorption;
    public final double distance;
    public final boolean player;
    public final int armor;

    public LivingInfo(String name, float health, float maxHealth, float absorption, double distance, boolean player, int armor) {
        this.name = name;
        this.health = health;
        this.maxHealth = maxHealth;
        this.absorption = absorption;
        this.distance = distance;
        this.player = player;
        this.armor = armor;
    }
}
