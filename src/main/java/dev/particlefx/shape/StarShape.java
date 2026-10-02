package dev.particlefx.shape;
import dev.particlefx.math.Vec3;
public final class StarShape implements ParticleShape {
    public Vec3 sample(int i,int n,ShapeSettings s) {
        double u=(double)i/n*s.points*2; int edge=(int)u; double t=u-edge;
        double a=Math.PI*edge/s.points-Math.PI/2,b=Math.PI*(edge+1)/s.points-Math.PI/2;
        double r1=edge%2==0?s.radius:s.innerRadius,r2=edge%2==0?s.innerRadius:s.radius;
        return new Vec3((1-t)*r1*Math.cos(a)+t*r2*Math.cos(b),0,(1-t)*r1*Math.sin(a)+t*r2*Math.sin(b));
    }
}
