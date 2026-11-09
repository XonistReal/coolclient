package dev.coolclient.v1_8_9.mixin;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.modules.utility.UtilityTweaks;
import dev.coolclient.core.util.Time;
import dev.coolclient.v1_8_9.Hooks189;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Reconnect: a button under "Back to server list", with an optional countdown that presses it. */
@Mixin(DisconnectedScreen.class)
public abstract class DisconnectedScreenMixin extends Screen {
    @Unique private static final int COOL_ID = 9180;
    @Shadow private int reasonHeight;
    @Unique private ButtonWidget cool$button;
    @Unique private long cool$openedAt;
    @Unique private boolean cool$done;

    @Inject(method = "init", at = @At("TAIL"))
    private void cool$addButton(CallbackInfo ci) {
        cool$button = null;
        if (!Hooks189.ready() || !UtilityTweaks.Reconnect.on() || !CoolClient.get().platform().canReconnect()) return;
        if (cool$openedAt == 0) cool$openedAt = Time.millis();
        cool$button = new ButtonWidget(COOL_ID, width / 2 - 100, height / 2 + reasonHeight / 2 + textRenderer.fontHeight + 24, "Reconnect");
        buttons.add(cool$button);
    }

    @Inject(method = "buttonClicked", at = @At("HEAD"), cancellable = true)
    private void cool$clicked(ButtonWidget button, CallbackInfo ci) {
        if (button.id != COOL_ID) return;
        ci.cancel();
        cool$reconnect();
    }

    @Unique
    private void cool$reconnect() {
        cool$done = true;
        CoolClient.get().platform().reconnect();
    }

    @Override
    public void tick() {
        super.tick();
        if (cool$button == null || cool$done || !Hooks189.ready()) return;
        int left = UtilityTweaks.Reconnect.countdown(cool$openedAt);
        if (left < 0) return;
        if (left == 0) cool$reconnect();
        else cool$button.message = "Reconnect (" + left + ")";
    }
}
