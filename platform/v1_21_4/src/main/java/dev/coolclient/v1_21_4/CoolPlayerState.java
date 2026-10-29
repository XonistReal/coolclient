package dev.coolclient.v1_21_4;

import java.util.UUID;

/** Extra data stored on PlayerRenderState by a mixin. */
public interface CoolPlayerState {
    UUID cool$uuid();

    void cool$setUuid(UUID uuid);

    boolean cool$flying();

    void cool$setFlying(boolean flying);
}
