package dev.particlefx.compat;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.server.network.ServerPlayerEntity;
/** Never requires a client mod. Return another vanilla simple particle for this viewer. */
@FunctionalInterface
public interface ParticleCompatibilityProvider {
    SimpleParticleType resolve(ServerPlayerEntity viewer,SimpleParticleType original);
}
