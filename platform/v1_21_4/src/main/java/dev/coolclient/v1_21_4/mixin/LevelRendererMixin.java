package dev.coolclient.v1_21_4.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.coolclient.core.CoolClient;
import dev.coolclient.core.modules.visual.BlockOutline;
import dev.coolclient.v1_21_4.Hooks21;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Unique
    private static BlockOutline cool$outline() {
        if (!Hooks21.ready()) return null;
        BlockOutline b = CoolClient.get().modules().get(BlockOutline.class);
        return b != null && b.isEnabled() ? b : null;
    }

    /** Ordinal 1 is the normal outline (ordinal 0 is the high-contrast backing line). */
    @ModifyArg(method = "renderBlockOutline", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;renderHitOutline(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)V",
            ordinal = 1), index = 8)
    private int cool$outlineColor(int color) {
        BlockOutline o = cool$outline();
        return o == null ? color : o.color.color();
    }

    @Inject(method = "renderHitOutline", at = @At("HEAD"))
    private void cool$fill(PoseStack pose, VertexConsumer consumer, Entity entity, double camX, double camY, double camZ,
                             BlockPos pos, BlockState state, int color, CallbackInfo ci) {
        BlockOutline o = cool$outline();
        if (o == null || !o.fill.on() || Minecraft.getInstance().level == null) return;
        VoxelShape shape = state.getShape(Minecraft.getInstance().level, pos, CollisionContext.of(entity));
        if (shape.isEmpty()) return;
        AABB b = shape.bounds().move(pos.getX() - camX, pos.getY() - camY, pos.getZ() - camZ).inflate(0.002);
        int c = o.fillColor.color();
        Matrix4f m = pose.last().pose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(CoreShaders.POSITION_COLOR);
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float x1 = (float) b.minX, y1 = (float) b.minY, z1 = (float) b.minZ, x2 = (float) b.maxX, y2 = (float) b.maxY, z2 = (float) b.maxZ;
        float[][] faces = {
                {x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2}, {x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1},
                {x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1}, {x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2},
                {x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1}, {x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2}};
        for (float[] f : faces)
            for (int i = 0; i < 4; i++) bb.addVertex(m, f[i * 3], f[i * 3 + 1], f[i * 3 + 2]).setColor(c);
        BufferUploader.drawWithShader(bb.buildOrThrow());
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}
