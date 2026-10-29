package dev.coolclient.v1_8_9.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import dev.coolclient.core.CoolClient;
import dev.coolclient.core.modules.visual.BlockOutline;
import dev.coolclient.v1_8_9.Hooks189;
import dev.coolclient.v1_8_9.World3D189;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.math.Box;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    @Unique
    private static BlockOutline cool$outline() {
        if (!Hooks189.ready()) return null;
        BlockOutline b = CoolClient.get().modules().get(BlockOutline.class);
        return b != null && b.isEnabled() ? b : null;
    }

    @Redirect(method = "drawBlockOutline", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;color(FFFF)V"))
    private void cool$outlineColor(float r, float g, float b, float a) {
        BlockOutline o = cool$outline();
        if (o == null) {
            GlStateManager.color(r, g, b, a);
            return;
        }
        int c = o.color.color();
        GlStateManager.color(((c >> 16) & 0xFF) / 255f, ((c >> 8) & 0xFF) / 255f, (c & 0xFF) / 255f, ((c >>> 24) & 0xFF) / 255f);
    }

    @Redirect(method = "drawBlockOutline", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glLineWidth(F)V"))
    private void cool$outlineWidth(float width) {
        BlockOutline o = cool$outline();
        GL11.glLineWidth(o == null ? width : o.width.f());
    }

    @Redirect(method = "drawBlockOutline", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;drawBox(Lnet/minecraft/util/math/Box;)V"))
    private void cool$outlineBox(Box box) {
        BlockOutline o = cool$outline();
        if (o != null && o.fill.on()) {
            World3D189.fillBox(box, o.fillColor.color());
            int c = o.color.color();
            GlStateManager.color(((c >> 16) & 0xFF) / 255f, ((c >> 8) & 0xFF) / 255f, (c & 0xFF) / 255f, ((c >>> 24) & 0xFF) / 255f);
        }
        WorldRenderer.drawBox(box);
    }

    /** Skybox: replaces the whole overworld sky (sun, moon and stars included). */
    @Inject(method = "renderSky", at = @At("HEAD"), cancellable = true)
    private void cool$sky(float tickDelta, int anaglyphFilter, CallbackInfo ci) {
        if (Hooks189.ready() && World3D189.sky()) ci.cancel();
    }
}
