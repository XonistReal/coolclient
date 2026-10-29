package dev.coolclient.v1_8_9.mixin;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.modules.visual.Freelook;
import dev.coolclient.v1_8_9.Hooks189;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
    /** Freelook: route mouse movement to the camera instead of the player while active. */
    @Inject(method = "increaseTransforms", at = @At("HEAD"), cancellable = true)
    private void cool$freelook(float yaw, float pitch, CallbackInfo ci) {
        if (!Hooks189.ready() || (Object) this != MinecraftClient.getInstance().player) return;
        Freelook f = CoolClient.get().modules().get(Freelook.class);
        if (f != null && f.active()) {
            // 1.8 subtracts pitch, so negate to match the core's convention.
            f.turn(yaw, -pitch);
            ci.cancel();
        }
    }
}
