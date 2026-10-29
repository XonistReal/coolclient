package dev.coolclient.v1_21_4.mixin;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.modules.utility.UtilityModules;
import dev.coolclient.v1_21_4.Hooks21;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabOverlayMixin {
    @Inject(method = "renderPingIcon", at = @At("HEAD"), cancellable = true)
    private void cool$numericPing(GuiGraphics g, int width, int x, int y, PlayerInfo info, CallbackInfo ci) {
        if (!Hooks21.ready()) return;
        UtilityModules.TabListTweaks t = CoolClient.get().modules().get(UtilityModules.TabListTweaks.class);
        if (t == null || !t.isEnabled() || !t.numericPing.on()) return;
        int ping = info.getLatency();
        String s = String.valueOf(ping);
        Minecraft mc = Minecraft.getInstance();
        int color = t.colorPing.on() ? UtilityModules.TabListTweaks.pingColor(ping) : 0xFFFFFFFF;
        g.pose().pushPose();
        g.pose().translate(x + width - mc.font.width(s) * 0.75f - 1, y + 1.5f, 0f);
        g.pose().scale(0.75f, 0.75f, 1f);
        g.drawString(mc.font, s, 0, 0, color, true);
        g.pose().popPose();
        ci.cancel();
    }
}
