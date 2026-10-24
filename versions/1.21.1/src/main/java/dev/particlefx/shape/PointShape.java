package dev.particlefx.shape;
import dev.particlefx.math.Vec3;
public final class PointShape implements ParticleShape {
    public Vec3 sample(int i, int n, ShapeSettings s) { return Vec3.ZERO; }
}
