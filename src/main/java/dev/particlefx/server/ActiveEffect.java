package dev.particlefx.server;
import java.util.Set;
import java.util.UUID;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
final class ActiveEffect {
    final UUID id=UUID.randomUUID();
    final CompiledEffect compiled;
    final RegistryKey<World> world;
    final UUID target,owner;
    final Set<UUID> viewers;
    final EffectClock clock=new EffectClock();
    final int maxAge;
    Vec3d position;
    ActiveEffect(CompiledEffect e,RegistryKey<World> world,Vec3d position,UUID target,UUID owner,Set<UUID> viewers,int maxAge) {
        this.compiled=e;this.world=world;this.position=position;this.target=target;this.owner=owner;
        this.viewers=viewers==null?null:Set.copyOf(viewers);this.maxAge=maxAge;
    }
}
