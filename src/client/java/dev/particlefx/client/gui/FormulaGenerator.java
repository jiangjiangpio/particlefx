package dev.particlefx.client.gui;

import dev.particlefx.math.Vec3;
import dev.particlefx.storage.EffectValidator;
import java.util.ArrayList;
import java.util.List;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.function.Function;

/** Client-only, bounded expression baking. No expression text reaches the server. */
public final class FormulaGenerator {
    public record Preset(String id, EditorI18n.Tier tier, String x, String y, String z, boolean surface) {}
    public static final List<Preset> PRESETS = List.of(
        new Preset("line",EditorI18n.Tier.ADVANCED,"4*t-2","0","0",false),
        new Preset("parabola",EditorI18n.Tier.ADVANCED,"4*t-2","(4*t-2)^2/2","0",false),
        new Preset("hyperbola",EditorI18n.Tier.ADVANCED,"2/cos(t)","0","2*tan(t)",false),
        new Preset("ellipse",EditorI18n.Tier.ADVANCED,"2*cos(2*pi*t)","0","sin(2*pi*t)",false),
        new Preset("spiral",EditorI18n.Tier.ADVANCED,"t*cos(6*pi*t)","0","t*sin(6*pi*t)",false),
        new Preset("rose",EditorI18n.Tier.ADVANCED,"2*cos(10*pi*t)*cos(2*pi*t)","0","2*cos(10*pi*t)*sin(2*pi*t)",false),
        new Preset("star",EditorI18n.Tier.ADVANCED,"(1.5+0.5*cos(10*pi*t))*cos(2*pi*t)","0","(1.5+0.5*cos(10*pi*t))*sin(2*pi*t)",false),
        new Preset("bezier",EditorI18n.Tier.ADVANCED,"3*(1-t)^2*t+3*(1-t)*t^2","3*(1-t)*t^2+t^3","0",false),
        new Preset("bspline",EditorI18n.Tier.ADVANCED,"2*t-1","(1-3*t+3*t^2-t^3)/6+4*(4-6*t^2+3*t^3)/6","0",false),
        new Preset("sphere",EditorI18n.Tier.ADVANCED,"2*sin(pi*u)*cos(t)","2*cos(pi*u)","2*sin(pi*u)*sin(t)",true),
        new Preset("torus",EditorI18n.Tier.EXPERT,"(2+0.6*cos(2*pi*u))*cos(t)","0.6*sin(2*pi*u)","(2+0.6*cos(2*pi*u))*sin(t)",true),
        new Preset("cylinder",EditorI18n.Tier.EXPERT,"cos(t)","4*u-2","sin(t)",true),
        new Preset("cone",EditorI18n.Tier.EXPERT,"(1-u)*cos(t)","2*u","(1-u)*sin(t)",true),
        new Preset("plane",EditorI18n.Tier.EXPERT,"4*t/(2*pi)-2","0","4*u-2",true),
        new Preset("mobius",EditorI18n.Tier.EXPERT,"(2+(u-0.5)*cos(t/2))*cos(t)","(u-0.5)*sin(t/2)","(2+(u-0.5)*cos(t/2))*sin(t)",true),
        new Preset("klein",EditorI18n.Tier.EXPERT,"(2+cos(2*pi*u))*cos(t)","sin(2*pi*u)*cos(t/2)","(2+cos(2*pi*u))*sin(t)+sin(2*pi*u)*sin(t/2)",true),
        new Preset("implicit",EditorI18n.Tier.EXPERT,"4*t/(2*pi)-2","sqrt(max(0,4-(4*t/(2*pi)-2)^2-(4*u-2)^2))","4*u-2",true),
        new Preset("fourier",EditorI18n.Tier.EXPERT,"2*t-1","sin(2*pi*t)+sin(6*pi*t)/3+sin(10*pi*t)/5","0",false),
        new Preset("noise",EditorI18n.Tier.EXPERT,"4*t-2","noise(t*8,0,0)","0",false),
        new Preset("fractal_noise",EditorI18n.Tier.EXPERT,"4*t-2","noise(t*4,0,0)+noise(t*8,0,0)/2+noise(t*16,0,0)/4","0",false),
        new Preset("wave",EditorI18n.Tier.EXPERT,"4*t-2","sin(4*pi*t)","0",false),
        new Preset("lerp",EditorI18n.Tier.EXPERT,"lerp(-2,2,t)","smoothstep(t)","0",false),
        new Preset("ease",EditorI18n.Tier.EXPERT,"4*t-2","easeinout(t)","0",false),
        new Preset("attractor",EditorI18n.Tier.EXPERT,"2*cos(8*pi*t)/(1+t)","2*t-1","2*sin(8*pi*t)/(1+t)",false),
        new Preset("repulsion",EditorI18n.Tier.EXPERT,"2*t-1","1/(0.2+abs(2*t-1))","0",false),
        new Preset("gravity",EditorI18n.Tier.EXPERT,"4*t-2","2-4*t^2","0",false),
        new Preset("magnetic",EditorI18n.Tier.EXPERT,"2*cos(8*pi*t)","2*t-1","2*sin(8*pi*t)",false),
        new Preset("curl_noise",EditorI18n.Tier.EXPERT,"4*t-2","noise(t*8,0,0)-noise(0,t*8,0)","noise(0,0,t*8)",false)
    );
    private static final Function MIN = new Function("min",2) { public double apply(double... x){return Math.min(x[0],x[1]);} };
    private static final Function MAX = new Function("max",2) { public double apply(double... x){return Math.max(x[0],x[1]);} };
    private static final Function POW = new Function("pow",2) { public double apply(double... x){return Math.pow(x[0],x[1]);} };
    private static final Function CLAMP = new Function("clamp",3) { public double apply(double... x){return Math.max(x[1],Math.min(x[2],x[0]));} };
    private static final Function LERP = new Function("lerp",3) { public double apply(double... x){return x[0]+(x[1]-x[0])*x[2];} };
    private static final Function SMOOTHSTEP = new Function("smoothstep",1) { public double apply(double... x){double t=Math.max(0,Math.min(1,x[0]));return t*t*(3-2*t);} };
    private static final Function EASE_IN = new Function("easein",1) { public double apply(double... x){double t=Math.max(0,Math.min(1,x[0]));return t*t;} };
    private static final Function EASE_OUT = new Function("easeout",1) { public double apply(double... x){double t=Math.max(0,Math.min(1,x[0]));return 1-(1-t)*(1-t);} };
    private static final Function EASE_IN_OUT = new Function("easeinout",1) { public double apply(double... x){double t=Math.max(0,Math.min(1,x[0]));return t*t*(3-2*t);} };
    private static final Function NOISE = new Function("noise",3) { public double apply(double... v){return valueNoise(v[0],v[1],v[2]);} };
    private static double valueNoise(double x,double y,double z) {
        long ix=(long)Math.floor(x),iy=(long)Math.floor(y),iz=(long)Math.floor(z);
        double fx=x-ix,fy=y-iy,fz=z-iz;
        fx=fade(fx);fy=fade(fy);fz=fade(fz);
        double x00=mix(hash(ix,iy,iz),hash(ix+1,iy,iz),fx);
        double x10=mix(hash(ix,iy+1,iz),hash(ix+1,iy+1,iz),fx);
        double x01=mix(hash(ix,iy,iz+1),hash(ix+1,iy,iz+1),fx);
        double x11=mix(hash(ix,iy+1,iz+1),hash(ix+1,iy+1,iz+1),fx);
        return mix(mix(x00,x10,fy),mix(x01,x11,fy),fz);
    }
    private static double hash(long x,long y,long z) {
        long n=x*0x9E3779B97F4A7C15L^Long.rotateLeft(y*0xC2B2AE3D27D4EB4FL,21)^Long.rotateLeft(z*0x165667B19E3779F9L,42);
        n=(n^(n>>>30))*0xBF58476D1CE4E5B9L;n=(n^(n>>>27))*0x94D049BB133111EBL;n^=n>>>31;
        return (n>>>11)*0x1.0p-52-1;
    }
    private static double fade(double t){return t*t*t*(t*(t*6-15)+10);}
    private static double mix(double a,double b,double t){return a+(b-a)*t;}
    private static Expression expression(String text) {
        EffectValidator.require(text!=null&&text.length()<=160&&!text.isBlank(),"Expression must have 1-160 characters");
        return new ExpressionBuilder(text).variables("t","u").functions(MIN,MAX,POW,CLAMP,LERP,SMOOTHSTEP,EASE_IN,EASE_OUT,EASE_IN_OUT,NOISE).build();
    }
    public static List<Vec3> generate(String x,String y,String z,int count,boolean surface) {
        EffectValidator.range(count,1,EffectValidator.MAX_CUSTOM_POINTS,"point count");
        Expression ex=expression(x),ey=expression(y),ez=expression(z);
        List<Vec3> result=new ArrayList<>(count);
        int cols=surface?(int)Math.ceil(Math.sqrt(count)):count;
        int rows=surface?(int)Math.ceil((double)count/cols):1;
        for(int i=0;i<count;i++){
            double t=surface?2*Math.PI*(i%cols)/Math.max(1,cols-1):(double)i/Math.max(1,count-1);
            double u=surface?(double)(i/cols)/Math.max(1,rows-1):0;
            ex.setVariable("t",t);ey.setVariable("t",t);ez.setVariable("t",t);
            ex.setVariable("u",u);ey.setVariable("u",u);ez.setVariable("u",u);
            Vec3 p=new Vec3(ex.evaluate(),ey.evaluate(),ez.evaluate());
            for(double v:new double[]{p.x(),p.y(),p.z()})EffectValidator.range(v,-128,128,"generated coordinate");
            result.add(new Vec3(round(p.x()),round(p.y()),round(p.z())));
        }
        return result;
    }
    private static double round(double value){return Math.round(value*1000)/1000.0;}
    private FormulaGenerator() {}
}
