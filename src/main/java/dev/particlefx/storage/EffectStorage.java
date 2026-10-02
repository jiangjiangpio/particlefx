package dev.particlefx.storage;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;
import dev.particlefx.effect.ParticleEffect;
import dev.particlefx.group.ParticleEffectGroup;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** A failed reload never overwrites live state. Filenames and JSON names must agree. */
public final class EffectStorage {
    public static final String[] PRESETS={"magic_circle","healing","teleport","fire_aura","ice_aura","boss_spawn","level_up","new_year_launch","new_year_burst","new_year_greeting"};
    public static final String[] GROUP_PRESETS={"new_year_fireworks"};
    private final Path root,effects,groups,triggers;
    public EffectStorage(Path root) {
        this.root=root.toAbsolutePath().normalize();
        this.effects=this.root.resolve("effects");
        this.groups=this.root.resolve("groups");
        this.triggers=this.root.resolve("triggers.json");
    }
    public void initialize() throws IOException {
        Files.createDirectories(effects);
        Files.createDirectories(groups);
        if(!Files.exists(root.resolve("config.json"))) atomicWrite(root.resolve("config.json"),JsonSupport.GSON.toJson(new EngineConfig()));
        migrateViewDistance();
        Path marker=root.resolve(".presets-installed");
        if(!Files.exists(marker)) {
            for(String name:PRESETS) {
                Path file=path(name);
                if(!Files.exists(file)) try(InputStream in=EffectStorage.class.getResourceAsStream("/data/particlefx/presets/"+name+".json")) {
                    if(in==null) throw new IOException("Missing packaged preset: "+name);
                    atomicWrite(file,new String(in.readAllBytes(),StandardCharsets.UTF_8));
                }
            }
            atomicWrite(marker,"Preset installation completed. Deleted presets are not restored automatically.\n");
        }
        installPresetOnce("new_year_launch");
        installPresetOnce("new_year_burst");
        installPresetOnce("new_year_greeting");
        migrateNewYearViewDistance();
        installGroupOnce("new_year_fireworks");
    }
    private void migrateViewDistance() throws IOException {
        Path marker=root.resolve(".view-distance-128-installed");
        if(Files.exists(marker)) return;
        Path config=root.resolve("config.json");
        try {
            JsonObject current=JsonParser.parseString(readBounded(config)).getAsJsonObject();
            if(current.has("maxViewDistance")&&current.get("maxViewDistance").getAsDouble()==64) {
                current.addProperty("maxViewDistance",128);
                atomicWrite(config,JsonSupport.GSON.toJson(current));
            }
            atomicWrite(marker,"View distance migration completed. Existing custom values are preserved.\n");
        } catch(RuntimeException ex) {
            throw new IOException("Could not migrate config view distance",ex);
        }
    }
    private void migrateNewYearViewDistance() throws IOException {
        Path marker=root.resolve(".new-year-view-distance-128-installed");
        if(Files.exists(marker)) return;
        for(String name:List.of("new_year_launch","new_year_burst","new_year_greeting")) {
            Path file=path(name);
            if(!Files.exists(file)) continue;
            try {
                JsonObject current=JsonParser.parseString(readBounded(file)).getAsJsonObject();
                if(current.has("viewDistance")&&current.get("viewDistance").getAsDouble()==64) {
                    String packaged=readPackagedPreset(name);
                    JsonObject previous=JsonParser.parseString(packaged).getAsJsonObject();
                    previous.addProperty("viewDistance",64);
                    JsonArray layers=previous.getAsJsonArray("layers");
                    if(name.equals("new_year_launch")) layers.remove(3);
                    if(name.equals("new_year_burst")) {layers.remove(2);layers.remove(2);}
                    if(current.equals(previous)) atomicWrite(file,packaged);
                    else {
                        current.addProperty("viewDistance",128);
                        atomicWrite(file,JsonSupport.GSON.toJson(current));
                    }
                }
            } catch(RuntimeException ex) {
                throw new IOException("Could not migrate New Year preset "+name,ex);
            }
        }
        atomicWrite(marker,"New Year visibility migration completed.\n");
    }
    private static String readPackagedPreset(String name) throws IOException {
        try(InputStream in=EffectStorage.class.getResourceAsStream("/data/particlefx/presets/"+name+".json")) {
            if(in==null)throw new IOException("Missing packaged preset: "+name);
            return new String(in.readAllBytes(),StandardCharsets.UTF_8);
        }
    }
    private void installPresetOnce(String name) throws IOException {
        Path migration=root.resolve(".preset-"+name+"-installed");
        if(Files.exists(migration)) return;
        Path file=path(name);
        if(!Files.exists(file)) try(InputStream in=EffectStorage.class.getResourceAsStream("/data/particlefx/presets/"+name+".json")) {
            if(in==null) throw new IOException("Missing packaged preset: "+name);
            atomicWrite(file,new String(in.readAllBytes(),StandardCharsets.UTF_8));
        }
        atomicWrite(migration,"Preset migration completed. Deleted presets are not restored automatically.\n");
    }
    private void installGroupOnce(String name) throws IOException {
        Path migration=root.resolve(".group-"+name+"-installed");
        if(Files.exists(migration)) return;
        Path file=groupPath(name);
        if(!Files.exists(file)) try(InputStream in=EffectStorage.class.getResourceAsStream("/data/particlefx/groups/"+name+".json")) {
            if(in==null) throw new IOException("Missing packaged group: "+name);
            atomicWrite(file,new String(in.readAllBytes(),StandardCharsets.UTF_8));
        }
        atomicWrite(migration,"Group migration completed. Deleted groups are not restored automatically.\n");
    }
    public EngineConfig loadConfig() throws IOException {
        EngineConfig c=JsonSupport.GSON.fromJson(readBounded(root.resolve("config.json")),EngineConfig.class);
        if(c==null)throw new IOException("config.json cannot be null");c.validate();return c;
    }
    public Map<String,ParticleEffect> loadEffects() throws IOException {
        Map<String,ParticleEffect> result=new LinkedHashMap<>();
        try(var files=Files.list(effects)) {
            for(Path p:files.filter(f->f.getFileName().toString().endsWith(".json")).sorted().limit(EffectValidator.MAX_EFFECTS+1L).toList()) {
                if(result.size()>=EffectValidator.MAX_EFFECTS)throw new IOException("Too many effect files (maximum 256)");
                String name=p.getFileName().toString().replaceFirst("\\.json$","");
                EffectValidator.name(name);
                try {
                    ParticleEffect e=JsonSupport.GSON.fromJson(readBounded(p),ParticleEffect.class);
                    EffectValidator.validate(e);
                    EffectValidator.require(name.equals(e.name),"Filename does not match effect.name");result.put(name,e);
                } catch(RuntimeException ex) {throw new IOException(p.getFileName()+": "+ex.getMessage(),ex);}
            }
        }
        return result;
    }
    public Map<String,ParticleEffectGroup> loadGroups(Set<String> effectNames) throws IOException {
        Map<String,ParticleEffectGroup> result=new LinkedHashMap<>();
        try(var files=Files.list(groups)) {
            for(Path p:files.filter(f->f.getFileName().toString().endsWith(".json")).sorted().limit(EffectValidator.MAX_GROUPS+1L).toList()) {
                if(result.size()>=EffectValidator.MAX_GROUPS)throw new IOException("Too many group files (maximum 256)");
                String name=p.getFileName().toString().replaceFirst("\\.json$","");
                EffectValidator.name(name);
                try {
                    ParticleEffectGroup group=JsonSupport.GSON.fromJson(readBounded(p),ParticleEffectGroup.class);
                    EffectValidator.validateGroup(group,effectNames);
                    EffectValidator.require(name.equals(group.name),"Filename does not match group.name");
                    result.put(name,group);
                } catch(RuntimeException ex) {throw new IOException(p.getFileName()+": "+ex.getMessage(),ex);}
            }
        }
        return result;
    }
    public List<RedstoneTrigger> loadTriggers() throws IOException {
        if(!Files.exists(triggers)) return new ArrayList<>();
        RedstoneTrigger[] loaded=JsonSupport.GSON.fromJson(readBounded(triggers),RedstoneTrigger[].class);
        return loaded==null?new ArrayList<>():new ArrayList<>(List.of(loaded));
    }
    public void saveTriggers(List<RedstoneTrigger> values) throws IOException {
        atomicWrite(triggers,JsonSupport.GSON.toJson(values));
    }
    public void save(ParticleEffect effect) throws IOException { EffectValidator.validate(effect);atomicWrite(path(effect.name),JsonSupport.GSON.toJson(effect)); }
    public void delete(String name) throws IOException { Files.deleteIfExists(path(name)); }
    public Path path(String name) {
        EffectValidator.name(name);Path p=effects.resolve(name+".json").normalize();
        if(!p.getParent().equals(effects))throw new IllegalArgumentException("Invalid effect path");return p;
    }
    public Path groupPath(String name) {
        EffectValidator.name(name);Path p=groups.resolve(name+".json").normalize();
        if(!p.getParent().equals(groups))throw new IllegalArgumentException("Invalid group path");return p;
    }
    private static String readBounded(Path p) throws IOException {
        if(Files.isSymbolicLink(p))throw new IOException("Symlink configuration files are not allowed: "+p);
        try(InputStream in=Files.newInputStream(p)) {
            byte[] data=in.readNBytes(EffectValidator.MAX_JSON_BYTES+1);
            if(data.length>EffectValidator.MAX_JSON_BYTES)throw new IOException("JSON exceeds 30000 bytes: "+p);
            return new String(data,StandardCharsets.UTF_8);
        }
    }
    private static void atomicWrite(Path dest,String content) throws IOException {
        Path temp=Files.createTempFile(dest.getParent(),".particlefx-",".tmp");
        try {
            Files.writeString(temp,content,StandardCharsets.UTF_8);
            try {Files.move(temp,dest,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException ignored) {Files.move(temp,dest,StandardCopyOption.REPLACE_EXISTING);}
        } finally {Files.deleteIfExists(temp);}
    }
}
