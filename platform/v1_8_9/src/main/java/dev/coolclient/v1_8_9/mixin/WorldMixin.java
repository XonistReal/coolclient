package dev.coolclient.v1_8_9.mixin;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.modules.visual.VisualTweaks;
import dev.coolclient.v1_8_9.Hooks189;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public abstract class WorldMixin {
    @Shadow @Final public boolean isClient;

    @org.spongepowered.asm.mixin.Unique
    private float[] cool$weather() {
        if (!isClient || !Hooks189.ready()) return null;
        VisualTweaks.WeatherChanger w = CoolClient.get().modules().get(VisualTweaks.WeatherChanger.class);
        return w == null ? null : w.levels();
    }

    @Inject(method = "getRainGradient", at = @At("HEAD"), cancellable = true)
    private void cool$rain(float delta, CallbackInfoReturnable<Float> cir) {
        float[] w = cool$weather();
        if (w != null) cir.setReturnValue(w[0]);
    }

    @Inject(method = "getThunderGradient", at = @At("HEAD"), cancellable = true)
    private void cool$thunder(float delta, CallbackInfoReturnable<Float> cir) {
        float[] w = cool$weather();
        if (w != null) cir.setReturnValue(w[1]);
    }
}
