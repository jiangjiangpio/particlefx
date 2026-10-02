package dev.particlefx.compat;
import dev.particlefx.ParticleFX;
import dev.particlefx.server.CompiledEffect;
import dev.particlefx.storage.EngineConfig;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Predicate;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.server.network.ServerPlayerEntity;

/** Reflection keeps both Geyser and Floodgate completely optional. No probing network traffic. */
public final class BedrockCompatibility implements ParticleCompatibilityProvider {
    private final EngineConfig config;
    private final List<Predicate<UUID>> detectors=new ArrayList<>();
    private final Map<UUID,Boolean> cache=new HashMap<>();
    private final Map<SimpleParticleType,SimpleParticleType> fallbacks=new HashMap<>();
    public BedrockCompatibility(EngineConfig config) {
        this.config=config;
        config.bedrockFallbacks.forEach((a,b)->fallbacks.put(CompiledEffect.particle(a),CompiledEffect.particle(b)));
        bind("org.geysermc.geyser.api.GeyserApi","api","isBedrockPlayer");
        bind("org.geysermc.floodgate.api.FloodgateApi","getInstance","isFloodgatePlayer");
        ParticleFX.LOGGER.info("Bedrock compatibility: mode={}, detected APIs={}",config.bedrockDetection,detectors.size());
    }
    private void bind(String className,String factory,String test) {
        try {
            Class<?> type=Class.forName(className);Object api=type.getMethod(factory).invoke(null);
            if(api==null)return;Method method=type.getMethod(test,UUID.class);
            detectors.add(uuid->{try {return Boolean.TRUE.equals(method.invoke(api,uuid));}catch(ReflectiveOperationException|RuntimeException e){return false;}});
        } catch(ClassNotFoundException ignored) {
            // Optional integration is absent; normal vanilla particle delivery continues.
        } catch(ReflectiveOperationException|LinkageError e) {ParticleFX.LOGGER.warn("Cannot initialize optional {}: {}",className,e.toString());}
    }
    public SimpleParticleType resolve(ServerPlayerEntity viewer,SimpleParticleType original) {
        boolean bedrock=switch(config.bedrockDetection) {
            case "all" -> true;
            case "off" -> false;
            default -> cache.computeIfAbsent(viewer.getUuid(),id->detectors.stream().anyMatch(d->d.test(id)));
        };
        return bedrock?fallbacks.getOrDefault(original,original):original;
    }
    public void forget(UUID uuid) {cache.remove(uuid);}
    public void clear() {cache.clear();}
}
