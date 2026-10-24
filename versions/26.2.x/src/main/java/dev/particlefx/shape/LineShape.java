package dev.particlefx.shape;
import dev.particlefx.math.Vec3;
public final class LineShape implements ParticleShape {
    public Vec3 sample(int i, int n, ShapeSettings s) { return new Vec3((ShapeMath.unit(i,n) - .5) * s.length,0,0); }
}
