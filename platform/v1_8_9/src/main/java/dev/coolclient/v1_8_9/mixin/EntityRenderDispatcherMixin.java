package dev.coolclient.v1_8_9.mixin;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.modules.perf.PerformanceModules;
import dev.coolclient.core.modules.visual.VisualTweaks;
import dev.coolclient.v1_8_9.Hooks189;
import dev.coolclient.v1_8_9.World3D189;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.CameraView;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    @Inject(method = "renderHitbox", at = @At("HEAD"), cancellable = true)
    private void cool$hitbox(Entity entity, double x, double y, double z, float yaw, float tickDelta, CallbackInfo ci) {
        if (!Hooks189.ready()) return;
        VisualTweaks.Hitboxes hb = CoolClient.get().modules().get(VisualTweaks.Hitboxes.class);
        if (hb == null || !hb.isEnabled()) return;
        World3D189.hitbox(entity, x, y, z, tickDelta, hb);
        ci.cancel();
    }

    @Inject(method = "shouldRender", at = @At("RETURN"), cancellable = true)
    private void cool$cull(Entity entity, CameraView view, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() || !Hooks189.ready()) return;
        PerformanceModules.EntityCulling c = CoolClient.get().modules().get(PerformanceModules.EntityCulling.class);
        MinecraftClient mc = MinecraftClient.getInstance();
        if (c == null || mc.player == null || entity == mc.player) return;
        if (c.shouldSkip(entity.squaredDistanceTo(mc.player), entity instanceof PlayerEntity)) cir.setReturnValue(false);
    }
}
