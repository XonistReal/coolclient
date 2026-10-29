package dev.coolclient.v1_8_9.mixin;

import dev.coolclient.core.CoolClient;
import dev.coolclient.core.modules.perf.PerformanceModules;
import dev.coolclient.v1_8_9.Hooks189;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.particle.ParticleType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ParticleManager.class)
public abstract class ParticleManagerMixin {
    @Shadow private List<Particle>[][] particles;

    @Inject(method = "addParticle(Lnet/minecraft/client/particle/Particle;)V", at = @At("HEAD"), cancellable = true)
    private void cool$limit(Particle particle, CallbackInfo ci) {
        if (!Hooks189.ready()) return;
        PerformanceModules.ParticleLimit l = CoolClient.get().modules().get(PerformanceModules.ParticleLimit.class);
        if (l == null || !l.isEnabled()) return;
        int total = 0;
        for (List<Particle>[] layer : particles) for (List<Particle> list : layer) total += list.size();
        if (total >= l.max.i()) ci.cancel();
    }

    @Inject(method = "addParticle(IDDDDDD[I)Lnet/minecraft/client/particle/Particle;", at = @At("HEAD"), cancellable = true)
    private void cool$explosions(int id, double x, double y, double z, double vx, double vy, double vz, int[] args,
                                   CallbackInfoReturnable<Particle> cir) {
        if (!Hooks189.ready()) return;
        PerformanceModules.ParticleLimit l = CoolClient.get().modules().get(PerformanceModules.ParticleLimit.class);
        if (l == null || !l.isEnabled() || !l.hideExplosions.on()) return;
        if (id == ParticleType.EXPLOSION.getId() || id == ParticleType.LARGE_EXPLOSION.getId() || id == ParticleType.HUGE_EXPLOSION.getId()) {
            cir.setReturnValue(null);
        }
    }
}
