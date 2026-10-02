package dev.particlefx.server;
import dev.particlefx.effect.EffectState;
import dev.particlefx.effect.PlaybackMode;

/** Tested independently of Minecraft's server runtime. */
public final class EffectClock {
    private long age;
    private EffectState state=EffectState.PLAYING;
    public long age() {return age;}
    public EffectState state() {return state;}
    public void pause() {if(state==EffectState.PLAYING)state=EffectState.PAUSED;}
    public void resume() {if(state==EffectState.PAUSED)state=EffectState.PLAYING;}
    public void stop() {state=EffectState.STOPPED;}
    public long localTick(PlaybackMode mode,int duration) {
        if(mode==PlaybackMode.ONCE)return Math.min(age,duration-1L);
        if(mode==PlaybackMode.LOOP)return age%duration;
        if(duration==1)return 0;
        long phase=age%(2L*(duration-1));return phase<duration?phase:2L*(duration-1)-phase;
    }
    public void advance(PlaybackMode mode,int duration) {
        if(state!=EffectState.PLAYING)return;
        age++;if(mode==PlaybackMode.ONCE&&age>=duration)stop();
    }
}
