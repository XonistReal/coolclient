package dev.coolclient.v1_8_9.mixin;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.modules.utility.UtilityModules;
import dev.coolclient.v1_8_9.Hooks189;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(TextRenderer.class)
public abstract class TextRendererMixin {
    @Unique
    private static String cool$nick(String text) {
        if (text == null || !Hooks189.ready()) return text;
        UtilityModules.NickHider n = CoolClient.get().modules().get(UtilityModules.NickHider.class);
        if (n == null || !n.isEnabled()) return text;
        return n.apply(text, MinecraftClient.getInstance().getSession().getUsername());
    }

    @ModifyVariable(method = "draw(Ljava/lang/String;FFIZ)I", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private String cool$nickDraw(String text) {
        return cool$nick(text);
    }

    @ModifyVariable(method = "getStringWidth", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private String cool$nickWidth(String text) {
        return cool$nick(text);
    }
}
