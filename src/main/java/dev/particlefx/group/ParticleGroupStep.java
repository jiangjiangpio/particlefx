package dev.particlefx.group;

import dev.particlefx.math.Vec3;

/** One effect launch in a group timeline. */
public final class ParticleGroupStep {
    public String effect = "new_effect";
    public int delayTicks = 0;
    public Vec3 offset = Vec3.ZERO;
}
