package dev.particlefx.animation;
import dev.particlefx.effect.PlaybackMode;

/** Scalar channels; axis is x/y/z/all, speed optionally replaces to-from per tick. */
public final class Animation {
    public String type = "rotation";
    public String axis = "y";
    public double from = 0;
    public double to = 360;
    public Double speed;
    public int durationTicks = 100;
    public PlaybackMode mode = PlaybackMode.LOOP;
    public double value(long tick) {
        double end = speed == null ? to : from + speed * durationTicks;
        return from + (end - from) * mode.phase(tick,durationTicks);
    }
}
