package dev.coolclient.v1_21_4.mixin;

import dev.coolclient.core.CoolClient;
import dev.coolclient.v1_21_4.Hooks21;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow public ClientLevel level;

    @Inject(method = "tick", at = @At("TAIL"))
    private void cool$tick(CallbackInfo ci) {
        Hooks21.tick();
    }

    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen cool$replaceScreen(Screen screen) {
        return Hooks21.replaceScreen(screen, level);
    }

    @Inject(method = "createTitle", at = @At("RETURN"), cancellable = true)
    private void cool$title(CallbackInfoReturnable<String> cir) {
        if (Hooks21.ready() && CoolClient.get().client().windowTitle.on()) {
            cir.setReturnValue(CoolClient.NAME + " " + CoolClient.VERSION + " | 1.21.4");
        }
    }

    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;Z)V", at = @At("HEAD"))
    private void cool$leave(Screen screen, boolean keepResourcePacks, CallbackInfo ci) {
        if (Hooks21.ready()) CoolClient.get().onWorldLeave();
    }

    @Inject(method = "stop", at = @At("HEAD"))
    private void cool$stop(CallbackInfo ci) {
        if (Hooks21.ready()) CoolClient.get().onShutdown();
    }
}
