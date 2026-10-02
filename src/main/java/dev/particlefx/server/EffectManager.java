package dev.particlefx.server;
import dev.particlefx.ParticleFX;
import dev.particlefx.animation.AnimationFrame;
import dev.particlefx.compat.*;
import dev.particlefx.effect.*;
import dev.particlefx.group.ParticleEffectGroup;
import dev.particlefx.group.ParticleGroupStep;
import dev.particlefx.storage.*;
import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.state.property.Properties;
import net.minecraft.world.World;

/** Sole scheduler: bounded work on the server thread, no per-effect callbacks or threads. */
public final class EffectManager {
    private final MinecraftServer server;
    private final EffectStorage storage;
    private EngineConfig config=new EngineConfig();
    private Map<String,CompiledEffect> effects=new LinkedHashMap<>();
    private Map<String,ParticleEffectGroup> groups=new LinkedHashMap<>();
    private final LinkedHashMap<UUID,ActiveEffect> active=new LinkedHashMap<>();
    private final LinkedHashMap<UUID,ActiveGroup> activeGroups=new LinkedHashMap<>();
    private final LinkedHashMap<String,RedstoneTrigger> redstoneTriggers=new LinkedHashMap<>();
    private BedrockCompatibility bedrock;
    private final List<ParticleCompatibilityProvider> providers=new ArrayList<>();
    private final EmissionBudget budget=new EmissionBudget();
    private final AnimationFrame frame=new AnimationFrame();
    private final double[] point=new double[3];
    private final ArrayList<ServerPlayerEntity> viewers=new ArrayList<>();
    private final ArrayList<SimpleParticleType> resolved=new ArrayList<>();
    private long tick,lastWarning=-200;
    public EffectManager(MinecraftServer server,EffectStorage storage) {this.server=server;this.storage=storage;}
    public void checkThread() {if(!server.isOnThread())throw new IllegalStateException("ParticleFX API must be called on the server thread (use server.execute)");}
    public EngineConfig config() {return config;}
    public Set<String> names() {checkThread();return Collections.unmodifiableSet(effects.keySet());}
    public Set<String> groupNames() {checkThread();return Collections.unmodifiableSet(groups.keySet());}
    public ParticleEffect definition(String name) {checkThread();return JsonSupport.copy(require(name).definition);}
    public ParticleEffectGroup groupDefinition(String name) {checkThread();return JsonSupport.GSON.fromJson(JsonSupport.GSON.toJson(requireGroup(name)),ParticleEffectGroup.class);}
    public int activeCount() {return active.size();}
    public int activeGroupCount() {return activeGroups.size();}
    public int lastPackets() {return budget.packets();}
    public EffectState state(UUID id) {checkThread();ActiveEffect a=active.get(id);return a==null?EffectState.STOPPED:a.clock.state();}
    public void reload() throws IOException {
        checkThread();EngineConfig next=storage.loadConfig();Map<String,CompiledEffect> loaded=new LinkedHashMap<>();
        Map<String,ParticleEffect> definitions=storage.loadEffects();
        for(var entry:definitions.entrySet())loaded.put(entry.getKey(),new CompiledEffect(entry.getValue(),next));
        Map<String,ParticleEffectGroup> loadedGroups=storage.loadGroups(definitions.keySet());
        List<RedstoneTrigger> loadedTriggers=storage.loadTriggers();
        for(RedstoneTrigger trigger:loadedTriggers)EffectValidator.validateTrigger(trigger,definitions.keySet(),loadedGroups.keySet());
        BedrockCompatibility nextBedrock=new BedrockCompatibility(next);
        clear();config=next;effects=loaded;bedrock=nextBedrock;
        groups=loadedGroups;redstoneTriggers.clear();
        for(RedstoneTrigger trigger:loadedTriggers)redstoneTriggers.put(trigger.key(),trigger);
        storage.saveTriggers(new ArrayList<>(redstoneTriggers.values()));
        ParticleFX.LOGGER.info("Loaded {} ParticleFX effects; active instances cleared",effects.size());
    }
    public void register(ParticleEffect effect,boolean persist) throws IOException {
        checkThread();if(!effects.containsKey(effect.name)&&effects.size()>=EffectValidator.MAX_EFFECTS)throw new IllegalArgumentException("Effect registry limit reached");
        CompiledEffect compiled=new CompiledEffect(effect,config);
        if(persist)storage.save(compiled.definition);
        control(effect.name,null,"stop");effects.put(effect.name,compiled);
    }
    public void delete(String name) throws IOException {checkThread();require(name);storage.delete(name);control(name,null,"stop");effects.remove(name);}
    public UUID play(String name,ServerWorld world,Vec3d position,UUID owner,Set<UUID> audience,int maxAge) {
        return start(name,world,position,null,owner,audience,maxAge);
    }
    public UUID attach(String name,LivingEntity entity,UUID owner) {
        if(!(entity.getEntityWorld() instanceof ServerWorld world)||!entity.isAlive())throw new IllegalArgumentException("Target must be a living server entity");
        return start(name,world,entity.getEntityPos(),entity.getUuid(),owner,null,0);
    }
    public UUID playGroup(String name,ServerWorld world,Vec3d position,UUID owner,Set<UUID> audience) {
        checkThread();ParticleEffectGroup group=requireGroup(name);
        if(activeGroups.size()>=config.maxActiveEffects)throw new IllegalArgumentException("maxActiveEffects reached");
        if(owner!=null&&activeGroups.values().stream().filter(a->owner.equals(a.owner)).count()>=config.maxEffectsPerPlayer)
            throw new IllegalArgumentException("maxEffectsPerPlayer reached");
        ActiveGroup activeGroup=new ActiveGroup(group,world.getRegistryKey(),position,owner,audience);
        activeGroups.put(activeGroup.id,activeGroup);return activeGroup.id;
    }
    public int controlGroup(String name,UUID owner,String action) {
        checkThread();
        if(!Set.of("stop","pause","resume").contains(action))throw new IllegalArgumentException("Unknown group control action");
        int count=0;
        for(var it=activeGroups.values().iterator();it.hasNext();) {
            ActiveGroup group=it.next();
            if(!group.definition.name.equals(name)||(owner!=null&&!owner.equals(group.owner)))continue;
            count++;
            if(action.equals("stop")) {stopChildren(group);it.remove();}
            else if(action.equals("pause")) {group.paused=true;pauseChildren(group);}
            else {group.paused=false;resumeChildren(group);}
        }
        return count;
    }
    public void addRedstoneTrigger(ServerWorld world,BlockPos pos,String targetType,String target) throws IOException {
        checkThread();
        requireTriggerTarget(targetType,target);
        RedstoneTrigger trigger=new RedstoneTrigger(world.getRegistryKey().getValue().toString(),pos,targetType,target);
        redstoneTriggers.put(trigger.key(),trigger);storage.saveTriggers(new ArrayList<>(redstoneTriggers.values()));
    }
    public boolean removeRedstoneTrigger(ServerWorld world,BlockPos pos) throws IOException {
        checkThread();String key=world.getRegistryKey().getValue().toString()+"|"+pos.getX()+"|"+pos.getY()+"|"+pos.getZ();
        if(redstoneTriggers.remove(key)==null)return false;
        storage.saveTriggers(new ArrayList<>(redstoneTriggers.values()));return true;
    }
    public List<RedstoneTrigger> redstoneTriggers() {checkThread();return List.copyOf(redstoneTriggers.values());}
    private UUID start(String name,ServerWorld world,Vec3d position,UUID target,UUID owner,Set<UUID> audience,int maxAge) {
        checkThread();CompiledEffect compiled=require(name);
        if(world.getServer()!=server)throw new IllegalArgumentException("World belongs to another server");
        if(!Double.isFinite(position.x)||!Double.isFinite(position.y)||!Double.isFinite(position.z))throw new IllegalArgumentException("Position must be finite");
        if(active.size()>=config.maxActiveEffects) {warnLimit();throw new IllegalArgumentException("maxActiveEffects reached");}
        if(owner!=null&&active.values().stream().filter(a->owner.equals(a.owner)).count()>=config.maxEffectsPerPlayer)
            throw new IllegalArgumentException("maxEffectsPerPlayer reached");
        ActiveEffect a=new ActiveEffect(compiled,world.getRegistryKey(),position,target,owner,audience,maxAge);active.put(a.id,a);return a.id;
    }
    public int control(String name,UUID owner,String action) {
        checkThread();return change(a->a.compiled.definition.name.equals(name)&&(owner==null||owner.equals(a.owner)),action);
    }
    public int control(UUID id,String action) {checkThread();return change(a->a.id.equals(id),action);}
    public int detach(UUID target,UUID owner) {checkThread();return change(a->target.equals(a.target)&&(owner==null||owner.equals(a.owner)),"stop");}
    private int change(Predicate<ActiveEffect> match,String action) {
        if(!Set.of("stop","pause","resume").contains(action))throw new IllegalArgumentException("Unknown control action");
        int n=0;for(var it=active.values().iterator();it.hasNext();) {ActiveEffect a=it.next();if(!match.test(a))continue;n++;
            switch(action) {case "stop"->{a.clock.stop();it.remove();}case "pause"->a.clock.pause();case "resume"->a.clock.resume();default->throw new IllegalStateException();}
        }return n;
    }
    public void forget(UUID player) {
        checkThread();change(a->player.equals(a.owner)||player.equals(a.target),"stop");
        for(var it=activeGroups.values().iterator();it.hasNext();) {
            ActiveGroup group=it.next();if(player.equals(group.owner)){stopChildren(group);it.remove();}
        }
        if(bedrock!=null)bedrock.forget(player);
    }
    public void clear() {
        checkThread();active.values().forEach(a->a.clock.stop());active.clear();
        activeGroups.values().forEach(this::stopChildren);activeGroups.clear();
        if(bedrock!=null)bedrock.clear();viewers.clear();resolved.clear();
    }
    public void addCompatibilityProvider(ParticleCompatibilityProvider p) {checkThread();providers.add(Objects.requireNonNull(p));}
    public void tick() {
        checkThread();tick++;budget.reset();
        tickGroups();
        tickRedstone();
        // Rotate insertion order to prevent an early expensive effect starving all later effects.
        if(active.size()>1) {var it=active.entrySet().iterator();var first=it.next();it.remove();active.put(first.getKey(),first.getValue());}
        for(var it=active.values().iterator();it.hasNext();) {ActiveEffect a=it.next();
            try {
                ServerWorld world=server.getWorld(a.world);
                if(world==null||invalidOwner(a)) {a.clock.stop();it.remove();continue;}
                if(a.target!=null) {var entity=world.getEntity(a.target);if(entity==null||!entity.isAlive()||entity.isRemoved()){a.clock.stop();it.remove();continue;}a.position=entity.getEntityPos();}
                if(a.clock.state()==EffectState.PAUSED)continue;
                emit(a,world);a.clock.advance(a.compiled.definition.mode,a.compiled.definition.durationTicks);
                if(a.maxAge>0&&a.clock.age()>=a.maxAge)a.clock.stop();
                if(a.clock.state()==EffectState.STOPPED)it.remove();
            } catch(RuntimeException e) {a.clock.stop();it.remove();ParticleFX.LOGGER.error("Stopped faulty effect {}",a.compiled.definition.name,e);}
        }
        viewers.clear();resolved.clear();
    }
    private void tickGroups() {
        for(var it=activeGroups.values().iterator();it.hasNext();) {
            ActiveGroup group=it.next();
            if(group.paused)continue;
            ServerWorld world=server.getWorld(group.world);
            if(world==null||(group.owner!=null&&server.getPlayerManager().getPlayer(group.owner)==null)) {
                stopChildren(group);it.remove();continue;
            }
            try {
                for(int i=0;i<group.definition.steps.size();i++) {
                    if(group.startedSteps.contains(i))continue;
                    ParticleGroupStep step=group.definition.steps.get(i);
                    if(group.age<step.delayTicks)continue;
                    UUID child=start(step.effect,world,group.position.add(step.offset.x(),step.offset.y(),step.offset.z()),null,null,group.viewers,0);
                    group.children.add(child);group.startedSteps.add(i);
                }
                group.age++;
                if(group.age>=group.definition.durationTicks){stopChildren(group);it.remove();}
            } catch(RuntimeException e) {
                stopChildren(group);it.remove();
                ParticleFX.LOGGER.error("Stopped faulty ParticleFX group {}",group.definition.name,e);
            }
        }
    }
    private void tickRedstone() {
        for(RedstoneTrigger trigger:redstoneTriggers.values()) {
            ServerWorld world=server.getWorld(worldKey(trigger.dimension));
            if(world==null)continue;
            BlockPos pos=trigger.pos();BlockState state=world.getBlockState(pos);
            boolean powered=world.getReceivedRedstonePower(pos)>0||(state.contains(Properties.LIT)&&state.get(Properties.LIT));
            if(powered&&!trigger.powered) {
                try {
                    Vec3d position=Vec3d.ofCenter(pos);
                    if(trigger.targetType.equals("group"))playGroup(trigger.target,world,position,null,null);
                    else play(trigger.target,world,position,null,null,0);
                } catch(RuntimeException e) {
                    ParticleFX.LOGGER.warn("Redstone trigger {} could not start {}",trigger.key(),trigger.target,e);
                }
            }
            trigger.powered=powered;
        }
    }
    private static RegistryKey<World> worldKey(String value) {
        return RegistryKey.of(RegistryKeys.WORLD,Identifier.of(value));
    }
    private void pauseChildren(ActiveGroup group) {for(UUID id:group.children){ActiveEffect child=active.get(id);if(child!=null)child.clock.pause();}}
    private void resumeChildren(ActiveGroup group) {for(UUID id:group.children){ActiveEffect child=active.get(id);if(child!=null)child.clock.resume();}}
    private void stopChildren(ActiveGroup group) {for(UUID id:group.children){ActiveEffect child=active.get(id);if(child!=null)child.clock.stop();}group.children.clear();}
    private void requireTriggerTarget(String type,String target) {
        EffectValidator.require(type!=null&&Set.of("effect","group").contains(type),"Invalid trigger target type");
        EffectValidator.name(target);
        if(type.equals("effect"))require(target);else requireGroup(target);
    }
    private boolean invalidOwner(ActiveEffect a) {
        if(a.owner==null)return false;var p=server.getPlayerManager().getPlayer(a.owner);
        return p==null||!p.isAlive()||!p.getEntityWorld().getRegistryKey().equals(a.world);
    }
    private void emit(ActiveEffect a,ServerWorld world) {
        var e=a.compiled.definition;double distance=Math.min(config.maxViewDistance,e.viewDistance),d2=distance*distance;
        viewers.clear();
        for(ServerPlayerEntity p:world.getPlayers())if((a.viewers==null||a.viewers.contains(p.getUuid()))&&p.getEntityPos().squaredDistanceTo(a.position)<=d2)viewers.add(p);
        if(viewers.isEmpty())return;long local=a.clock.localTick(e.mode,e.durationTicks);int emitted=0;
        for(CompiledEffect.Layer layer:a.compiled.layers) {
            ParticleLayer l=layer.definition();long age=local-l.delay;
            if(age<0||age%l.intervalTicks!=0)continue;frame.evaluate(l,age);if(age>=frame.lifetime)continue;
            int count=(int)Math.min(layer.geometry().length,Math.ceil(l.count*frame.density));
            resolved.clear();
            for(ServerPlayerEntity p:viewers) {
                SimpleParticleType type=bedrock==null?layer.particle():bedrock.resolve(p,layer.particle());
                for(ParticleCompatibilityProvider provider:providers)type=provider.resolve(p,type);
                if(type==null||!net.minecraft.registry.Registries.PARTICLE_TYPE.getId(type).getNamespace().equals("minecraft"))throw new IllegalArgumentException("Compatibility provider must return a vanilla particle");
                resolved.add(type);
            }
            for(int i=0;i<count;i++) {
                if(emitted>=config.maxParticlesPerEffect||!budget.particle(config)){warnLimit();return;}emitted++;
                frame.transform(layer.geometry()[(int)((long)i*layer.geometry().length/count)],point);
                double x=a.position.x+point[0],y=a.position.y+point[1],z=a.position.z+point[2];
                if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z)||Math.abs(x)>30000000||Math.abs(z)>30000000||Math.abs(y)>20000000)continue;
                ParticleS2CPacket original=null;
                for(int p=0;p<viewers.size();p++) {
                    ServerPlayerEntity viewer=viewers.get(p);if(viewer.getEntityPos().squaredDistanceTo(x,y,z)>d2)continue;
                    if(!budget.packet(viewer.getUuid(),config)){warnLimit();continue;}
                    SimpleParticleType type=resolved.get(p);
                    if(type==layer.particle()) {
                        if(original==null)original=new ParticleS2CPacket(type,true,false,x,y,z,0,0,0,(float)l.speed,1);
                        viewer.networkHandler.sendPacket(original);
                    } else viewer.networkHandler.sendPacket(new ParticleS2CPacket(type,true,false,x,y,z,0,0,0,(float)l.speed,1));
                }
            }
        }
    }
    private void warnLimit() {if(tick-lastWarning>=200){lastWarning=tick;ParticleFX.LOGGER.warn("ParticleFX safety budget reached; truncated emission (particles={}, packets={}, active={})",budget.particles(),budget.packets(),active.size());}}
    private CompiledEffect require(String name) {CompiledEffect e=effects.get(name);if(e==null)throw new IllegalArgumentException("Unknown effect: "+name);return e;}
    private ParticleEffectGroup requireGroup(String name) {ParticleEffectGroup group=groups.get(name);if(group==null)throw new IllegalArgumentException("Unknown group: "+name);return group;}
}
