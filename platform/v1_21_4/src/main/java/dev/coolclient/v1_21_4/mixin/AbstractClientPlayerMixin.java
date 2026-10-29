package dev.coolclient.v1_21_4.mixin;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.modules.visual.VisualTweaks;
import dev.coolclient.v1_21_4.Hooks21;
import dev.coolclient.v1_21_4.Platform21;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {
    /** CoolClient capes (and matching elytra) through the vanilla skin pipeline. */
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void cool$cape(CallbackInfoReturnable<PlayerSkin> cir) {
        if (!Hooks21.ready()) return;
        int tex = CoolClient.get().cosmetics().capeTexture(((Player) (Object) this).getUUID());
        if (tex <= 0) return;
        ResourceLocation rl = Platform21.TEXTURES.location(tex);
        if (rl == null) return;
        PlayerSkin s = cir.getReturnValue();
        cir.setReturnValue(new PlayerSkin(s.texture(), s.textureUrl(), rl, rl, s.model(), s.secure()));
    }

    @Inject(method = "getFieldOfViewModifier", at = @At("RETURN"), cancellable = true)
    private void cool$fov(boolean firstPerson, float effectScale, CallbackInfoReturnable<Float> cir) {
        if (!Hooks21.ready()) return;
        VisualTweaks.FovChanger f = CoolClient.get().modules().get(VisualTweaks.FovChanger.class);
        if (f == null || !f.isEnabled()) return;
        Player self = (Player) (Object) this;
        float bow = 1f;
        if (self.isUsingItem() && self.getUseItem().is(Items.BOW)) {
            float pull = Math.min(self.getTicksUsingItem() / 20f, 1f);
            bow = 1f - pull * pull * 0.15f;
        }
        // Undo vanilla's lerp(effectScale, 1, raw) to recover the raw multiplier.
        float v = cir.getReturnValueF();
        float raw = effectScale <= 0f ? 1f : (v - 1f + effectScale) / effectScale;
        float speedPart = bow == 0f ? raw : raw / bow;
        if (f.noSpeedFov.on()) speedPart = 1f;
        if (f.noBowFov.on()) bow = 1f;
        cir.setReturnValue(1f + (speedPart * bow - 1f) * effectScale);
    }
}
