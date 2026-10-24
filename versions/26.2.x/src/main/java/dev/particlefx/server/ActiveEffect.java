package dev.particlefx.server;
import java.util.Set;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
final class ActiveEffect {
    final UUID id=UUID.randomUUID();
    final CompiledEffect compiled;
    final ResourceKey<Level> world;
    final UUID target,owner;
    final Set<UUID> viewers;
    final EffectClock clock=new EffectClock();
    final int maxAge;
    Vec3 position;
    ActiveEffect(CompiledEffect e,ResourceKey<Level> world,Vec3 position,UUID target,UUID owner,Set<UUID> viewers,int maxAge) {
        this.compiled=e;this.world=world;this.position=position;this.target=target;this.owner=owner;
        this.viewers=viewers==null?null:Set.copyOf(viewers);this.maxAge=maxAge;
    }
}
