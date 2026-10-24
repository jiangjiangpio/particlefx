package dev.particlefx.shape;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
public final class ShapeRegistry {
    private static final Map<String,ParticleShape> SHAPES = new ConcurrentHashMap<>();
    static {
        register("point",new PointShape()); register("line",new LineShape());
        register("circle",new CircleShape()); register("ring",new RingShape());
        register("sphere",new SphereShape()); register("dome",new DomeShape());
        register("cylinder",new CylinderShape()); register("cone",new ConeShape());
        register("helix",new HelixShape()); register("spiral",new SpiralShape());
        register("cube",new CubeShape()); register("plane",new PlaneShape());
        register("star",new StarShape()); register("heart",new HeartShape());
        register("custom",new CustomShape());
    }
    public static void register(String id,ParticleShape shape) {
        if(id==null || !id.matches("[a-z][a-z0-9_:.-]{0,63}") || shape==null) throw new IllegalArgumentException("Invalid shape registration");
        if(SHAPES.putIfAbsent(id,shape)!=null) throw new IllegalArgumentException("Shape already registered: "+id);
    }
    public static ParticleShape get(String id) {
        ParticleShape s=id==null?null:SHAPES.get(id.toLowerCase(Locale.ROOT));
        if(s==null) throw new IllegalArgumentException("Unknown shape: "+id);
        return s;
    }
    public static Set<String> ids() { return Collections.unmodifiableSet(SHAPES.keySet()); }
    private ShapeRegistry() {}
}
