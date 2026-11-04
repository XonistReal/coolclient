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

    @Unique
    private boolean cool$clear() {
        return isClientSide && Hooks21.ready() && CoolClient.get().modules().active(VisualTweaks.WeatherChanger.class);
    }

    @Inject(method = "getRainLevel", at = @At("HEAD"), cancellable = true)
    private void cool$rain(float delta, CallbackInfoReturnable<Float> cir) {
        if (cool$clear()) cir.setReturnValue(0f);
    }

    @Inject(method = "getThunderLevel", at = @At("HEAD"), cancellable = true)
    private void cool$thunder(float delta, CallbackInfoReturnable<Float> cir) {
        if (cool$clear()) cir.setReturnValue(0f);
    }
}
