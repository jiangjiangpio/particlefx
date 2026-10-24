package dev.particlefx.server;
import dev.particlefx.ParticleFX;
import dev.particlefx.storage.EffectStorage;
import java.util.IdentityHashMap;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
public final class ParticleFXServer {
    private static final Map<MinecraftServer,EffectManager> SERVERS=new IdentityHashMap<>();
    public static synchronized void start(MinecraftServer server) {
        EffectStorage storage=new EffectStorage(FabricLoader.getInstance().getConfigDir().resolve("particlefx"));
        EffectManager manager=new EffectManager(server,storage);SERVERS.put(server,manager);
        try {storage.initialize();manager.reload();}
        catch(Exception e) {ParticleFX.LOGGER.error("ParticleFX initial load failed. Engine remains empty; fix JSON and run /particlefx reload.",e);}
    }
    public static synchronized EffectManager get(MinecraftServer server) {
        EffectManager m=SERVERS.get(server);if(m==null)throw new IllegalStateException("ParticleFX server has not started");return m;
    }
    public static synchronized void stop(MinecraftServer server) {EffectManager m=SERVERS.remove(server);if(m!=null)m.clear();}
    private ParticleFXServer() {}
}
