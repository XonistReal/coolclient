package dev.coolclient.v1_21_4.mixin;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.modules.hud.PotionEffectsHud;
import dev.coolclient.core.modules.utility.UtilityModules;
import dev.coolclient.core.modules.visual.Crosshair;
import dev.coolclient.v1_21_4.Hooks21;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @Unique private boolean cool$scaled;

    @Inject(method = "render", at = @At("TAIL"))
    private void cool$render(GuiGraphics g, DeltaTracker delta, CallbackInfo ci) {
        Hooks21.renderHud(g, delta.getGameTimeDeltaPartialTick(false));
    }

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void cool$crosshair(GuiGraphics g, DeltaTracker delta, CallbackInfo ci) {
        if (Hooks21.ready() && CoolClient.get().modules().active(Crosshair.class)) ci.cancel();
    }

    /** Hide the vanilla effect icons when Cool Client's Potion Effects HUD is on. */
    @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
    private void cool$effects(GuiGraphics g, DeltaTracker delta, CallbackInfo ci) {
        if (Hooks21.ready() && CoolClient.get().modules().active(PotionEffectsHud.class)) ci.cancel();
    }

    @Unique
    private static UtilityModules.ScoreboardTweaks cool$sb() {
        if (!Hooks21.ready()) return null;
        UtilityModules.ScoreboardTweaks s = CoolClient.get().modules().get(UtilityModules.ScoreboardTweaks.class);
        return s != null && s.isEnabled() ? s : null;
    }

    @Inject(method = "displayScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void cool$sbStart(GuiGraphics g, Objective objective, CallbackInfo ci) {
        cool$scaled = false;
        UtilityModules.ScoreboardTweaks s = cool$sb();
        if (s == null) return;
        if (s.hide.on()) {
            ci.cancel();
            return;
        }
        float scale = s.scale.f();
        if (scale != 1f) {
            float w = g.guiWidth(), h = g.guiHeight() / 2f;
            g.pose().pushPose();
            g.pose().translate(w, h, 0f);
            g.pose().scale(scale, scale, 1f);
            g.pose().translate(-w, -h, 0f);
            cool$scaled = true;
        }
    }

    @Inject(method = "displayScoreboardSidebar", at = @At("RETURN"))
    private void cool$sbEnd(GuiGraphics g, Objective objective, CallbackInfo ci) {
        if (cool$scaled) {
            g.pose().popPose();
            cool$scaled = false;
        }
    }

    @ModifyArg(method = "displayScoreboardSidebar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"), index = 4)
    private int cool$sbBackground(int color) {
        UtilityModules.ScoreboardTweaks s = cool$sb();
        if (s == null) return color;
        int a = (color >>> 24) & 0xFF;
        return (Math.min(255, Math.round(a * s.background.f() / 30f)) << 24) | (color & 0xFFFFFF);
    }

    @Redirect(method = "displayScoreboardSidebar", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I"))
    private int cool$sbText(GuiGraphics g, Font font, Component text, int x, int y, int color, boolean shadow) {
        UtilityModules.ScoreboardTweaks s = cool$sb();
        if (s == null) return g.drawString(font, text, x, y, color, shadow);
        if (s.hideNumbers.on() && cool$isScore(text.getString())) return 0;
        return g.drawString(font, text, x, y, color, s.shadow.on());
    }

    @Unique
    private static boolean cool$isScore(String s) {
        if (s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!(Character.isDigit(c) || (i == 0 && c == '-'))) return false;
        }
        return true;
    }
}
