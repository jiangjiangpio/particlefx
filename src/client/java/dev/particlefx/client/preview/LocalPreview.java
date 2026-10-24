package dev.particlefx.client.preview;
import dev.particlefx.effect.ParticleEffect;
import dev.particlefx.client.gui.EditorLimits;
import dev.particlefx.server.CompiledEffect;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.particle.Particle;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.Vec3d;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
public final class LocalPreview {
    private static PreviewModel model;
    private static ClientWorld world;
    private static Vec3d origin;
    private static int remaining;
    private static int phase;
    private static final Map<String,SimpleParticleType> particles=new HashMap<>();
    private static final List<Particle> visible=new ArrayList<>();
    public static void start(ParticleEffect effect) {
        MinecraftClient client=MinecraftClient.getInstance();if(client.world==null||client.player==null)throw new IllegalArgumentException("Join a world for local particle preview");
        stop();for(var layer:effect.layers)particles.put(layer.particle,CompiledEffect.particle(layer.particle));
        model=new PreviewModel(effect,EditorLimits.maxCustomPoints(),512);world=client.world;origin=client.player.getEntityPos().add(client.player.getRotationVec(1).multiply(4));remaining=100;phase=0;
    }
    public static void tick(MinecraftClient client) {
        if(model==null)return;
        if(client.world!=world||client.player==null||remaining--<=0||model.stopped()){stop();return;}
        visible.removeIf(p->!p.isAlive());
        model.visit(true,Math.min(128,2048-visible.size()),phase++,(l,x,y,z,k)->{
            var random=world.getRandom();double s=l.speed;
            Particle particle=client.particleManager.addParticle(particles.get(l.particle),origin.x+x,origin.y+y,origin.z+z,random.nextGaussian()*s,random.nextGaussian()*s,random.nextGaussian()*s);
            if(particle!=null){particle.setMaxAge(Math.min(40,particle.getMaxAge()));visible.add(particle);}
        });model.tick();
    }
    public static void stop(){for(Particle p:visible)p.markDead();visible.clear();model=null;world=null;origin=null;remaining=0;particles.clear();}
    private LocalPreview(){}
}
