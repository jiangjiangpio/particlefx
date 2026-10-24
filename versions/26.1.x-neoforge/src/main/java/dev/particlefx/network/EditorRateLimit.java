package dev.particlefx.network;

/** Allows list/config bursts while bounding expensive editor operations. */
public final class EditorRateLimit {
    private long window = Long.MIN_VALUE;
    private int used;
    public boolean allow(long tick) {
        if (window == Long.MIN_VALUE || tick < window || tick - window >= 20) { window = tick; used = 0; }
        return ++used <= 8;
    }
}
