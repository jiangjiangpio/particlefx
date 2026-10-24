package dev.particlefx.shape;
import dev.particlefx.math.Vec3;
public final class ConeShape implements ParticleShape {
    public Vec3 sample(int i,int n,ShapeSettings s) {
        double t=1-Math.sqrt(1-(i+.5)/n),r=s.radius*(1-t),a=ShapeMath.GOLDEN_ANGLE*i;
        return new Vec3(r*Math.cos(a),t*s.height,r*Math.sin(a));
    }
}
