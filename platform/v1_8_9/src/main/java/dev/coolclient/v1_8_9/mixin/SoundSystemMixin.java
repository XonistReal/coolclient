package dev.coolclient.v1_8_9.mixin;

import dev.coolclient.core.modules.utility.UtilityTweaks;
import dev.coolclient.v1_8_9.Hooks189;
import net.minecraft.client.sound.Sound;
import net.minecraft.client.sound.SoundCategory;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Sounds: per-sound volume on top of the vanilla category sliders (method_7100 works out the volume). */
@Mixin(SoundSystem.class)
public abstract class SoundSystemMixin {
    @Inject(method = "method_7100", at = @At("RETURN"), cancellable = true)
    private void cool$volume(SoundInstance sound, Sound entry, SoundCategory category, CallbackInfoReturnable<Float> cir) {
        if (!Hooks189.ready()) return;
        float f = UtilityTweaks.Sounds.volume(String.valueOf(sound.getIdentifier()));
        if (f != 1f) cir.setReturnValue(Math.max(0f, Math.min(1f, cir.getReturnValue() * f)));
    }
}
