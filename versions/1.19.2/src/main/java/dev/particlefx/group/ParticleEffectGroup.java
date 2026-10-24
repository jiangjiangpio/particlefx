package dev.particlefx.group;

import java.util.ArrayList;
import java.util.List;

/** A one-shot timeline that starts existing effects at relative delays. */
public final class ParticleEffectGroup {
    public int schemaVersion = 1;
    public String name = "new_group";
    public int durationTicks = 100;
    public List<ParticleGroupStep> steps = new ArrayList<>();
}
