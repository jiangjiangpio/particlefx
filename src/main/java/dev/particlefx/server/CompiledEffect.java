package dev.particlefx.server;
import dev.particlefx.animation.Animation;
import dev.particlefx.effect.ParticleEffect;
import dev.particlefx.effect.ParticleLayer;
import dev.particlefx.math.Vec3;
import dev.particlefx.shape.ShapeRegistry;
import dev.particlefx.storage.*;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public final class CompiledEffect {
    final ParticleEffect definition;
    final List<Layer> layers;
    public record Layer(ParticleLayer definition,SimpleParticleType particle,Vec3[] geometry) {}
    public CompiledEffect(ParticleEffect input,EngineConfig config) {
        EffectValidator.validate(input);definition=JsonSupport.copy(input);
        List<Layer> built=new ArrayList<>();
        int remaining=config.maxParticlesPerEffect;
        for(ParticleLayer l:definition.layers) {
            SimpleParticleType particle=particle(l.particle);
            double density=l.density;
            for(Animation a:l.animations)if(a.type.equalsIgnoreCase("density"))density*=Math.max(a.from,a.speed==null?a.to:a.from+a.speed*a.durationTicks);
            int count=(int)Math.min(remaining,Math.ceil(l.count*density));
            if(count<0)count=0;
            if(l.count*density>remaining)dev.particlefx.ParticleFX.LOGGER.warn("Effect {} geometry truncated at {} particles per emission",definition.name,config.maxParticlesPerEffect);
            remaining-=count;Vec3[] geometry=new Vec3[count];var shape=ShapeRegistry.get(l.shape.type);
            for(int i=0;i<count;i++) {
                geometry[i]=shape.sample(i,count,l.shape);
                if(geometry[i]==null||!geometry[i].finite())throw new IllegalArgumentException("Nonfinite shape sample: "+l.shape.type);
            }
            built.add(new Layer(l,particle,geometry));
        }
        layers=List.copyOf(built);
    }
    public static SimpleParticleType particle(String id) {
        EffectValidator.particleId(id);Identifier key=Identifier.of(id);
        if(!Registries.PARTICLE_TYPE.containsId(key)||!(Registries.PARTICLE_TYPE.get(key) instanceof SimpleParticleType simple))
            throw new IllegalArgumentException("Unknown or parameterized particle (v1 supports simple vanilla types): "+id);
        return simple;
    }
}
