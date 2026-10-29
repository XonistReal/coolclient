package dev.coolclient.launcher;

import dev.coolclient.core.api.EffectInfo;
import dev.coolclient.core.api.GameState;
import dev.coolclient.core.api.ItemRef;
import dev.coolclient.core.util.Keys;
import dev.coolclient.launcher.core.Accounts;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** "No world" game state; the player is the selected account, so the locker shows its cosmetics. */
final class LauncherGame implements GameState {
    private final Accounts accounts;
    int fps;

    LauncherGame(Accounts accounts) {
        this.accounts = accounts;
    }

    @Override public boolean inWorld() { return false; }

    @Override public int fps() { return fps; }

    @Override public int ping() { return 0; }

    @Override public String serverAddress() { return null; }

    @Override
    public String selfName() {
        Accounts.Account a = accounts.selected();
        return a == null ? "Player" : a.name;
    }

    /** Without an account the locker still works, on a fixed local id. */
    @Override
    public UUID selfUuid() {
        Accounts.Account a = accounts.selected();
        try {
            return a == null ? new UUID(0, 0) : UUID.fromString(a.uuid);
        } catch (IllegalArgumentException e) {
            return new UUID(0, 0);
        }
    }

    @Override public double x() { return 0; }

    @Override public double y() { return 0; }

    @Override public double z() { return 0; }

    @Override public float yaw() { return 0; }

    @Override public float pitch() { return 0; }

    @Override public String biome() { return ""; }

    @Override public String dimension() { return "minecraft:overworld"; }

    @Override public long worldTime() { return 0; }

    @Override public float health() { return 20; }

    @Override public float maxHealth() { return 20; }

    @Override public int food() { return 20; }

    @Override public float saturation() { return 5; }

    @Override public ItemRef armor(int slot) { return ItemRef.EMPTY; }

    @Override public ItemRef mainHand() { return ItemRef.EMPTY; }

    @Override public ItemRef offHand() { return ItemRef.EMPTY; }

    @Override public int countItems(String... ids) { return 0; }

    @Override public ItemRef itemOf(String id) { return ItemRef.EMPTY; }

    @Override public List<EffectInfo> effects() { return Collections.emptyList(); }

    @Override public String resourcePack() { return ""; }

    @Override public float cooldown(String itemId) { return 0; }

    @Override public boolean sprinting() { return false; }

    @Override public boolean sneaking() { return false; }

    @Override public boolean thirdPerson() { return false; }

    @Override public boolean hudHidden() { return false; }

    @Override public boolean debugOpen() { return false; }

    @Override public int bindingKey(String name) { return Keys.NONE; }

    @Override public int perspective() { return 0; }

    @Override public void setPerspective(int perspective) {}

    @Override public void setSprintHeld(boolean held) {}

    @Override public void setSneakHeld(boolean held) {}

    @Override public List<UUID> loadedPlayers() { return Collections.emptyList(); }
}
