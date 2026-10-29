package dev.coolclient.v1_8_9.mixin;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.modules.visual.VisualTweaks;
import dev.coolclient.v1_8_9.Hooks189;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    // TAIL only runs on the game thread (the network-thread pass exits early to reschedule).

    @Inject(method = "onGameJoin", at = @At("TAIL"))
    private void cool$join(GameJoinS2CPacket packet, CallbackInfo ci) {
        if (!Hooks189.ready()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        CoolClient.get().onWorldJoin(mc.getCurrentServerEntry() == null ? null : mc.getCurrentServerEntry().address);
    }

    @Inject(method = "onPlayerRespawn", at = @At("TAIL"))
    private void cool$respawn(PlayerRespawnS2CPacket packet, CallbackInfo ci) {
        if (Hooks189.ready()) CoolClient.get().waypoints().setDimension(CoolClient.get().platform().game().dimension());
    }

    @Inject(method = "onEntityStatus", at = @At("TAIL"))
    private void cool$status(EntityStatusS2CPacket packet, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!Hooks189.ready() || mc.world == null) return;
        Entity e = packet.getEntity(mc.world);
        if (e == null) return;
        byte status = packet.getStatus();
        if (status == 2 && e == mc.player) CoolClient.get().onSelfHurt();
        if (status == 3 && e instanceof PlayerEntity) CoolClient.get().onPlayerDeath(e.getTranslationKey());
    }

    @Inject(method = "onWorldTimeUpdate", at = @At("HEAD"), cancellable = true)
    private void cool$time(WorldTimeUpdateS2CPacket packet, CallbackInfo ci) {
        if (Hooks189.ready() && CoolClient.get().modules().active(VisualTweaks.TimeChanger.class)) ci.cancel();
    }
}
