package dev.particlefx.client.gui;

import dev.particlefx.animation.AnimationFrame;
import dev.particlefx.effect.ParticleLayer;
import dev.particlefx.math.Vec3;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Pure coordinate/stroke model. Camera state never belongs to the player entity. */
public final class Freehand3DModel {
    public enum Result { OK, LIMIT, OUTSIDE }
    public enum Tool { MOVE, OBSERVE, BRUSH, TRANSLATE, ROTATE }
    public record Projected(double x, double y, double depth) {}
    private static final double SPACING = .15, CELL = .1, MIN_DISTANCE_SQUARED = .0064;
    private final List<Vec3> points;
    private final List<Integer> strokes = new ArrayList<>();
    private final Map<Long,List<Vec3>> cells = new HashMap<>();
    private final int limit;
    private final ParticleLayer layer;
    private final AnimationFrame frame = new AnimationFrame();
    private final double[] transformed = new double[3];
    private Vec3 camera, initialCamera, previous;
    private float yaw, pitch, initialYaw, initialPitch;
    private double distance = 4;
    private double baseFov = 70, zoom = 1;
    private Tool tool = Tool.MOVE;
    private boolean drawing, orbitStarted;

    public Freehand3DModel(List<Vec3> existing,int limit,ParticleLayer layer,Vec3 camera,float yaw,float pitch) {
        if(layer.scale.x()==0||layer.scale.y()==0||layer.scale.z()==0)throw new IllegalArgumentException("Zero brush scale");
        this.points=new ArrayList<>(existing);this.limit=limit;this.layer=layer;
        this.initialCamera=camera;this.initialYaw=yaw;this.initialPitch=pitch;
        resetCamera();points.forEach(this::index);
        ParticleLayer base=new ParticleLayer();base.offset=layer.offset;base.rotation=layer.rotation;base.scale=layer.scale;
        frame.evaluate(base,0);
    }
    public List<Vec3> points(){return List.copyOf(points);}
    public int size(){return points.size();}
    public Vec3 point(int index){return points.get(index);}
    public Vec3 camera(){return camera;}
    public float yaw(){return yaw;}
    public float pitch(){return pitch;}
    public double distance(){return distance;}
    public boolean drawing(){return drawing;}
    public Tool tool(){return tool;}
    public double zoom(){return zoom;}
    public double fov(){return Math.max(10,Math.min(120,Math.toDegrees(2*Math.atan(Math.tan(Math.toRadians(baseFov/2))/zoom))));}
    public void setTool(Tool value){endStroke();tool=value == null ? Tool.OBSERVE : value;}
    public void setBaseFov(double value){if(Double.isFinite(value))baseFov=Math.max(30,Math.min(110,value));}
    public void setDistance(double value) {
        if(!Double.isFinite(value)||value<.5||value>64)throw new IllegalArgumentException("Invalid drawing depth");
        endStroke();distance=value;
    }
    public void resetCamera(){endStroke();camera=initialCamera;yaw=initialYaw;pitch=initialPitch;zoom=1;orbitStarted=false;}
    public void faceOrigin() {
        endStroke();
        if(dot(camera,camera)<.25)camera=forward().multiply(-8);
        aimAt(Vec3.ZERO);
        orbitStarted=true;
    }
    public void look(double dx,double dy) {
        if(!cameraTool()||!Double.isFinite(dx)||!Double.isFinite(dy))return;
        yaw=(float)((yaw+dx*.35)%360);pitch=(float)Math.max(-89.9,Math.min(89.9,pitch+dy*.35));
    }
    public void adjustDistance(double delta){distance=Math.max(.5,Math.min(64,distance+delta*.5));previous=null;}
    public void zoom(double delta) {
        if(!Double.isFinite(delta))return;
        endStroke();
        zoom=Math.max(.5,Math.min(8,zoom*Math.exp(Math.max(-10,Math.min(10,delta))*.12)));
    }
    public void orbit(double dx,double dy) {
        orbitAround(Vec3.ZERO,dx,dy);
    }
    public void orbitAround(Vec3 target,double dx,double dy) {
        if(!cameraTool()||target==null||!target.finite()||!Double.isFinite(dx)||!Double.isFinite(dy))return;
        if(dx==0&&dy==0)return;
        Vec3 relative=camera.add(target.multiply(-1));
        double radius=Math.sqrt(relative.x()*relative.x()+relative.y()*relative.y()+relative.z()*relative.z());
        if(!orbitStarted&&radius<8) {
            camera=target.add(forward().multiply(-8));
            relative=camera.add(target.multiply(-1));
            radius=8;
        }
        orbitStarted=true;
        radius=Math.max(.5,Math.min(192,radius));
        double orbitYaw=Math.atan2(-relative.x(),relative.z())+Math.toRadians(dx*.35);
        double orbitPitch=Math.atan2(relative.y(),Math.hypot(relative.x(),relative.z()))+Math.toRadians(dy*.35);
        orbitPitch=Math.max(Math.toRadians(-89.9),Math.min(Math.toRadians(89.9),orbitPitch));
        double cos=Math.cos(orbitPitch);
        camera=target.add(new Vec3(-Math.sin(orbitYaw)*cos*radius,Math.sin(orbitPitch)*radius,Math.cos(orbitYaw)*cos*radius));
        aimAt(target);
        previous=null;
    }
    private void aimAt(Vec3 target) {
        Vec3 direction=target.add(camera.multiply(-1));
        double length=Math.sqrt(direction.x()*direction.x()+direction.y()*direction.y()+direction.z()*direction.z());
        if(length<1e-8)return;
        direction=direction.multiply(1/length);
        pitch=(float)Math.toDegrees(Math.asin(-direction.y()));
        yaw=(float)Math.toDegrees(Math.atan2(-direction.x(),direction.z()));
    }
    public Vec3 forward() {
        double y=Math.toRadians(yaw),p=Math.toRadians(pitch),c=Math.cos(p);
        return new Vec3(-Math.sin(y)*c,-Math.sin(p),Math.cos(y)*c);
    }
    public Vec3 right(){double y=Math.toRadians(yaw);return new Vec3(-Math.cos(y),0,-Math.sin(y));}
    public Vec3 up() {
        Vec3 r=right(),f=forward();
        return new Vec3(-r.z()*f.y(),r.z()*f.x()-r.x()*f.z(),r.x()*f.y());
    }
    public Vec3 planeCenter() {
        Vec3 f=forward(),c=cursor();
        return f.multiply(c.x()*f.x()+c.y()*f.y()+c.z()*f.z());
    }
    public void move(double forward,double right,double up,boolean fast) {
        move(forward,right,up,fast,.05);
    }
    public void move(double forward,double right,double up,boolean fast,double seconds) {
        if(!cameraTool()||!Double.isFinite(seconds)||seconds<=0)return;
        Vec3 f=forward();double y=Math.toRadians(yaw);
        // Minecraft yaw zero faces +Z; screen-right is -X, not +X.
        Vec3 delta=new Vec3(f.x()*forward-Math.cos(y)*right,f.y()*forward+up,f.z()*forward-Math.sin(y)*right);
        double length=Math.sqrt(delta.x()*delta.x()+delta.y()*delta.y()+delta.z()*delta.z());
        if(length==0)return;
        Vec3 next=camera.add(delta.multiply((fast?13:4)*Math.min(.1,seconds)/Math.max(1,length)));
        camera=new Vec3(clamp(next.x()),clamp(next.y()),clamp(next.z()));
    }
    private static double clamp(double v){return Math.max(-192,Math.min(192,v));}
    public Vec3 cursor(){return camera.add(forward().multiply(distance));}
    public Vec3 rayFromScreen(double x,double y,double width,double height,double aspect) {
        if(!Double.isFinite(x+y+width+height+aspect)||width<=0||height<=0||aspect<=0)
            throw new IllegalArgumentException("Invalid drawing viewport");
        double horizontal=(2*x/width-1)*aspect,vertical=1-2*y/height;
        double tan=Math.tan(Math.toRadians(fov()/2));
        Vec3 direction=forward().add(right().multiply(horizontal*tan)).add(up().multiply(vertical*tan));
        double length=Math.sqrt(direction.x()*direction.x()+direction.y()*direction.y()+direction.z()*direction.z());
        return length<1e-9?forward():direction.multiply(1/length);
    }
    public Projected project(Vec3 point,double width,double height,double aspect) {
        if(point==null||!point.finite()||width<=0||height<=0||aspect<=0)return null;
        Vec3 relative=point.add(camera.multiply(-1));
        Vec3 right=right(),up=up(),forward=forward();
        double depth=dot(relative,forward);
        if(!Double.isFinite(depth)||depth<.05)return null;
        double horizontal=dot(relative,right),vertical=dot(relative,up);
        double tan=Math.tan(Math.toRadians(fov()/2));
        return new Projected(width*.5*(1+horizontal/(depth*tan*aspect)),
            height*.5*(1-vertical/(depth*tan)),depth);
    }
    public Vec3 screenPoint(double x,double y,double width,double height,double aspect) {
        if(!Double.isFinite(x+y+width+height+aspect)||width<=0||height<=0||aspect<=0)
            throw new IllegalArgumentException("Invalid drawing viewport");
        double horizontal=(2*x/width-1)*aspect,vertical=1-2*y/height;
        double halfHeight=distance*Math.tan(Math.toRadians(fov()/2));
        Vec3 right=right(),up=up();
        return cursor().add(right.multiply(horizontal*halfHeight)).add(up.multiply(vertical*halfHeight));
    }
    public Vec3 displayPoint(int index){frame.transform(points.get(index),transformed);return new Vec3(transformed[0],transformed[1],transformed[2]);}
    public void beginStroke(){if(!drawing){drawing=true;strokes.add(points.size());previous=null;}}
    public void endStroke(){
        if(drawing&&!strokes.isEmpty()&&strokes.getLast()==points.size())strokes.removeLast();
        drawing=false;previous=null;
    }
    public Result sample() {
        return sampleAt(cursor());
    }
    public Result sampleScreen(double x,double y,double width,double height,double aspect) {
        if(tool!=Tool.BRUSH||!drawing)return Result.OK;
        return sampleAt(screenPoint(x,y,width,height,aspect));
    }
    public Result sampleWorld(Vec3 position) {
        if(tool!=Tool.BRUSH||!drawing||position==null)return Result.OK;
        return sampleAt(position);
    }
    public void breakStrokePath(){previous=null;}
    private Result sampleAt(Vec3 position) {
        Vec3 target=toLocal(position);
        if(!target.finite()||Math.abs(target.x())>128||Math.abs(target.y())>128||Math.abs(target.z())>128){previous=null;return Result.OUTSIDE;}
        if(previous==null){previous=target;return add(target);}
        double dx=target.x()-previous.x(),dy=target.y()-previous.y(),dz=target.z()-previous.z();
        int steps=Math.min(16,Math.max(1,(int)Math.ceil(Math.sqrt(dx*dx+dy*dy+dz*dz)/SPACING)));
        for(int i=1;i<=steps;i++) {
            double t=(double)i/steps;
            Result result=add(new Vec3(previous.x()+dx*t,previous.y()+dy*t,previous.z()+dz*t));
            if(result!=Result.OK){previous=target;return result;}
        }
        previous=target;return Result.OK;
    }
    public void undo() {
        endStroke();
        if(points.isEmpty())return;
        int start=strokes.isEmpty()?points.size()-1:strokes.removeLast();
        points.subList(start,points.size()).clear();reindex();
    }
    public void clear(){endStroke();points.clear();strokes.clear();cells.clear();}
    private Result add(Vec3 p) {
        if(points.size()>=limit)return Result.LIMIT;
        p=new Vec3(Math.round(p.x()*1000)/1000.0,Math.round(p.y()*1000)/1000.0,Math.round(p.z()*1000)/1000.0);
        int x=cell(p.x()),y=cell(p.y()),z=cell(p.z());
        for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++)for(int dz=-1;dz<=1;dz++) {
            var nearby=cells.get(key(x+dx,y+dy,z+dz));
            if(nearby!=null)for(Vec3 q:nearby) {
                double a=p.x()-q.x(),b=p.y()-q.y(),c=p.z()-q.z();
                if(a*a+b*b+c*c<MIN_DISTANCE_SQUARED)return Result.OK;
            }
        }
        points.add(p);index(p);return Result.OK;
    }
    private void reindex(){cells.clear();points.forEach(this::index);}
    private void index(Vec3 p){cells.computeIfAbsent(key(cell(p.x()),cell(p.y()),cell(p.z())),k->new ArrayList<>()).add(p);}
    private static int cell(double p){return (int)Math.floor(p/CELL);}
    private static long key(int x,int y,int z){return ((long)(x+4096)<<28)|((long)(y+4096)<<14)|(z+4096);}
    public Vec3 toLocal(Vec3 point) {
        double x=point.x()-layer.offset.x(),y=point.y()-layer.offset.y(),z=point.z()-layer.offset.z();
        double rz=Math.toRadians(layer.rotation.z()),ry=Math.toRadians(layer.rotation.y()),rx=Math.toRadians(layer.rotation.x());
        double x1=x*Math.cos(rz)+y*Math.sin(rz),y1=-x*Math.sin(rz)+y*Math.cos(rz);
        double x2=x1*Math.cos(ry)-z*Math.sin(ry),z2=x1*Math.sin(ry)+z*Math.cos(ry);
        return new Vec3(x2/layer.scale.x(),(y1*Math.cos(rx)+z2*Math.sin(rx))/layer.scale.y(),
            (-y1*Math.sin(rx)+z2*Math.cos(rx))/layer.scale.z());
    }
    private boolean cameraTool(){return tool==Tool.MOVE||tool==Tool.OBSERVE;}
    private static double dot(Vec3 a,Vec3 b){return a.x()*b.x()+a.y()*b.y()+a.z()*b.z();}
}


