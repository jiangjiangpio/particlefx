package dev.particlefx.server;

import dev.particlefx.group.ParticleEffectGroup;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

final class ActiveGroup {
    final UUID id=UUID.randomUUID();
    final ParticleEffectGroup definition;
    final ResourceKey<Level> world;
    final UUID owner;
    final Set<UUID> viewers;
    final Vec3 position;
    final Set<Integer> startedSteps=new HashSet<>();
    final Set<UUID> children=new HashSet<>();
    long age;
    boolean paused;

    ActiveGroup(ParticleEffectGroup definition,ResourceKey<Level> world,Vec3 position,UUID owner,Set<UUID> viewers) {
        this.definition=definition;
        this.world=world;
        this.position=position;
        this.owner=owner;
        this.viewers=viewers==null?null:Set.copyOf(viewers);
    }
}
