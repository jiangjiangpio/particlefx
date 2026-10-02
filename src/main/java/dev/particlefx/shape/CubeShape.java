package dev.particlefx.shape;
import dev.particlefx.math.Vec3;
/** Twelve wireframe edges, distributed by edge then segment. */
public final class CubeShape implements ParticleShape {
    public Vec3 sample(int i,int n,ShapeSettings s) {
        int edge=i%12, axis=edge/4, corner=edge%4;
        int samples=(n+11-edge)/12;
        double t=ShapeMath.unit(i/12,samples)-.5, a=(corner%2==0?-.5:.5),b=(corner<2?-.5:.5);
        return switch(axis) {
            case 0 -> new Vec3(t*s.width,a*s.height,b*s.length);
            case 1 -> new Vec3(a*s.width,t*s.height,b*s.length);
            default -> new Vec3(a*s.width,b*s.height,t*s.length);
        };
    }
}
