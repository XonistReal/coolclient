package dev.coolclient.v1_21_4.mixin;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.modules.cpvp.TotemPopCounter;
import dev.coolclient.core.modules.utility.UtilityModules;
import dev.coolclient.v1_21_4.CoolPlayerState;
import dev.coolclient.v1_21_4.CosmeticsLayer;
import dev.coolclient.v1_21_4.Hooks21;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin extends LivingEntityRenderer<AbstractClientPlayer, PlayerRenderState, PlayerModel> {
    private PlayerRendererMixin(EntityRendererProvider.Context ctx, PlayerModel model, float shadow) {
        super(ctx, model, shadow);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void cool$addCosmetics(EntityRendererProvider.Context ctx, boolean slim, CallbackInfo ci) {
        this.addLayer(new CosmeticsLayer((PlayerRenderer) (Object) this));
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;F)V",
            at = @At("TAIL"))
    private void cool$extract(AbstractClientPlayer player, PlayerRenderState state, float tickDelta, CallbackInfo ci) {
        CoolPlayerState duck = (CoolPlayerState) state;
        duck.cool$setUuid(player.getUUID());
        duck.cool$setFlying(player.getAbilities().flying);
        if (state.nameTag == null || !Hooks21.ready()) return;
        float lift = CoolClient.get().cosmetics().nametagLift(player.getUUID());
        if (lift > 0 && state.nameTagAttachment != null) state.nameTagAttachment = state.nameTagAttachment.add(0, lift, 0);
        Component tag = state.nameTag;
        // Nick Hider: name tags are components, which the plain-string hook in FontMixin never sees.
        UtilityModules.NickHider nick = CoolClient.get().modules().get(UtilityModules.NickHider.class);
        if (nick != null && nick.isEnabled()) {
            String own = Minecraft.getInstance().getUser().getName();
            String plain = tag.getString();
            if (plain.contains(own)) tag = Component.literal(nick.apply(plain, own)).withStyle(tag.getStyle());
        }
        if (CoolClient.get().client().showBadges.on() && CoolClient.get().cosmetics().isClientUser(player.getUUID())) {
            // The owner gets a purple crown, Media (creators) a red play badge, supporters a gold star,
            // everyone else the aqua one.
            if (CoolClient.get().cosmetics().isOwner(player.getUUID())) {
                tag = Component.literal("♚ ").withStyle(ChatFormatting.LIGHT_PURPLE).append(tag);
            } else if (CoolClient.get().cosmetics().isMedia(player.getUUID())) {
                tag = Component.literal("▶ ").withStyle(ChatFormatting.RED).append(tag);
            } else {
                boolean supporter = CoolClient.get().cosmetics().isSupporter(player.getUUID());
                tag = Component.literal("✦ ").withStyle(supporter ? ChatFormatting.GOLD : ChatFormatting.AQUA).append(tag);
            }
        }
        TotemPopCounter pops = CoolClient.get().modules().get(TotemPopCounter.class);
        if (pops != null && pops.showOnNametags()) {
            int n = pops.pops(player.getGameProfile().getName());
            if (n > 0) tag = tag.copy().append(Component.literal(" -" + n).withStyle(ChatFormatting.RED));
        }
        state.nameTag = tag;
    }
}
