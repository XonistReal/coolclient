package dev.coolclient.v1_21_4.mixin;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.modules.cpvp.CrystalPvpTweaks;
import dev.coolclient.v1_21_4.Hooks21;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    // TAIL only runs on the game thread (the network-thread pass bails out to reschedule).

    @Inject(method = "handleLogin", at = @At("TAIL"))
    private void cool$join(ClientboundLoginPacket packet, CallbackInfo ci) {
        if (!Hooks21.ready()) return;
        Minecraft mc = Minecraft.getInstance();
        String server = mc.isSingleplayer() || mc.getCurrentServer() == null ? null : mc.getCurrentServer().ip;
        if (server != null) dev.coolclient.v1_21_4.Platform21.lastServer = mc.getCurrentServer();
        CoolClient.get().onWorldJoin(server);
    }

    @Inject(method = "handleRespawn", at = @At("TAIL"))
    private void cool$respawn(ClientboundRespawnPacket packet, CallbackInfo ci) {
        if (Hooks21.ready()) CoolClient.get().waypoints().setDimension(CoolClient.get().platform().game().dimension());
    }

    @Inject(method = "handleEntityEvent", at = @At("TAIL"))
    private void cool$entityEvent(ClientboundEntityEventPacket packet, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (!Hooks21.ready() || mc.level == null) return;
        Entity e = packet.getEntity(mc.level);
        if (!(e instanceof Player)) return;
        String name = ((Player) e).getGameProfile().getName();
        if (packet.getEventId() == 35) CoolClient.get().onTotemPop(name, e == mc.player);
        else if (packet.getEventId() == 3) CoolClient.get().onPlayerDeath(name);
    }

    @Redirect(method = "handleEntityEvent", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/particle/ParticleEngine;createTrackingEmitter(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/particles/ParticleOptions;I)V"))
    private void cool$totemParticles(ParticleEngine engine, Entity entity, ParticleOptions particle, int lifetime) {
        if (Hooks21.ready()) {
            CrystalPvpTweaks.TotemPopAnimation t = CoolClient.get().modules().get(CrystalPvpTweaks.TotemPopAnimation.class);
            if (t != null && t.isEnabled() && t.hideParticles.on()) return;
        }
        engine.createTrackingEmitter(entity, particle, lifetime);
    }

    @Inject(method = "handleDamageEvent", at = @At("TAIL"))
    private void cool$damage(ClientboundDamageEventPacket packet, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (Hooks21.ready() && mc.player != null && packet.entityId() == mc.player.getId()) CoolClient.get().onSelfHurt();
    }

    /** Vanilla sends the world time every 20 server ticks; TPS is worked out from it. */
    @Inject(method = "handleSetTime", at = @At("TAIL"))
    private void cool$time(net.minecraft.network.protocol.game.ClientboundSetTimePacket packet, CallbackInfo ci) {
        if (Hooks21.ready()) CoolClient.get().events().post(new dev.coolclient.core.event.Events.ServerTime(packet.gameTime()));
    }
}
