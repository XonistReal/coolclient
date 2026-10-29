package dev.coolclient.v1_8_9.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import dev.coolclient.core.CoolClient;
import dev.coolclient.core.modules.utility.UtilityModules;
import dev.coolclient.core.modules.visual.Crosshair;
import dev.coolclient.v1_8_9.Hooks189;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.Window;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Unique private boolean cool$scaled;

    @Inject(method = "render", at = @At("TAIL"))
    private void cool$render(float tickDelta, CallbackInfo ci) {
        Hooks189.renderHud(tickDelta);
    }

    @Inject(method = "showCrosshair", at = @At("HEAD"), cancellable = true)
    private void cool$crosshair(CallbackInfoReturnable<Boolean> cir) {
        if (Hooks189.ready() && CoolClient.get().modules().active(Crosshair.class)) cir.setReturnValue(false);
    }

    @Inject(method = "renderBossBar", at = @At("HEAD"), cancellable = true)
    private void cool$bossBar(CallbackInfo ci) {
        if (!Hooks189.ready()) return;
        UtilityModules.BossBarTweaks b = CoolClient.get().modules().get(UtilityModules.BossBarTweaks.class);
        if (b != null && b.isEnabled() && b.hide.on()) ci.cancel();
    }

    @Unique
    private static UtilityModules.ScoreboardTweaks cool$sb() {
        if (!Hooks189.ready()) return null;
        UtilityModules.ScoreboardTweaks s = CoolClient.get().modules().get(UtilityModules.ScoreboardTweaks.class);
        return s != null && s.isEnabled() ? s : null;
    }

    @Inject(method = "renderScoreboardObjective", at = @At("HEAD"), cancellable = true)
    private void cool$sbStart(ScoreboardObjective objective, Window window, CallbackInfo ci) {
        cool$scaled = false;
        UtilityModules.ScoreboardTweaks s = cool$sb();
        if (s == null) return;
        if (s.hide.on()) {
            ci.cancel();
            return;
        }
        float scale = s.scale.f();
        if (scale != 1f) {
            float w = window.getWidth(), h = window.getHeight() / 2f;
            GlStateManager.pushMatrix();
            GlStateManager.translate(w, h, 0f);
            GlStateManager.scale(scale, scale, 1f);
            GlStateManager.translate(-w, -h, 0f);
            cool$scaled = true;
        }
    }

    @Inject(method = "renderScoreboardObjective", at = @At("RETURN"))
    private void cool$sbEnd(ScoreboardObjective objective, Window window, CallbackInfo ci) {
        if (cool$scaled) {
            GlStateManager.popMatrix();
            cool$scaled = false;
        }
    }

    @ModifyArg(method = "renderScoreboardObjective", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/hud/InGameHud;fill(IIIII)V"), index = 4)
    private int cool$sbBackground(int color) {
        UtilityModules.ScoreboardTweaks s = cool$sb();
        if (s == null) return color;
        int alpha = (color >>> 24) & 0xFF;
        // Vanilla alphas are 0x50/0x60; scale them relative to the default 30%.
        int a = Math.min(255, Math.round(alpha * s.background.f() / 30f));
        return (a << 24) | (color & 0xFFFFFF);
    }

    @Redirect(method = "renderScoreboardObjective", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/font/TextRenderer;draw(Ljava/lang/String;III)I"))
    private int cool$sbText(TextRenderer tr, String text, int x, int y, int color) {
        UtilityModules.ScoreboardTweaks s = cool$sb();
        if (s != null && s.hideNumbers.on() && text.startsWith("§c") && cool$isNumber(text.substring(2))) return 0;
        if (s != null && s.shadow.on()) return tr.drawWithShadow(text, x, y, color);
        return tr.draw(text, x, y, color);
    }

    @Unique
    private static boolean cool$isNumber(String s) {
        if (s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!(Character.isDigit(c) || (i == 0 && c == '-'))) return false;
        }
        return true;
    }
}
