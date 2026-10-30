package dev.coolclient.core.api;

import java.util.List;
import java.util.UUID;

/** Read-only (mostly) view of the running game, implemented per Minecraft version. */
public interface GameState {
    boolean inWorld();

    int fps();

    /** Own latency in ms, or -1 if unknown. */
    int ping();

    /** Address of the connected server, or null in singleplayer / menus. */
    String serverAddress();

    String selfName();

    UUID selfUuid();

    double x();

    double y();

    double z();

    float yaw();

    float pitch();

    String biome();

    /** "overworld", "the_nether", "the_end" or a modded id. */
    String dimension();

    /** World time of day in ticks (not wrapped). */
    long worldTime();

    float health();

    float maxHealth();

    int food();

    float saturation();

    /** Armor piece by slot: 0 = boots, 1 = leggings, 2 = chestplate, 3 = helmet. */
    ItemRef armor(int slot);

    ItemRef mainHand();

    /** Offhand item, or {@link ItemRef#EMPTY} on versions without an offhand. */
    ItemRef offHand();

    /** Total count of items with any of the given ids in the inventory (incl. offhand). */
    int countItems(String... ids);

    /** A single item of the given id (for drawing icons), or EMPTY if unknown on this version. */
    ItemRef itemOf(String id);

    List<EffectInfo> effects();

    /** Name of the highest-priority enabled resource pack (excluding the default one). */
    String resourcePack();

    /** Remaining cooldown fraction (0..1) for an item id; 0 on versions without cooldowns. */
    float cooldown(String itemId);

    boolean sprinting();

    boolean sneaking();

    boolean thirdPerson();

    /** True when the F1 "hide HUD" toggle is active. */
    boolean hudHidden();

    /** Sets the F1 "hide HUD" toggle (used for clean screenshots). */
    default void setHudHidden(boolean hidden) {}

    /** True when the F3 debug screen is visible. */
    boolean debugOpen();

    /**
     * Code bound to a vanilla key mapping, as a GLFW key or {@code Keys.mouse(button)}.
     * Names: forward, left, back, right, jump, sprint, sneak, attack, use.
     */
    int bindingKey(String name);

    /** 0 = first person, 1 = third person back, 2 = third person front. */
    int perspective();

    void setPerspective(int perspective);

    /** Force the vanilla sprint/sneak key binding held (for toggle sprint/sneak). */
    void setSprintHeld(boolean held);

    void setSneakHeld(boolean held);

    /** UUIDs of players currently loaded in the world (for cosmetics lookups). */
    List<UUID> loadedPlayers();
}
