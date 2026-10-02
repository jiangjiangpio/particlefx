package dev.particlefx.shape;

import dev.particlefx.math.Vec3;

public final class CustomShape implements ParticleShape {
    @Override
    public Vec3 sample(int index, int total, ShapeSettings settings) {
        int size = settings.vertices.size();
        return settings.vertices.get((int) Math.min(size - 1, (long) index * size / total));
    }
}
