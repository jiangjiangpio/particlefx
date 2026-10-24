package dev.particlefx.math;

import dev.particlefx.animation.AnimationFrame;
import dev.particlefx.effect.ParticleLayer;

/** Immutable compiled positions only when time cannot change the spatial transform. */
public final class StaticGeometry {
    public static double[] compile(ParticleLayer layer,Vec3[] points) {
        for(var animation:layer.animations) {
            String type=animation.type.toLowerCase(java.util.Locale.ROOT);
            if(type.equals("rotation")||type.equals("translation")||type.equals("scale"))return null;
        }
        double[] result=new double[points.length*3],out=new double[3];
        AnimationFrame frame=new AnimationFrame();frame.evaluate(layer,0);
        for(int i=0;i<points.length;i++){frame.transform(points[i],out);System.arraycopy(out,0,result,i*3,3);}
        return result;
    }
    private StaticGeometry(){}
}
