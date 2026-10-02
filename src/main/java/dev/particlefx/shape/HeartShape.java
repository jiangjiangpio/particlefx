package dev.particlefx.shape;
import dev.particlefx.math.Vec3;
public final class HeartShape implements ParticleShape {
    public Vec3 sample(int i,int n,ShapeSettings s) {
        double t=ShapeMath.TAU*i/n,x=16*Math.pow(Math.sin(t),3);
        double y=13*Math.cos(t)-5*Math.cos(2*t)-2*Math.cos(3*t)-Math.cos(4*t);
        return new Vec3(x*s.radius/16,y*s.radius/16,0);
    }
}
