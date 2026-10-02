package dev.particlefx.effect;
import dev.particlefx.animation.Animation;
import dev.particlefx.math.Vec3;
import dev.particlefx.shape.ShapeSettings;
import java.util.ArrayList;
import java.util.List;

public final class ParticleLayer {
    public String particle = "minecraft:end_rod";
    public ShapeSettings shape = new ShapeSettings();
    public Vec3 offset = Vec3.ZERO;
    /** Euler degrees, X then Y then Z; offset is applied after rotation. */
    public Vec3 rotation = Vec3.ZERO;
    public Vec3 scale = Vec3.ONE;
    public double density = 1;
    /** Base geometry samples per emission; one vanilla particle per sample. */
    public int count = 32;
    public double speed = 0;
    /** Emission window, NOT the client-controlled lifetime of a vanilla particle. */
    public int lifetime = 100;
    public int delay = 0;
    public int intervalTicks = 2;
    public List<Animation> animations = new ArrayList<>();
}
