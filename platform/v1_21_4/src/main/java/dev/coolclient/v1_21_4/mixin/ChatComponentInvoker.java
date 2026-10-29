package dev.coolclient.v1_21_4.mixin;

import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChatComponent.class)
public interface ChatComponentInvoker {
    @Invoker("refreshTrimmedMessages")
    void cool$refresh();
}
