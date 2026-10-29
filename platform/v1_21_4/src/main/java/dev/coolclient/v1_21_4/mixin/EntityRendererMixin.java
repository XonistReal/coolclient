package dev.coolclient.v1_21_4.mixin;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.modules.visual.VisualTweaks;
import dev.coolclient.v1_21_4.Hooks21;
import net.minecraft.client.renderer.entity.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
    @ModifyArg(method = "renderNameTag", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;getBackgroundOpacity(F)F"))
    private float cool$labelBackground(float opacity) {
        if (!Hooks21.ready()) return opacity;
        VisualTweaks.Nametags n = CoolClient.get().modules().get(VisualTweaks.Nametags.class);
        return n != null && n.isEnabled() ? n.background.f() / 100f : opacity;
    }
}
