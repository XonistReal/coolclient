package dev.coolclient.v1_21_4.mixin;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.modules.visual.VisualTweaks;
import dev.coolclient.v1_21_4.Hooks21;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Client-side weather override (never touches the server world). Time is in {@link ClientLevelDataMixin}. */
@Mixin(Level.class)
public abstract class LevelMixin {
    @Shadow @Final public boolean isClientSide;

    @org.spongepowered.asm.mixin.Unique
    private float[] cool$weather() {
        if (!isClientSide || !Hooks21.ready()) return null;
        VisualTweaks.WeatherChanger w = CoolClient.get().modules().get(VisualTweaks.WeatherChanger.class);
        return w == null ? null : w.levels();
    }

    @Inject(method = "getRainLevel", at = @At("HEAD"), cancellable = true)
    private void cool$rain(float delta, CallbackInfoReturnable<Float> cir) {
        float[] w = cool$weather();
        if (w != null) cir.setReturnValue(w[0]);
    }

    @Inject(method = "getThunderLevel", at = @At("HEAD"), cancellable = true)
    private void cool$thunder(float delta, CallbackInfoReturnable<Float> cir) {
        float[] w = cool$weather();
        if (w != null) cir.setReturnValue(w[1]);
    }
}
