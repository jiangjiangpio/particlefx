package dev.particlefx.shape;
import dev.particlefx.math.Vec3;
public final class PlaneShape implements ParticleShape {
    public Vec3 sample(int i,int n,ShapeSettings s) {
        return new Vec3(((i+.5)/n-.5)*s.width,0,(ShapeMath.radicalInverse(i+1,2)-.5)*s.length);
    }
}
