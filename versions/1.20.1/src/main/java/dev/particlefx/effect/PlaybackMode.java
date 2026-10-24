package dev.particlefx.effect;
import com.google.gson.annotations.SerializedName;
public enum PlaybackMode {
    @SerializedName("once") ONCE,
    @SerializedName("loop") LOOP,
    @SerializedName("ping-pong") PING_PONG;
    public double phase(long tick, int duration) {
        if (duration < 1) throw new IllegalArgumentException("duration must be positive");
        long t = Math.max(0, tick);
        return switch (this) {
            case ONCE -> Math.min(1.0, (double) t / duration);
            case LOOP -> (double) (t % duration) / duration;
            case PING_PONG -> 1.0 - Math.abs((double) (t % (2L * duration)) / duration - 1.0);
        };
    }
}
