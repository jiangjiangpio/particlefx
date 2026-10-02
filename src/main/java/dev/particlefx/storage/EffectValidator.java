package dev.particlefx.storage;
import dev.particlefx.animation.Animation;
import dev.particlefx.effect.ParticleEffect;
import dev.particlefx.effect.ParticleLayer;
import dev.particlefx.group.ParticleEffectGroup;
import dev.particlefx.group.ParticleGroupStep;
import dev.particlefx.math.Vec3;
import dev.particlefx.shape.ShapeRegistry;
import dev.particlefx.shape.ShapeSettings;
import java.util.Locale;
import java.util.Set;

public final class EffectValidator {
    public static final int MAX_JSON_BYTES = 30000;
    /** Keeps pretty-printed editor JSON below the existing 30 KiB storage and payload limit. */
    public static final int MAX_CUSTOM_POINTS = 256;
    public static final int MAX_EFFECTS = 256;
    public static final int MAX_GROUPS = 256;
    public static final int MAX_GROUP_STEPS = 256;
    public static void name(String s) {
        require(s!=null && s.matches("[a-z0-9][a-z0-9_-]{0,63}"),"Name must be 1-64 lowercase letters, numbers, _ or -");
        require(!s.matches("con|prn|aux|nul|com[1-9]|lpt[1-9]"),"Reserved Windows filename: "+s);
    }
    public static void particleId(String s) { require(s!=null && s.matches("minecraft:[a-z0-9_]+"),"Only vanilla minecraft: particle IDs are supported"); }
    public static void validate(ParticleEffect e) {
        require(e!=null,"Effect cannot be null");name(e.name);
        require(e.schemaVersion==1,"Unsupported effect schemaVersion: "+e.schemaVersion);
        range(e.durationTicks,1,72000,"durationTicks");require(e.mode!=null,"Invalid playback mode");
        range(e.viewDistance,1,256,"viewDistance");
        require(e.layers!=null && !e.layers.isEmpty() && e.layers.size()<=32,"Use 1-32 layers");
        for(ParticleLayer l:e.layers) {
            require(l!=null,"Layer cannot be null");particleId(l.particle);
            require(l.shape!=null,"Missing shape");ShapeSettings s=l.shape;ShapeRegistry.get(s.type);
            range(s.radius,0,128,"radius");range(s.innerRadius,0,128,"innerRadius");
            if(s.type.equalsIgnoreCase("ring")||s.type.equalsIgnoreCase("star")) require(s.innerRadius<=s.radius,"innerRadius exceeds radius");
            range(s.height,0,256,"height");range(s.width,0,256,"width");range(s.length,0,256,"length");
            range(s.turns,-100,100,"turns");range(s.points,3,64,"points");
            require(s.vertices!=null,"Missing shape vertices");
            require(s.vertices.size()<=(s.type.equalsIgnoreCase("custom")?MAX_CUSTOM_POINTS:0),
                "Only custom shapes may contain up to "+MAX_CUSTOM_POINTS+" vertices");
            if(s.type.equalsIgnoreCase("custom"))require(!s.vertices.isEmpty(),"Custom shape needs at least one vertex");
            for(Vec3 vertex:s.vertices)vector(vertex,128,"custom vertex");
            vector(l.offset,1024,"offset");vector(l.rotation,360000,"rotation");vector(l.scale,100,"scale");
            range(l.density,0,20,"density");range(l.count,1,20000,"count");range(l.speed,0,10,"speed");
            range(l.lifetime,1,72000,"lifetime");range(l.delay,0,72000,"delay");range(l.intervalTicks,1,1200,"intervalTicks");
            require(l.animations!=null && l.animations.size()<=16,"Use at most 16 animations per layer");
            for(Animation a:l.animations) {
                require(a!=null && a.type!=null && a.axis!=null && a.mode!=null,"Invalid animation");
                require(Set.of("rotation","translation","scale","density","lifetime").contains(a.type.toLowerCase(Locale.ROOT)),"Unknown animation type");
                require(Set.of("x","y","z","all").contains(a.axis.toLowerCase(Locale.ROOT)),"Unknown animation axis");
                range(a.durationTicks,1,72000,"animation durationTicks");
                boolean multiplier=Set.of("scale","density","lifetime").contains(a.type.toLowerCase(Locale.ROOT));
                double min=multiplier?0:-360000,max=multiplier?20:360000;
                range(a.from,min,max,"animation from");range(a.to,min,max,"animation to");
                if(a.speed!=null) { range(a.speed,-3600,3600,"animation speed");range(a.from+a.speed*a.durationTicks,min,max,"animation endpoint"); }
            }
        }
        require(JsonSupport.GSON.toJson(e).getBytes(java.nio.charset.StandardCharsets.UTF_8).length<=MAX_JSON_BYTES,"Effect JSON exceeds 30000 UTF-8 bytes");
    }
    public static void validateGroup(ParticleEffectGroup group,Set<String> effects) {
        require(group!=null,"Group cannot be null");
        name(group.name);
        require(group.schemaVersion==1,"Unsupported group schemaVersion: "+group.schemaVersion);
        range(group.durationTicks,1,72000,"group durationTicks");
        require(group.steps!=null&&!group.steps.isEmpty()&&group.steps.size()<=MAX_GROUP_STEPS,"Use 1-256 group steps");
        for(ParticleGroupStep step:group.steps) {
            require(step!=null,"Group step cannot be null");
            name(step.effect);
            require(effects.contains(step.effect),"Unknown group effect: "+step.effect);
            range(step.delayTicks,0,group.durationTicks-1,"group delayTicks");
            vector(step.offset,1024,"group offset");
        }
        require(JsonSupport.GSON.toJson(group).getBytes(java.nio.charset.StandardCharsets.UTF_8).length<=MAX_JSON_BYTES,"Group JSON exceeds 30000 UTF-8 bytes");
    }
    public static void validateTrigger(RedstoneTrigger trigger,Set<String> effects,Set<String> groups) {
        require(trigger!=null,"Trigger cannot be null");
        require(trigger.dimension!=null&&trigger.dimension.matches("[a-z0-9_.-]+:[a-z0-9/_.-]+"),"Invalid trigger dimension");
        require(trigger.targetType!=null&&Set.of("effect","group").contains(trigger.targetType),"Invalid trigger target type");
        name(trigger.target);
        require(trigger.targetType.equals("effect")?effects.contains(trigger.target):groups.contains(trigger.target),
            "Unknown trigger target: "+trigger.target);
    }
    public static void range(double v,double min,double max,String field) { require(Double.isFinite(v)&&v>=min&&v<=max,field+" must be finite and in ["+min+", "+max+"]"); }
    public static void require(boolean v,String message) { if(!v)throw new IllegalArgumentException(message); }
    private static void vector(Vec3 v,double max,String field) { require(v!=null&&v.finite(),field+" must be finite");range(v.x(),-max,max,field);range(v.y(),-max,max,field);range(v.z(),-max,max,field); }
    private EffectValidator() {}
}
