package dev.particlefx.shape;
import dev.particlefx.math.Vec3;
public final class SphereShape implements ParticleShape {
    public Vec3 sample(int i, int n, ShapeSettings s) {
        double y = 1-2*(i+.5)/n, r = Math.sqrt(Math.max(0,1-y*y)), a = ShapeMath.GOLDEN_ANGLE*i;
        return new Vec3(r*Math.cos(a)*s.radius,y*s.radius,r*Math.sin(a)*s.radius);
    }
}
