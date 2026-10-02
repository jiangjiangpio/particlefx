package dev.particlefx.shape;
import dev.particlefx.math.Vec3;
public final class HelixShape implements ParticleShape {
    public Vec3 sample(int i,int n,ShapeSettings s) {
        double t=ShapeMath.unit(i,n),a=ShapeMath.TAU*s.turns*t;
        return new Vec3(s.radius*Math.cos(a),s.height*t,s.radius*Math.sin(a));
    }
}
