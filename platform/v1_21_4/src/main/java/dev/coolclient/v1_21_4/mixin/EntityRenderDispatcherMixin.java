package dev.coolclient.v1_21_4.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.coolclient.core.CoolClient;
import dev.coolclient.core.modules.perf.PerformanceModules;
import dev.coolclient.core.modules.visual.VisualTweaks;
import dev.coolclient.v1_21_4.Hooks21;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    @Inject(method = "renderHitbox", at = @At("HEAD"), cancellable = true)
    private static void cool$hitbox(PoseStack pose, VertexConsumer lines, Entity entity, float tickDelta,
                                      float r, float g, float b, CallbackInfo ci) {
        if (!Hooks21.ready()) return;
        VisualTweaks.Hitboxes hb = CoolClient.get().modules().get(VisualTweaks.Hitboxes.class);
        if (hb == null || !hb.isEnabled()) return;
        ci.cancel();
        if (hb.playersOnly.on() && !(entity instanceof Player)) return;
        int c = hb.color.color();
        float cr = ((c >> 16) & 0xFF) / 255f, cg = ((c >> 8) & 0xFF) / 255f, cb = (c & 0xFF) / 255f, ca = ((c >>> 24) & 0xFF) / 255f;
        AABB box = entity.getBoundingBox().move(-entity.getX(), -entity.getY(), -entity.getZ());
        ShapeRenderer.renderLineBox(pose, lines, box, cr, cg, cb, ca);
        if (hb.showEyes.on() && entity instanceof LivingEntity) {
            double ey = entity.getEyeHeight();
            ShapeRenderer.renderLineBox(pose, lines, box.minX, ey - 0.01, box.minZ, box.maxX, ey + 0.01, box.maxZ, 1f, 0.2f, 0.2f, 1f);
        }
        if (hb.showLook.on()) {
            Vec3 look = entity.getViewVector(tickDelta);
            PoseStack.Pose p = pose.last();
            float ey = entity.getEyeHeight();
            lines.addVertex(p, 0f, ey, 0f).setColor(0.2f, 0.4f, 1f, 1f).setNormal(p, (float) look.x, (float) look.y, (float) look.z);
            lines.addVertex(p, (float) (look.x * 2), (float) (ey + look.y * 2), (float) (look.z * 2)).setColor(0.2f, 0.4f, 1f, 1f)
                    .setNormal(p, (float) look.x, (float) look.y, (float) look.z);
        }
    }

    @Inject(method = "shouldRender", at = @At("RETURN"), cancellable = true)
    private void cool$cull(Entity entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() || !Hooks21.ready()) return;
        PerformanceModules.EntityCulling c = CoolClient.get().modules().get(PerformanceModules.EntityCulling.class);
        Minecraft mc = Minecraft.getInstance();
        if (c == null || mc.player == null || entity == mc.player) return;
        if (c.shouldSkip(entity.distanceToSqr(mc.player), entity instanceof Player)) cir.setReturnValue(false);
    }
}
