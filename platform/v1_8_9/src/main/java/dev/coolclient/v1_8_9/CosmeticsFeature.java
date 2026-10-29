package dev.coolclient.v1_8_9;

import com.mojang.blaze3d.platform.GlStateManager;
import dev.coolclient.core.CoolClient;
import dev.coolclient.core.cosmetic.CosmeticContext;
import dev.coolclient.core.cosmetic.Loadout;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.model.BiPedModel;

/** Renders mesh cosmetics (wings, hats, halos, auras) on players. */
public final class CosmeticsFeature implements FeatureRenderer<AbstractClientPlayerEntity> {
    private final PlayerEntityRenderer renderer;
    private final ModelRenderer189 mr = new ModelRenderer189();
    private final CosmeticContext ctx = new CosmeticContext();

    public CosmeticsFeature(PlayerEntityRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public void render(AbstractClientPlayerEntity player, float limbAngle, float limbDistance, float tickDelta,
                       float age, float headYaw, float headPitch, float scale) {
        if (player.isInvisible()) return;
        Loadout l = CoolClient.get().cosmetics().loadoutOf(player.getUuid());
        if (l == null || l.isEmpty()) return;
        BiPedModel model = (BiPedModel) renderer.getModel();
        ctx.uuid = player.getUuid();
        ctx.self = player == net.minecraft.client.MinecraftClient.getInstance().player;
        ctx.age = age;
        ctx.limbSwing = limbAngle;
        ctx.limbSwingAmount = limbDistance;
        ctx.sneaking = player.isSneaking();
        ctx.flying = player.abilities.flying;
        ctx.wearingHelmet = player.inventory.armor[3] != null;
        ctx.wearingChestplate = player.inventory.armor[2] != null;
        ctx.white = Platform189.TEXTURES.white();

        GlStateManager.pushMatrix();
        if (ctx.sneaking) GlStateManager.translate(0f, 0.2f, 0f);
        model.body.preRender(0.0625f);
        CoolClient.get().cosmetics().renderBody(ctx.uuid, mr, ctx);
        GlStateManager.popMatrix();

        GlStateManager.pushMatrix();
        if (ctx.sneaking) GlStateManager.translate(0f, 0.2f, 0f);
        model.head.preRender(0.0625f);
        CoolClient.get().cosmetics().renderHead(ctx.uuid, mr, ctx);
        GlStateManager.popMatrix();
        GlStateManager.color(1f, 1f, 1f, 1f);
        // Emissive meshes force full-bright; restore this entity's light for later layers.
        int light = player.getLightmapCoordinates(tickDelta);
        com.mojang.blaze3d.platform.GLX.gl13MultiTexCoord2f(com.mojang.blaze3d.platform.GLX.lightmapTextureUnit,
                light % 65536, (float) (light / 65536));
    }

    @Override
    public boolean combineTextures() { return false; }
}
