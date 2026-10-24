package dev.particlefx.shape;
import dev.particlefx.math.Vec3;
public final class CircleShape implements ParticleShape {
    public Vec3 sample(int i, int n, ShapeSettings s) {
        double a = ShapeMath.TAU * i / n;
        return new Vec3(s.radius * Math.cos(a),0,s.radius * Math.sin(a));
    }
}
