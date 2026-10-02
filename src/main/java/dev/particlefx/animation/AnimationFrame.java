package dev.particlefx.animation;
import dev.particlefx.effect.ParticleLayer;
import dev.particlefx.math.Vec3;

/** One reusable transform per active layer. No allocation for individual points. */
public final class AnimationFrame {
    public double density, lifetime;
    private double tx,ty,tz,sx,sy,sz,cx,cy,cz,rx,ry,rz;
    public void evaluate(ParticleLayer l,long tick) {
        tx=l.offset.x();ty=l.offset.y();tz=l.offset.z();
        sx=l.scale.x();sy=l.scale.y();sz=l.scale.z();
        double x=l.rotation.x(),y=l.rotation.y(),z=l.rotation.z();
        density=l.density;lifetime=l.lifetime;
        for(Animation a:l.animations) {
            double v=a.value(tick); boolean ax=a.axis.equalsIgnoreCase("x")||a.axis.equalsIgnoreCase("all"),
                ay=a.axis.equalsIgnoreCase("y")||a.axis.equalsIgnoreCase("all"),az=a.axis.equalsIgnoreCase("z")||a.axis.equalsIgnoreCase("all");
            switch(a.type.toLowerCase(java.util.Locale.ROOT)) {
                case "rotation" -> {if(ax)x+=v;if(ay)y+=v;if(az)z+=v;}
                case "translation" -> {if(ax)tx+=v;if(ay)ty+=v;if(az)tz+=v;}
                case "scale" -> {if(ax)sx*=v;if(ay)sy*=v;if(az)sz*=v;}
                case "density" -> density*=v;
                case "lifetime" -> lifetime*=v;
                default -> throw new IllegalArgumentException("Unknown animation: "+a.type);
            }
        }
        x=Math.toRadians(x);y=Math.toRadians(y);z=Math.toRadians(z);
        cx=Math.cos(x);rx=Math.sin(x);cy=Math.cos(y);ry=Math.sin(y);cz=Math.cos(z);rz=Math.sin(z);
    }
    public void transform(Vec3 p,double[] out) {
        double x=p.x()*sx,y=p.y()*sy,z=p.z()*sz;
        double y1=y*cx-z*rx,z1=y*rx+z*cx,x2=x*cy+z1*ry,z2=-x*ry+z1*cy;
        out[0]=x2*cz-y1*rz+tx;out[1]=x2*rz+y1*cz+ty;out[2]=z2+tz;
    }
}
