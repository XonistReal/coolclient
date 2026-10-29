package dev.coolclient.v1_8_9.mixin;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.modules.perf.PerformanceModules;
import dev.coolclient.v1_8_9.Hooks189;
import dev.coolclient.v1_8_9.KeyMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.world.ClientWorld;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Shadow public ClientWorld world;
    @Shadow public Screen currentScreen;

    @Inject(method = "initializeGame", at = @At("TAIL"))
    private void cool$title(CallbackInfo ci) {
        if (Hooks189.ready() && CoolClient.get().client().windowTitle.on()) {
            Display.setTitle(CoolClient.NAME + " " + CoolClient.VERSION + " | 1.8.9");
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void cool$tick(CallbackInfo ci) {
        Hooks189.tick();
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;setKeyPressed(IZ)V", ordinal = 0))
    private void cool$mouse(CallbackInfo ci) {
        if (currentScreen != null || !Hooks189.ready()) return;
        int button = Mouse.getEventButton();
        if (button >= 0) CoolClient.get().onMouseButton(button, Mouse.getEventButtonState());
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;setKeyPressed(IZ)V", ordinal = 1))
    private void cool$key(CallbackInfo ci) {
        if (currentScreen != null || !Hooks189.ready()) return;
        int key = Keyboard.getEventKey();
        if (key != 0) CoolClient.get().onKey(KeyMap.toGlfw(key), Keyboard.getEventKeyState());
    }

    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen cool$replaceScreen(Screen screen) {
        return Hooks189.replaceScreen(screen, world);
    }

    @Inject(method = "getMaxFramerate", at = @At("RETURN"), cancellable = true)
    private void cool$fps(CallbackInfoReturnable<Integer> cir) {
        if (!Hooks189.ready()) return;
        PerformanceModules.FpsLimiter l = CoolClient.get().modules().get(PerformanceModules.FpsLimiter.class);
        if (l == null || !l.isEnabled()) return;
        if (!Display.isActive()) cir.setReturnValue(l.unfocused.i());
        else if (world == null && currentScreen != null) cir.setReturnValue(l.menu.i());
    }

    @Inject(method = "connect(Lnet/minecraft/client/world/ClientWorld;Ljava/lang/String;)V", at = @At("HEAD"))
    private void cool$leave(ClientWorld newWorld, String message, CallbackInfo ci) {
        if (newWorld == null && Hooks189.ready()) CoolClient.get().onWorldLeave();
    }

    @Inject(method = "stop", at = @At("HEAD"))
    private void cool$stop(CallbackInfo ci) {
        if (Hooks189.ready()) CoolClient.get().onShutdown();
    }
}
