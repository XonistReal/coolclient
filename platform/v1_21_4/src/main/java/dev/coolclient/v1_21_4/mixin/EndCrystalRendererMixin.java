package dev.coolclient.v1_21_4.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.coolclient.core.CoolClient;
import dev.coolclient.core.modules.cpvp.CrystalPvpTweaks;
import dev.coolclient.v1_21_4.Hooks21;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EndCrystalRenderer;
import net.minecraft.client.renderer.entity.state.EndCrystalRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/** Crystal Tweaks: purely visual scale, spin and bounce of end crystals. */
@Mixin(EndCrystalRenderer.class)
public abstract class EndCrystalRendererMixin {
    @Unique
    private static CrystalPvpTweaks.CrystalTweaks cool$tweaks() {
        if (!Hooks21.ready()) return null;
        CrystalPvpTweaks.CrystalTweaks t = CoolClient.get().modules().get(CrystalPvpTweaks.CrystalTweaks.class);
        return t != null && t.isEnabled() ? t : null;
    }

    @Inject(method = "render(Lnet/minecraft/client/renderer/entity/state/EndCrystalRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"))
    private void cool$spin(EndCrystalRenderState state, PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo ci) {
        CrystalPvpTweaks.CrystalTweaks t = cool$tweaks();
        if (t != null) state.ageInTicks *= t.spin.f();
    }

    @ModifyArgs(method = "render(Lnet/minecraft/client/renderer/entity/state/EndCrystalRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V", ordinal = 0))
    private void cool$scale(Args args) {
        CrystalPvpTweaks.CrystalTweaks t = cool$tweaks();
        if (t == null) return;
        float s = t.scale.f();
        args.set(0, (Float) args.get(0) * s);
        args.set(1, (Float) args.get(1) * s);
        args.set(2, (Float) args.get(2) * s);
    }

    @Inject(method = "getY", at = @At("RETURN"), cancellable = true)
    private static void cool$bounce(float age, CallbackInfoReturnable<Float> cir) {
        CrystalPvpTweaks.CrystalTweaks t = cool$tweaks();
        if (t != null) cir.setReturnValue((cir.getReturnValueF() + 1.4f) * t.bounce.f() - 1.4f);
    }
}
