package dev.coolclient.v1_21_4.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.coolclient.core.CoolClient;
import dev.coolclient.core.modules.perf.PerformanceModules;
import dev.coolclient.v1_21_4.Hooks21;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void cool$cull(BlockEntity be, float tickDelta, PoseStack pose, MultiBufferSource buffers, CallbackInfo ci) {
        if (!Hooks21.ready()) return;
        PerformanceModules.EntityCulling c = CoolClient.get().modules().get(PerformanceModules.EntityCulling.class);
        Minecraft mc = Minecraft.getInstance();
        if (c == null || mc.player == null) return;
        if (c.shouldSkipTile(mc.player.distanceToSqr(be.getBlockPos().getCenter()))) ci.cancel();
    }
}
