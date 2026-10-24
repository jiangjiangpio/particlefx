package dev.particlefx.shape;
import dev.particlefx.math.Vec3;
/** Filled annulus with approximately uniform area sampling. */
public final class RingShape implements ParticleShape {
    public Vec3 sample(int i, int n, ShapeSettings s) {
        double r = Math.sqrt(s.innerRadius*s.innerRadius + (s.radius*s.radius-s.innerRadius*s.innerRadius)*(i+.5)/n);
        double a = ShapeMath.GOLDEN_ANGLE*i;
        return new Vec3(r*Math.cos(a),0,r*Math.sin(a));
    }
}
