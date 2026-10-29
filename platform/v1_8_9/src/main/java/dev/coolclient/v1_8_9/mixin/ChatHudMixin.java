package dev.coolclient.v1_8_9.mixin;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.modules.utility.UtilityModules;
import dev.coolclient.v1_8_9.Hooks189;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public abstract class ChatHudMixin {
    @Unique private static final int COOL_ID_BASE = 0x41550000;
    @Unique private String cool$lastPlain;
    @Unique private int cool$count;
    @Unique private int cool$stackId;
    @Unique private int cool$nextId = COOL_ID_BASE;
    @Unique private boolean cool$reentrant;

    @Shadow
    public abstract void addMessage(Text message, int id);

    @Shadow
    public abstract void removeMessage(int id);

    @Unique
    private static UtilityModules.ChatTweaks cool$tweaks() {
        if (!Hooks189.ready()) return null;
        UtilityModules.ChatTweaks c = CoolClient.get().modules().get(UtilityModules.ChatTweaks.class);
        return c != null && c.isEnabled() ? c : null;
    }

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"))
    private void cool$received(Text message, CallbackInfo ci) {
        if (Hooks189.ready()) CoolClient.get().onChat(message.asUnformattedString());
    }

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;I)V", at = @At("HEAD"), cancellable = true)
    private void cool$decorate(Text message, int id, CallbackInfo ci) {
        if (cool$reentrant) return;
        UtilityModules.ChatTweaks c = cool$tweaks();
        if (c == null || (!c.stack.on() && !c.timestamps.on())) return;
        Text out = message;
        int useId = id;
        if (c.stack.on() && id == 0) {
            String plain = message.asUnformattedString();
            if (plain.equals(cool$lastPlain)) {
                cool$count++;
                removeMessage(cool$stackId);
                out = message.copy().append(new LiteralText(" §7(x" + cool$count + ")"));
            } else {
                cool$lastPlain = plain;
                cool$count = 1;
                cool$stackId = cool$nextId++;
                if (cool$nextId > COOL_ID_BASE + 0xFFFF) cool$nextId = COOL_ID_BASE;
            }
            useId = cool$stackId;
        }
        if (c.timestamps.on()) out = new LiteralText(c.timestamp()).append(out);
        cool$reentrant = true;
        try {
            addMessage(out, useId);
        } finally {
            cool$reentrant = false;
        }
        ci.cancel();
    }

    @ModifyConstant(method = "addMessage(Lnet/minecraft/text/Text;IIZ)V", constant = @Constant(intValue = 100))
    private int cool$history(int max) {
        UtilityModules.ChatTweaks c = cool$tweaks();
        return c != null && c.longHistory.on() ? 1000 : max;
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHud;fill(IIIII)V", ordinal = 0), index = 4)
    private int cool$background(int color) {
        UtilityModules.ChatTweaks c = cool$tweaks();
        if (c == null) return color;
        int a = (color >>> 24) & 0xFF;
        return (Math.min(255, Math.round(a * c.background.f() / 50f)) << 24) | (color & 0xFFFFFF);
    }
}
