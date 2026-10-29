package dev.coolclient.v1_21_4.mixin;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.modules.visual.Freelook;
import dev.coolclient.v1_21_4.Hooks21;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
    /** Freelook: route mouse movement to the camera instead of the player while active. */
    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void cool$freelook(double yaw, double pitch, CallbackInfo ci) {
        if (!Hooks21.ready() || (Object) this != Minecraft.getInstance().player) return;
        Freelook f = CoolClient.get().modules().get(Freelook.class);
        if (f != null && f.active()) {
            f.turn(yaw, pitch);
            ci.cancel();
        }
    }
}
