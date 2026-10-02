package dev.particlefx.shape;
import dev.particlefx.math.Vec3;
/** Implementations must be deterministic and side-effect free. Called only when caching geometry. */
@FunctionalInterface
public interface ParticleShape {
    Vec3 sample(int index, int total, ShapeSettings settings);
}
