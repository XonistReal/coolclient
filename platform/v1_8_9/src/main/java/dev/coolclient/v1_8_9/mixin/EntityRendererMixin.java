package dev.coolclient.v1_8_9.mixin;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.modules.visual.VisualTweaks;
import dev.coolclient.v1_8_9.Hooks189;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
    @Unique private static boolean cool$inBadge;

    @Shadow
    protected abstract void renderLabelIfPresent(Entity entity, String text, double x, double y, double z, int maxDistance);

    @ModifyConstant(method = "renderLabelIfPresent", constant = @Constant(floatValue = 0.25f))
    private float cool$labelBackground(float alpha) {
        if (!Hooks189.ready()) return alpha;
        VisualTweaks.Nametags n = CoolClient.get().modules().get(VisualTweaks.Nametags.class);
        return n != null && n.isEnabled() ? n.background.f() / 100f : alpha;
    }

    /** Prefix Cool Client users' nametags with the client badge and lift them clear of hats/halos. */
    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
    private void cool$badge(Entity entity, String text, double x, double y, double z, int maxDistance, CallbackInfo ci) {
        if (cool$inBadge || !Hooks189.ready() || !(entity instanceof PlayerEntity)) return;
        boolean badge = CoolClient.get().client().showBadges.on() && CoolClient.get().cosmetics().isClientUser(entity.getUuid());
        float lift = CoolClient.get().cosmetics().nametagLift(entity.getUuid());
        if (!badge && lift <= 0) return;
        cool$inBadge = true;
        try {
            // The owner gets a purple crown, Media (creators) a red play badge, supporters a gold star,
            // everyone else the aqua one.
            String star = CoolClient.get().cosmetics().isOwner(entity.getUuid()) ? "§d♚§r "
                    : CoolClient.get().cosmetics().isMedia(entity.getUuid()) ? "§c▶§r "
                    : CoolClient.get().cosmetics().isSupporter(entity.getUuid()) ? "§6✦§r " : "§b✦§r ";
            renderLabelIfPresent(entity, badge ? star + text : text, x, y + lift, z, maxDistance);
        } finally {
            cool$inBadge = false;
        }
        ci.cancel();
    }
}
