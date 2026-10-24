package dev.particlefx.api;
import dev.particlefx.compat.ParticleCompatibilityProvider;
import dev.particlefx.effect.*;
import dev.particlefx.group.ParticleEffectGroup;
import dev.particlefx.permission.*;
import dev.particlefx.server.ParticleFXServer;
import dev.particlefx.shape.*;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

/** v1 API: all runtime operations must run on the owning server thread. */
public final class ParticleFXAPI {
    public static UUID play(String name,ServerWorld world,Vec3d position) {return ParticleFXServer.get(world.getServer()).play(name,world,position,null,null,0);}
    public static UUID play(String name,ServerWorld world,Vec3d position,Set<UUID> viewers) {return ParticleFXServer.get(world.getServer()).play(name,world,position,null,viewers,0);}
    public static UUID playGroup(String name,ServerWorld world,Vec3d position) {return ParticleFXServer.get(world.getServer()).playGroup(name,world,position,null,null);}
    public static UUID attach(String name,LivingEntity entity) {
        if(!(entity.getEntityWorld() instanceof ServerWorld world))throw new IllegalArgumentException("Server entity required");
        return ParticleFXServer.get(world.getServer()).attach(name,entity,null);
    }
    public static void stop(MinecraftServer server,UUID handle){ParticleFXServer.get(server).control(handle,"stop");}
    public static void pause(MinecraftServer server,UUID handle){ParticleFXServer.get(server).control(handle,"pause");}
    public static void resume(MinecraftServer server,UUID handle){ParticleFXServer.get(server).control(handle,"resume");}
    public static EffectState state(MinecraftServer server,UUID handle){return ParticleFXServer.get(server).state(handle);}
    public static int detach(MinecraftServer server,UUID target){return ParticleFXServer.get(server).detach(target,null);}
    public static void registerEffect(MinecraftServer server,ParticleEffect effect,boolean persist) throws IOException {ParticleFXServer.get(server).register(effect,persist);}
    public static ParticleEffect getEffect(MinecraftServer server,String name){return ParticleFXServer.get(server).definition(name);}
    public static ParticleEffectGroup getGroup(MinecraftServer server,String name){return ParticleFXServer.get(server).groupDefinition(name);}
    public static void registerShape(String id,ParticleShape shape){ShapeRegistry.register(id,shape);}
    public static void registerCompatibilityProvider(MinecraftServer server,ParticleCompatibilityProvider provider){ParticleFXServer.get(server).addCompatibilityProvider(provider);}
    public static void setPermissionProvider(PermissionProvider provider){ParticleFXPermissions.setProvider(provider);}
    private ParticleFXAPI() {}
}
