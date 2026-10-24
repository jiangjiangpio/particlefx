package dev.particlefx.server;

import dev.particlefx.group.ParticleEffectGroup;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

final class ActiveGroup {
    final UUID id=UUID.randomUUID();
    final ParticleEffectGroup definition;
    final RegistryKey<World> world;
    final UUID owner;
    final Set<UUID> viewers;
    final Vec3d position;
    final Set<Integer> startedSteps=new HashSet<>();
    final Set<UUID> children=new HashSet<>();
    long age;
    boolean paused;

    ActiveGroup(ParticleEffectGroup definition,RegistryKey<World> world,Vec3d position,UUID owner,Set<UUID> viewers) {
        this.definition=definition;
        this.world=world;
        this.position=position;
        this.owner=owner;
        this.viewers=viewers==null?null:Set.copyOf(viewers);
    }
}
