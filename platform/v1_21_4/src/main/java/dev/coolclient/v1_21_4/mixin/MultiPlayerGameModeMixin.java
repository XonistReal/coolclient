package dev.coolclient.v1_21_4.mixin;

import dev.coolclient.v1_21_4.Hooks21;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
    @Inject(method = "attack", at = @At("HEAD"))
    private void cool$attack(Player player, Entity target, CallbackInfo ci) {
        Hooks21.attack(player, target);
    }
}
