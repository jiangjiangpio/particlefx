package dev.particlefx.compat;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerPlayer;
/** Never requires a client mod. Return another vanilla simple particle for this viewer. */
@FunctionalInterface
public interface ParticleCompatibilityProvider {
    SimpleParticleType resolve(ServerPlayer viewer,SimpleParticleType original);
}
