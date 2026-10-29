package dev.coolclient.v1_8_9.mixin;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.modules.perf.PerformanceModules;
import dev.coolclient.v1_8_9.Hooks189;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {
    @Inject(method = "renderEntity(Lnet/minecraft/block/entity/BlockEntity;FI)V", at = @At("HEAD"), cancellable = true)
    private void cool$cull(BlockEntity be, float tickDelta, int destroyStage, CallbackInfo ci) {
        if (!Hooks189.ready()) return;
        PerformanceModules.EntityCulling c = CoolClient.get().modules().get(PerformanceModules.EntityCulling.class);
        MinecraftClient mc = MinecraftClient.getInstance();
        if (c == null || mc.player == null) return;
        BlockPos p = be.getPos();
        if (c.shouldSkipTile(mc.player.squaredDistanceTo(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5))) ci.cancel();
    }
}
