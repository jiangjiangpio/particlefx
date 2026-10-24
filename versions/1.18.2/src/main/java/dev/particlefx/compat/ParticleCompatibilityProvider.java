package dev.particlefx.compat;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.server.network.ServerPlayerEntity;
/** Never requires a client mod. Return another vanilla simple particle for this viewer. */
@FunctionalInterface
public interface ParticleCompatibilityProvider {
    DefaultParticleType resolve(ServerPlayerEntity viewer,DefaultParticleType original);
}
