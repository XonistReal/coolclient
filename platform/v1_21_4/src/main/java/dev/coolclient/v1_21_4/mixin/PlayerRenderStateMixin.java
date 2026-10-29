package dev.coolclient.v1_21_4.mixin;

import dev.coolclient.v1_21_4.CoolPlayerState;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.UUID;

@Mixin(PlayerRenderState.class)
public abstract class PlayerRenderStateMixin implements CoolPlayerState {
    @Unique private UUID cool$uuid;
    @Unique private boolean cool$flying;

    @Override public UUID cool$uuid() { return cool$uuid; }

    @Override public void cool$setUuid(UUID uuid) { cool$uuid = uuid; }

    @Override public boolean cool$flying() { return cool$flying; }

    @Override public void cool$setFlying(boolean flying) { cool$flying = flying; }
}
