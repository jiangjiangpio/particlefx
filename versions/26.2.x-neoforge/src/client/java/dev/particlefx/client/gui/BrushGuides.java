package dev.particlefx.client.gui;

import dev.particlefx.math.Vec3;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;

/** Bounded geometry and perspective clipping for the GUI-only world reference overlay. */
public final class BrushGuides {
    public record Segment(Vec3 start,Vec3 end,boolean major) {}
    public record Point(double x,double y) {}
    public record Line(Point start,Point end) {}
    public static final int RANGE=16;
    private static final double NEAR=.05;
    private static final List<Segment> GROUND=grid(Vec3.ZERO,new Vec3(1,0,0),new Vec3(0,0,1));
    private final Vec3 camera,right,up,forward;
    private final double width,height,tan,aspect;
    private final double left,top,bottom,edge;

    public BrushGuides(Freehand3DModel model,double width,double height,double aspect,double edge,double top,double bottom) {
        this.camera=model.camera();this.right=model.right();this.up=model.up();this.forward=model.forward();
        this.width=width;this.height=height;this.aspect=aspect;this.tan=Math.tan(Math.toRadians(model.fov()/2));
        this.left=0;this.edge=edge;this.top=top;this.bottom=bottom;
    }
    public static List<Segment> ground(){return GROUND;}
    public static List<Segment> plane(Freehand3DModel model){return grid(model.planeCenter(),model.right(),model.up());}
    public static List<Segment> boardGrid(BoardTransform board) {
        List<Segment> lines=new ArrayList<>();
        for(int i=(int)Math.ceil(-board.width()/2);i<=board.width()/2;i++)
            lines.add(new Segment(board.world(i,-board.height()/2),board.world(i,board.height()/2),i%4==0));
        for(int i=(int)Math.ceil(-board.height()/2);i<=board.height()/2;i++)
            lines.add(new Segment(board.world(-board.width()/2,i),board.world(board.width()/2,i),i%4==0));
        return lines;
    }
    private static List<Segment> grid(Vec3 center,Vec3 a,Vec3 b) {
        List<Segment> lines=new ArrayList<>(66);
        for(int i=-RANGE;i<=RANGE;i++) {
            Vec3 ca=center.add(a.multiply(i)),cb=center.add(b.multiply(i));
            lines.add(new Segment(ca.add(b.multiply(-RANGE)),ca.add(b.multiply(RANGE)),i%4==0));
            lines.add(new Segment(cb.add(a.multiply(-RANGE)),cb.add(a.multiply(RANGE)),i%4==0));
        }
        return List.copyOf(lines);
    }
    private static double dot(Vec3 a,Vec3 b){return a.x()*b.x()+a.y()*b.y()+a.z()*b.z();}
    private Vec3 view(Vec3 p) {
        Vec3 relative=p.add(camera.multiply(-1));
        return new Vec3(dot(relative,right),dot(relative,up),dot(relative,forward));
    }
    private Point screen(Vec3 p) {
        return new Point(width*.5*(1+p.x()/(p.z()*tan*aspect)),height*.5*(1-p.y()/(p.z()*tan)));
    }
    public Point point(Vec3 p) {
        Vec3 v=view(p);
        if(!v.finite()||v.z()<NEAR)return null;
        Point result=screen(v);
        return code(result)==0?result:null;
    }
    public List<Point> polygon(List<Vec3> vertices) {
        List<Vec3> clipped=new ArrayList<>();
        if(vertices.size()<3)return List.of();
        Vec3 previous=view(vertices.getLast());
        for(Vec3 vertex:vertices) {
            Vec3 next=view(vertex);
            if(!previous.finite()||!next.finite())return List.of();
            boolean pin=previous.z()>=NEAR,nin=next.z()>=NEAR;
            if(pin!=nin)clipped.add(interpolate(previous,next,(NEAR-previous.z())/(next.z()-previous.z())));
            if(nin)clipped.add(next);
            previous=next;
        }
        List<Point> points=new ArrayList<>();
        for(Vec3 vertex:clipped)points.add(screen(vertex));
        points=clip(points,p->p.x()-left);
        points=clip(points,p->edge-p.x());
        points=clip(points,p->p.y()-top);
        points=clip(points,p->bottom-p.y());
        return points.stream().map(p->new Point(Math.max(left,Math.min(edge,p.x())),
            Math.max(top,Math.min(bottom,p.y())))).toList();
    }
    public record Span(int y,int start,int end) {}
    public List<Span> spans(List<Point> polygon) {
        if(polygon.size()<3)return List.of();
        List<Span> result=new ArrayList<>();
        int first=Math.max((int)Math.ceil(top),(int)Math.floor(polygon.stream().mapToDouble(Point::y).min().orElse(top)));
        int last=Math.min((int)Math.floor(bottom)-1,(int)Math.ceil(polygon.stream().mapToDouble(Point::y).max().orElse(bottom)));
        for(int y=first;y<=last;y++) {
            double scan=y+.5,min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY;
            Point previous=polygon.getLast();
            for(Point next:polygon) {
                if((previous.y()<=scan&&next.y()>scan)||(next.y()<=scan&&previous.y()>scan)) {
                    double x=previous.x()+(next.x()-previous.x())*(scan-previous.y())/(next.y()-previous.y());
                    min=Math.min(min,x);max=Math.max(max,x);
                }
                previous=next;
            }
            if(Double.isFinite(min)&&Double.isFinite(max)&&max>min)
                result.add(new Span(y,Math.max((int)left,(int)Math.ceil(min)),Math.min((int)edge,(int)Math.ceil(max))));
        }
        return result;
    }
    private static List<Point> clip(List<Point> input,ToDoubleFunction<Point> distance) {
        if(input.isEmpty())return input;
        List<Point> result=new ArrayList<>();
        Point previous=input.getLast();double pd=distance.applyAsDouble(previous);
        for(Point next:input) {
            double nd=distance.applyAsDouble(next);
            if((pd>=0)!=(nd>=0)) {
                double t=pd/(pd-nd);
                result.add(new Point(previous.x()+(next.x()-previous.x())*t,previous.y()+(next.y()-previous.y())*t));
            }
            if(nd>=0)result.add(next);
            previous=next;pd=nd;
        }
        return result;
    }
    public Line line(Vec3 start,Vec3 end) {
        Vec3 a=view(start),b=view(end);
        if(!a.finite()||!b.finite()||a.z()<NEAR&&b.z()<NEAR)return null;
        if(a.z()<NEAR)a=interpolate(a,b,(NEAR-a.z())/(b.z()-a.z()));
        if(b.z()<NEAR)b=interpolate(b,a,(NEAR-b.z())/(a.z()-b.z()));
        Point p=screen(a),q=screen(b);
        // Clip in screen space before rounding or allocating a GUI quad.
        for(int i=0;i<8;i++) {
            int pc=code(p),qc=code(q);
            if((pc|qc)==0)return new Line(p,q);
            if((pc&qc)!=0)return null;
            int c=pc!=0?pc:qc;
            double x,y;
            if((c&8)!=0){y=bottom;x=p.x()+(q.x()-p.x())*(y-p.y())/(q.y()-p.y());}
            else if((c&4)!=0){y=top;x=p.x()+(q.x()-p.x())*(y-p.y())/(q.y()-p.y());}
            else if((c&2)!=0){x=edge;y=p.y()+(q.y()-p.y())*(x-p.x())/(q.x()-p.x());}
            else{x=left;y=p.y()+(q.y()-p.y())*(x-p.x())/(q.x()-p.x());}
            Point clipped=new Point(x,y);
            if(!Double.isFinite(x)||!Double.isFinite(y))return null;
            if(c==pc)p=clipped;else q=clipped;
        }
        return null;
    }
    private int code(Point p) {
        return (p.x()<left?1:0)|(p.x()>edge?2:0)|(p.y()<top?4:0)|(p.y()>bottom?8:0);
    }
    private static Vec3 interpolate(Vec3 a,Vec3 b,double t){return a.multiply(1-t).add(b.multiply(t));}
}


