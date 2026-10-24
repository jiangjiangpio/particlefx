package dev.particlefx.server;
import dev.particlefx.storage.EngineConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
public final class EmissionBudget {
    private int particles,packets;
    private final Map<UUID,Integer> playerPackets=new HashMap<>();
    public void reset() {particles=0;packets=0;playerPackets.clear();}
    public boolean particle(EngineConfig c) {if(particles>=c.maxParticlesPerTick)return false;particles++;return true;}
    public boolean packet(UUID player,EngineConfig c) {
        int used=playerPackets.getOrDefault(player,0);
        if(packets>=c.maxPacketsPerTick||used>=c.maxPacketsPerPlayerPerTick)return false;
        packets++;playerPackets.put(player,used+1);return true;
    }
    public int particles(){return particles;}
    public int packets(){return packets;}
}
