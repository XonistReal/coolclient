package dev.coolclient.v1_21_4.mixin;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.modules.visual.Freelook;
import dev.coolclient.v1_21_4.Hooks21;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Freelook: override the camera's primary rotation (setRotation ordinals 0 = minecart, 1 = normal).
 * Third-person front view derives from these values, so it follows automatically.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Unique
    private static Freelook cool$freelook() {
        if (!Hooks21.ready()) return null;
        Freelook f = CoolClient.get().modules().get(Freelook.class);
        return f != null && f.active() ? f : null;
    }

    @ModifyArg(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 0), index = 0)
    private float cool$cartYaw(float yaw) {
        Freelook f = cool$freelook();
        return f != null ? f.yaw() : yaw;
    }

    @ModifyArg(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 0), index = 1)
    private float cool$cartPitch(float pitch) {
        Freelook f = cool$freelook();
        return f != null ? f.pitch() : pitch;
    }

    @ModifyArg(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 1), index = 0)
    private float cool$yaw(float yaw) {
        Freelook f = cool$freelook();
        return f != null ? f.yaw() : yaw;
    }

    @ModifyArg(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 1), index = 1)
    private float cool$pitch(float pitch) {
        Freelook f = cool$freelook();
        return f != null ? f.pitch() : pitch;
    }
}
