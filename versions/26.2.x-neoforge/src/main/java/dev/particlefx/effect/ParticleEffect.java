package dev.particlefx.effect;
import java.util.ArrayList;
import java.util.List;

/** Persisted definition, never a live task. Registering an effect makes a defensive copy. */
public final class ParticleEffect {
    public int schemaVersion = 1;
    public String name = "new_effect";
    public int durationTicks = 100;
    public PlaybackMode mode = PlaybackMode.ONCE;
    public double viewDistance = 32;
    public List<ParticleLayer> layers = new ArrayList<>(List.of(new ParticleLayer()));
}
