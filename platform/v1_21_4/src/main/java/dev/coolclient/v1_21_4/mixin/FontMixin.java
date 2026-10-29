package dev.coolclient.v1_21_4.mixin;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.modules.utility.UtilityModules;
import dev.coolclient.v1_21_4.Hooks21;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Nick Hider for plain-string text (HUD, scoreboard names, tab list strings). */
@Mixin(Font.class)
public abstract class FontMixin {
    @Unique
    private static String cool$nick(String text) {
        if (text == null || !Hooks21.ready()) return text;
        UtilityModules.NickHider n = CoolClient.get().modules().get(UtilityModules.NickHider.class);
        if (n == null || !n.isEnabled()) return text;
        return n.apply(text, Minecraft.getInstance().getUser().getName());
    }

    @ModifyVariable(method = "drawInBatch(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)I",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private String cool$draw(String text) {
        return cool$nick(text);
    }

    @ModifyVariable(method = "width(Ljava/lang/String;)I", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private String cool$width(String text) {
        return cool$nick(text);
    }
}
