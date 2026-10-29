package dev.coolclient.v1_8_9.mixin;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.modules.utility.UtilityModules;
import dev.coolclient.v1_8_9.Hooks189;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {
    @Inject(method = "renderLatencyIcon", at = @At("HEAD"), cancellable = true)
    private void cool$numericPing(int width, int x, int y, PlayerListEntry entry, CallbackInfo ci) {
        if (!Hooks189.ready()) return;
        UtilityModules.TabListTweaks t = CoolClient.get().modules().get(UtilityModules.TabListTweaks.class);
        if (t == null || !t.isEnabled() || !t.numericPing.on()) return;
        int ping = entry.getLatency();
        String s = String.valueOf(ping);
        MinecraftClient mc = MinecraftClient.getInstance();
        int color = t.colorPing.on() ? UtilityModules.TabListTweaks.pingColor(ping) : 0xFFFFFFFF;
        float sx = x + width - mc.textRenderer.getStringWidth(s) * 0.75f - 1;
        com.mojang.blaze3d.platform.GlStateManager.pushMatrix();
        com.mojang.blaze3d.platform.GlStateManager.translate(sx, y + 1.5f, 0f);
        com.mojang.blaze3d.platform.GlStateManager.scale(0.75f, 0.75f, 1f);
        mc.textRenderer.drawWithShadow(s, 0, 0, color);
        com.mojang.blaze3d.platform.GlStateManager.popMatrix();
        ci.cancel();
    }
}
