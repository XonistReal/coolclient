package dev.coolclient.v1_21_4.mixin;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.modules.utility.UtilityModules;
import dev.coolclient.v1_21_4.Hooks21;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
    @Shadow @Final private List<GuiMessage> allMessages;
    @Unique private String cool$lastPlain;
    @Unique private int cool$count;
    @Unique private boolean cool$reentrant;

    @Shadow
    public abstract void addMessage(Component message, MessageSignature signature, GuiMessageTag tag);

    @Unique
    private static UtilityModules.ChatTweaks cool$tweaks() {
        if (!Hooks21.ready()) return null;
        UtilityModules.ChatTweaks c = CoolClient.get().modules().get(UtilityModules.ChatTweaks.class);
        return c != null && c.isEnabled() ? c : null;
    }

    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
            at = @At("HEAD"), cancellable = true)
    private void cool$decorate(Component message, MessageSignature signature, GuiMessageTag tag, CallbackInfo ci) {
        if (cool$reentrant || !Hooks21.ready()) return;
        String plain = message.getString();
        CoolClient.get().onChat(plain);
        UtilityModules.NickHider nick = CoolClient.get().modules().get(UtilityModules.NickHider.class);
        Component out = message;
        if (nick != null && nick.isEnabled()) {
            String name = Minecraft.getInstance().getUser().getName();
            if (plain.contains(name)) out = Component.literal(nick.apply(plain, name));
        }
        UtilityModules.ChatTweaks c = cool$tweaks();
        if (c != null && c.stack.on() && signature == null) {
            if (plain.equals(cool$lastPlain) && !allMessages.isEmpty()) {
                cool$count++;
                allMessages.remove(0);
                ((ChatComponentInvoker) this).cool$refresh();
                out = out.copy().append(Component.literal(" §7(x" + cool$count + ")"));
            } else {
                cool$lastPlain = plain;
                cool$count = 1;
            }
        }
        if (c != null && c.timestamps.on()) out = Component.literal(c.timestamp()).append(out);
        if (out == message) return;
        cool$reentrant = true;
        try {
            addMessage(out, signature, tag);
        } finally {
            cool$reentrant = false;
        }
        ci.cancel();
    }

    @ModifyConstant(method = {"addMessageToDisplayQueue", "addMessageToQueue"}, constant = @Constant(intValue = 100))
    private int cool$history(int max) {
        UtilityModules.ChatTweaks c = cool$tweaks();
        return c != null && c.longHistory.on() ? 1000 : max;
    }
}
