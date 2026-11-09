package dev.coolclient.v1_21_4.mixin;

import dev.coolclient.core.modules.utility.UtilityTweaks;
import dev.coolclient.v1_21_4.Hooks21;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Sounds: per-sound volume on top of the vanilla category sliders. */
@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {
    @Inject(method = "calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F", at = @At("RETURN"), cancellable = true)
    private void cool$volume(SoundInstance sound, CallbackInfoReturnable<Float> cir) {
        if (!Hooks21.ready()) return;
        float f = UtilityTweaks.Sounds.volume(sound.getLocation().toString());
        if (f != 1f) cir.setReturnValue(Math.max(0f, Math.min(1f, cir.getReturnValueF() * f)));
    }
}
