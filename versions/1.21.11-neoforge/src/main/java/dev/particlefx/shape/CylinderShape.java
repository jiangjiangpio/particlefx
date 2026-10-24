package dev.particlefx.shape;
import dev.particlefx.math.Vec3;
public final class CylinderShape implements ParticleShape {
    public Vec3 sample(int i,int n,ShapeSettings s) {
        double a=ShapeMath.GOLDEN_ANGLE*i;
        return new Vec3(s.radius*Math.cos(a),ShapeMath.unit(i,n)*s.height,s.radius*Math.sin(a));
    }
}
