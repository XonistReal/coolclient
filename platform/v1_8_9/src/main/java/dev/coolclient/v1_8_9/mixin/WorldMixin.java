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
    private boolean cool$clear() {
        return isClient && Hooks189.ready() && CoolClient.get().modules().active(VisualTweaks.WeatherChanger.class);
    }

    @Inject(method = "getRainGradient", at = @At("HEAD"), cancellable = true)
    private void cool$rain(float delta, CallbackInfoReturnable<Float> cir) {
        if (cool$clear()) cir.setReturnValue(0f);
    }

    @Inject(method = "getThunderGradient", at = @At("HEAD"), cancellable = true)
    private void cool$thunder(float delta, CallbackInfoReturnable<Float> cir) {
        if (cool$clear()) cir.setReturnValue(0f);
    }
}
