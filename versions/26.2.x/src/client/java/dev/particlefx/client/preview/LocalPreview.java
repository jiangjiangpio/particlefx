package dev.particlefx.client.preview;
import dev.particlefx.effect.ParticleEffect;
import dev.particlefx.client.gui.EditorLimits;
import dev.particlefx.server.CompiledEffect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.SimpleParticleType;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
public final class LocalPreview {
    private static PreviewModel model;
    private static ClientLevel world;
    private static net.minecraft.world.phys.Vec3 origin;
    private static int remaining;
    private static int phase;
    private static final Map<String,SimpleParticleType> particles=new HashMap<>();
    private static final List<Particle> visible=new ArrayList<>();
    public static void start(ParticleEffect effect) {
        Minecraft minecraft=Minecraft.getInstance();if(minecraft.level==null||minecraft.player==null)throw new IllegalArgumentException("Join a world for local particle preview");
        stop();for(var layer:effect.layers)particles.put(layer.particle,CompiledEffect.particle(layer.particle));
        model=new PreviewModel(effect,EditorLimits.maxCustomPoints(),512);world=minecraft.level;origin=minecraft.player.position().add(minecraft.player.getViewVector(1).scale(4));remaining=100;phase=0;
    }
    public static void tick(Minecraft minecraft) {
        if(model==null)return;
        if(minecraft.level!=world||minecraft.player==null||remaining--<=0||model.stopped()){stop();return;}
        visible.removeIf(p->!p.isAlive());
        model.visit(true,Math.min(128,2048-visible.size()),phase++,(l,x,y,z,k)->{
            var random=world.getRandom();double s=l.speed;
            Particle particle=minecraft.particleEngine.createParticle(particles.get(l.particle),origin.x+x,origin.y+y,origin.z+z,random.nextGaussian()*s,random.nextGaussian()*s,random.nextGaussian()*s);
            if(particle!=null){particle.setLifetime(Math.min(40,particle.getLifetime()));visible.add(particle);}
        });model.tick();
    }
    public static void stop(){for(Particle p:visible)p.remove();visible.clear();model=null;world=null;origin=null;remaining=0;particles.clear();}
    private LocalPreview(){}
}


