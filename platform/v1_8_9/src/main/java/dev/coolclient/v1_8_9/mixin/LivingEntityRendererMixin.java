package dev.coolclient.v1_8_9.mixin;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.modules.visual.VisualTweaks;
import dev.coolclient.v1_8_9.Hooks189;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Unique
    private static int cool$hitColor() {
        if (!Hooks189.ready()) return 0;
        VisualTweaks.HitColor h = CoolClient.get().modules().get(VisualTweaks.HitColor.class);
        return h != null && h.isEnabled() ? h.color.color() : 0;
    }

    // The hurt overlay is four FloatBuffer.put calls: r, g, b, a (1, 0, 0, 0.3 in vanilla).
    @ModifyArg(method = "method_10252", at = @At(value = "INVOKE", target = "Ljava/nio/FloatBuffer;put(F)Ljava/nio/FloatBuffer;", ordinal = 0))
    private float cool$r(float v) { int c = cool$hitColor(); return c == 0 ? v : ((c >> 16) & 0xFF) / 255f; }

    @ModifyArg(method = "method_10252", at = @At(value = "INVOKE", target = "Ljava/nio/FloatBuffer;put(F)Ljava/nio/FloatBuffer;", ordinal = 1))
    private float cool$g(float v) { int c = cool$hitColor(); return c == 0 ? v : ((c >> 8) & 0xFF) / 255f; }

    @ModifyArg(method = "method_10252", at = @At(value = "INVOKE", target = "Ljava/nio/FloatBuffer;put(F)Ljava/nio/FloatBuffer;", ordinal = 2))
    private float cool$b(float v) { int c = cool$hitColor(); return c == 0 ? v : (c & 0xFF) / 255f; }

    @ModifyArg(method = "method_10252", at = @At(value = "INVOKE", target = "Ljava/nio/FloatBuffer;put(F)Ljava/nio/FloatBuffer;", ordinal = 3))
    private float cool$a(float v) { int c = cool$hitColor(); return c == 0 ? v : ((c >>> 24) & 0xFF) / 255f; }

    /** Own nametag in third person. */
    @Inject(method = "hasLabel(Lnet/minecraft/entity/LivingEntity;)Z", at = @At("RETURN"), cancellable = true)
    private void cool$ownLabel(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() || !Hooks189.ready()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (entity != mc.player || mc.options.perspective == 0 || mc.options.hudHidden) return;
        VisualTweaks.Nametags n = CoolClient.get().modules().get(VisualTweaks.Nametags.class);
        if (n != null && n.isEnabled() && n.showOwn.on() && !entity.isInvisible()) cir.setReturnValue(true);
    }
}
