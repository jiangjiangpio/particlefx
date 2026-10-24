package dev.particlefx.client.preview;
import dev.particlefx.animation.AnimationFrame;
import dev.particlefx.effect.*;
import dev.particlefx.math.Vec3;
import dev.particlefx.server.EffectClock;
import dev.particlefx.shape.ShapeRegistry;
import dev.particlefx.storage.*;
import java.util.ArrayList;
import java.util.List;

/** CPU geometry preview shares definitions, shape sampling, transforms and clock with server. */
public final class PreviewModel {
    @FunctionalInterface public interface Visitor {void point(ParticleLayer layer,double x,double y,double z,int index);}
    private record Layer(ParticleLayer definition,Vec3[] points){}
    private final ParticleEffect effect;
    private final List<Layer> layers=new ArrayList<>();
    private final EffectClock clock=new EffectClock();
    private final AnimationFrame frame=new AnimationFrame();
    private final double[] out=new double[3];
    public PreviewModel(ParticleEffect input) {
        EffectValidator.validate(input);effect=JsonSupport.copy(input);int remaining=2000;
        for(ParticleLayer l:effect.layers) {
            double density=l.density;
            for(var a:l.animations)if(a.type.equalsIgnoreCase("density"))density*=Math.max(a.from,a.speed==null?a.to:a.from+a.speed*a.durationTicks);
            int n=(int)Math.min(remaining,Math.ceil(l.count*density));remaining-=n;Vec3[] pts=new Vec3[n];
            var shape=ShapeRegistry.get(l.shape.type);for(int i=0;i<n;i++)pts[i]=shape.sample(i,n,l.shape);layers.add(new Layer(l,pts));
        }
    }
    public void visit(boolean emissionOnly,Visitor visitor) {
        if(clock.state()==EffectState.STOPPED)return;
        long tick=clock.localTick(effect.mode,effect.durationTicks);
        for(int k=0;k<layers.size();k++){var layer=layers.get(k);var l=layer.definition;long age=tick-l.delay;
            if(age<0||(emissionOnly&&age%l.intervalTicks!=0))continue;frame.evaluate(l,age);if(age>=frame.lifetime)continue;
            int count=(int)Math.min(layer.points.length,Math.ceil(l.count*frame.density));
            for(int i=0;i<count;i++){frame.transform(layer.points[(int)((long)i*layer.points.length/count)],out);visitor.point(l,out[0],out[1],out[2],k);}
        }
    }
    public void tick(){clock.advance(effect.mode,effect.durationTicks);}
    public boolean stopped(){return clock.state()==EffectState.STOPPED;}
}
