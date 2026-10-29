package dev.coolclient.v1_21_4.mixin;

import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import dev.coolclient.v1_21_4.ItemStateDuck;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemEntityRenderState.class)
public abstract class ItemEntityRenderStateMixin implements ItemStateDuck {
    @Unique private boolean cool$onGround;

    @Override public boolean cool$onGround() { return cool$onGround; }

    @Override public void cool$setOnGround(boolean onGround) { cool$onGround = onGround; }
}
