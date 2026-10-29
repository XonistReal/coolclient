package dev.coolclient.v1_8_9.mixin;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.modules.visual.VisualTweaks;
import dev.coolclient.v1_8_9.Hooks189;
import dev.coolclient.v1_8_9.Platform189;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin {
    // Legacy Yarn swaps the names: getSkinId() returns the cape and getCapeId() the skin.
    @Inject(method = "getSkinId()Lnet/minecraft/util/Identifier;", at = @At("RETURN"), cancellable = true)
    private void cool$cape(CallbackInfoReturnable<Identifier> cir) {
        if (!Hooks189.ready()) return;
        int tex = CoolClient.get().cosmetics().capeTexture(((PlayerEntity) (Object) this).getUuid());
        if (tex > 0) {
            Identifier id = Platform189.TEXTURES.identifier(tex);
            if (id != null) cir.setReturnValue(id);
        }
    }

    @Inject(method = "getSpeed", at = @At("RETURN"), cancellable = true)
    private void cool$fov(CallbackInfoReturnable<Float> cir) {
        if (!Hooks189.ready()) return;
        VisualTweaks.FovChanger f = CoolClient.get().modules().get(VisualTweaks.FovChanger.class);
        if (f == null || !f.isEnabled()) return;
        PlayerEntity self = (PlayerEntity) (Object) this;
        float value = cir.getReturnValueF();
        float bow = 1f;
        if (self.isUsingItem() && self.getUsedItem() != null && self.getUsedItem().getItem() == Items.BOW) {
            float pull = self.getItemUseTicks() / 20f;
            pull = pull > 1f ? 1f : pull * pull;
            bow = 1f - pull * 0.15f;
        }
        float speedPart = bow == 0f ? value : value / bow;
        if (f.noSpeedFov.on()) speedPart = 1f;
        if (f.noBowFov.on()) bow = 1f;
        cir.setReturnValue(speedPart * bow);
    }
}
