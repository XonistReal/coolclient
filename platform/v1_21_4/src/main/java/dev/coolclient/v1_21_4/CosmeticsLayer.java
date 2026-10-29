package dev.coolclient.v1_21_4;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.coolclient.core.CoolClient;
import dev.coolclient.core.cosmetic.CosmeticContext;
import dev.coolclient.core.cosmetic.Loadout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;

import java.util.UUID;

/** Renders mesh cosmetics (wings, hats, halos, auras) on players. */
public final class CosmeticsLayer extends RenderLayer<PlayerRenderState, PlayerModel> {
    private final ModelRenderer21 mr = new ModelRenderer21();
    private final CosmeticContext ctx = new CosmeticContext();

    public CosmeticsLayer(RenderLayerParent<PlayerRenderState, PlayerModel> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, PlayerRenderState state, float yRot, float xRot) {
        if (state.isInvisible || CoolClient.get() == null) return;
        CoolPlayerState duck = (CoolPlayerState) state;
        UUID id = duck.cool$uuid();
        if (id == null) return;
        Loadout l = CoolClient.get().cosmetics().loadoutOf(id);
        if (l == null || l.isEmpty()) return;
        ctx.uuid = id;
        ctx.self = Minecraft.getInstance().player != null && id.equals(Minecraft.getInstance().player.getUUID());
        ctx.age = state.ageInTicks;
        ctx.limbSwing = state.walkAnimationPos;
        ctx.limbSwingAmount = state.walkAnimationSpeed;
        ctx.sneaking = state.isCrouching;
        ctx.flying = state.isFallFlying || duck.cool$flying();
        ctx.wearingHelmet = !state.headEquipment.isEmpty();
        ctx.wearingChestplate = !state.chestEquipment.isEmpty();
        ctx.white = Platform21.TEXTURES.white();

        PlayerModel model = getParentModel();
        mr.setup(pose, buffers, light);
        pose.pushPose();
        model.body.translateAndRotate(pose);
        CoolClient.get().cosmetics().renderBody(id, mr, ctx);
        pose.popPose();

        pose.pushPose();
        model.head.translateAndRotate(pose);
        CoolClient.get().cosmetics().renderHead(id, mr, ctx);
        pose.popPose();
    }
}
