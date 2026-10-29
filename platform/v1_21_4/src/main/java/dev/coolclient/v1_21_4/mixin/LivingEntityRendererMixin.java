package dev.coolclient.v1_21_4.mixin;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.modules.visual.VisualTweaks;
import dev.coolclient.v1_21_4.Hooks21;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    /** Show your own nametag in third person. */
    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z", at = @At("RETURN"), cancellable = true)
    private void cool$ownName(LivingEntity entity, double distSq, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() || !Hooks21.ready()) return;
        Minecraft mc = Minecraft.getInstance();
        if (entity != mc.player || mc.options.getCameraType().isFirstPerson() || mc.options.hideGui) return;
        VisualTweaks.Nametags n = CoolClient.get().modules().get(VisualTweaks.Nametags.class);
        if (n != null && n.isEnabled() && n.showOwn.on() && !entity.isInvisible()) cir.setReturnValue(true);
    }
}
