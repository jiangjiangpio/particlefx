package dev.particlefx.shape;
import dev.particlefx.math.Vec3;
public final class SpiralShape implements ParticleShape {
    public Vec3 sample(int i,int n,ShapeSettings s) {
        double t=ShapeMath.unit(i,n),a=ShapeMath.TAU*s.turns*t,r=s.radius*t;
        return new Vec3(r*Math.cos(a),0,r*Math.sin(a));
    }
}
