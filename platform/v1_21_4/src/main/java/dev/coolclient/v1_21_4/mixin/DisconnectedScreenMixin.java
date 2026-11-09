package dev.coolclient.v1_21_4.mixin;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.modules.utility.UtilityTweaks;
import dev.coolclient.core.util.Time;
import dev.coolclient.v1_21_4.Hooks21;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Reconnect: a button under the disconnect reason, with an optional countdown that presses it. */
@Mixin(DisconnectedScreen.class)
public abstract class DisconnectedScreenMixin extends Screen {
    @Shadow @Final private LinearLayout layout;
    @Unique private Button cool$button;
    @Unique private long cool$openedAt;
    @Unique private boolean cool$done;

    protected DisconnectedScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/layouts/LinearLayout;arrangeElements()V"))
    private void cool$addButton(CallbackInfo ci) {
        cool$button = null;
        if (!Hooks21.ready() || !UtilityTweaks.Reconnect.on() || !CoolClient.get().platform().canReconnect()) return;
        if (cool$openedAt == 0) cool$openedAt = Time.millis();
        cool$button = layout.addChild(Button.builder(Component.literal("Reconnect"), b -> cool$reconnect()).width(200).build());
    }

    @Unique
    private void cool$reconnect() {
        cool$done = true;
        CoolClient.get().platform().reconnect();
    }

    @Override
    public void tick() {
        super.tick();
        if (cool$button == null || cool$done || !Hooks21.ready()) return;
        int left = UtilityTweaks.Reconnect.countdown(cool$openedAt);
        if (left < 0) return;
        if (left == 0) cool$reconnect();
        else cool$button.setMessage(Component.literal("Reconnect (" + left + ")"));
    }
}
