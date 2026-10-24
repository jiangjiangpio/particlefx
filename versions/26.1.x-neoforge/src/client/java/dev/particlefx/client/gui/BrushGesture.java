package dev.particlefx.client.gui;

/** A gesture starts only on a fresh press; polling can stop it but never restart it. */
public final class BrushGesture {
    public record Delta(double x,double y) {}
    private int button=-1;
    private double x,y;
    private boolean leftHeld,rightHeld;

    public int button(){return button;}
    public void stop(){button=-1;}
    public void resetInput(){stop();leftHeld=false;rightHeld=false;}
    public boolean eventPress(int button,double x,double y) {
        boolean repeated=button==0?leftHeld:button==1&&rightHeld;
        if(button==0)leftHeld=true;
        if(button==1)rightHeld=true;
        if(repeated||leftHeld&&rightHeld){stop();return false;}
        begin(button,x,y);
        return this.button!=-1;
    }
    public void eventRelease(int button) {
        if(button==0)leftHeld=false;
        if(button==1)rightHeld=false;
        stop();
    }
    public boolean checkEvents(boolean focused,boolean inside,boolean inputFocused) {
        if(!focused)resetInput();
        return check(focused,inside,inputFocused,leftHeld,rightHeld);
    }
    public void begin(int button,double x,double y) {
        stop();
        if((button==0||button==1)&&Double.isFinite(x)&&Double.isFinite(y)) {
            this.button=button;this.x=x;this.y=y;
        }
    }
    public boolean check(boolean focused,boolean inside,boolean inputFocused,boolean left,boolean right) {
        if(!focused||!inside||inputFocused||left&&right||
            button==0&&!left||button==1&&!right)stop();
        return button!=-1;
    }
    public Delta drag(int button,double x,double y,double width,double height) {
        if(this.button!=button)return null;
        double dx=x-this.x,dy=y-this.y;
        // Reject discontinuous cursor jumps instead of applying stale/native accumulated deltas.
        if(!Double.isFinite(dx)||!Double.isFinite(dy)||Math.abs(dx)>width/2||Math.abs(dy)>height/2) {
            stop();return null;
        }
        this.x=x;this.y=y;
        return new Delta(dx,dy);
    }
}

