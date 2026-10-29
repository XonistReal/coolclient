package dev.coolclient.v1_8_9.mixin;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.item.HeldItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(HeldItemRenderer.class)
public interface HeldItemRendererInvoker {
    @Invoker("renderArm")
    void cool$renderArm(AbstractClientPlayerEntity player, float equipProgress, float swingProgress);

    @Invoker("applyPlayerLighting")
    void cool$applyLighting(AbstractClientPlayerEntity player);
}
