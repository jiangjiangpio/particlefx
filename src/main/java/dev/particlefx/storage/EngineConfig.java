package dev.particlefx.storage;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EngineConfig {
    public int schemaVersion = 1;
    public int maxParticlesPerEffect = 5000;
    public int maxParticlesPerTick = 10000;
    public int maxPacketsPerTick = 20000;
    public int maxPacketsPerPlayerPerTick = 2000;
    public int maxActiveEffects = 100;
    public int maxEffectsPerPlayer = 5;
    public double maxViewDistance = 128;
    public boolean allowPlayerUse = true;
    public boolean allowPlayerPlay = false;
    public int opPermissionLevel = 2;
    /** auto: Geyser/Floodgate API; all: proxy without local API; off: no substitutions. */
    public String bedrockDetection = "auto";
    public Map<String,String> bedrockFallbacks = new LinkedHashMap<>(Map.of(
        "minecraft:sculk_soul","minecraft:soul",
        "minecraft:enchant","minecraft:end_rod"));
    public void validate() {
        EffectValidator.require(schemaVersion==1,"Unsupported config schemaVersion");
        EffectValidator.range(maxParticlesPerEffect,1,20000,"maxParticlesPerEffect");
        EffectValidator.range(maxParticlesPerTick,1,100000,"maxParticlesPerTick");
        EffectValidator.range(maxPacketsPerTick,1,200000,"maxPacketsPerTick");
        EffectValidator.range(maxPacketsPerPlayerPerTick,1,20000,"maxPacketsPerPlayerPerTick");
        EffectValidator.range(maxActiveEffects,1,1000,"maxActiveEffects");
        EffectValidator.range(maxEffectsPerPlayer,1,maxActiveEffects,"maxEffectsPerPlayer");
        EffectValidator.range(maxViewDistance,1,256,"maxViewDistance");
        EffectValidator.range(opPermissionLevel,1,4,"opPermissionLevel");
        EffectValidator.require(java.util.Set.of("auto","all","off").contains(bedrockDetection),"Invalid bedrockDetection");
        EffectValidator.require(bedrockFallbacks!=null && bedrockFallbacks.size()<=256,"Invalid fallback map");
        bedrockFallbacks.forEach((a,b)->{EffectValidator.particleId(a);EffectValidator.particleId(b);});
    }
}
