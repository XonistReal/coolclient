package dev.coolclient.v1_21_4.mixin;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.modules.perf.PerformanceModules;
import dev.coolclient.v1_21_4.Hooks21;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.Queue;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {
    @Shadow @Final private Map<ParticleRenderType, Queue<Particle>> particles;

    @Unique
    private static PerformanceModules.ParticleLimit cool$limit() {
        if (!Hooks21.ready()) return null;
        PerformanceModules.ParticleLimit l = CoolClient.get().modules().get(PerformanceModules.ParticleLimit.class);
        return l != null && l.isEnabled() ? l : null;
    }

    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    private void cool$cap(Particle particle, CallbackInfo ci) {
        PerformanceModules.ParticleLimit l = cool$limit();
        if (l == null) return;
        int total = 0;
        for (Queue<Particle> q : particles.values()) total += q.size();
        if (total >= l.max.i()) ci.cancel();
    }

    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
    private void cool$explosions(ParticleOptions options, double x, double y, double z, double vx, double vy, double vz,
                                   CallbackInfoReturnable<Particle> cir) {
        PerformanceModules.ParticleLimit l = cool$limit();
        if (l == null || !l.hideExplosions.on()) return;
        if (options.getType() == ParticleTypes.EXPLOSION || options.getType() == ParticleTypes.EXPLOSION_EMITTER) cir.setReturnValue(null);
    }
}
