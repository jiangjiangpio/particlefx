package dev.particlefx.client.gui;

import dev.particlefx.math.Vec3;
import java.util.ArrayList;
import java.util.List;

/** CPU-only projection and hit testing for the board translation/rotation gizmos. */
public final class BoardGizmo {
    public enum Kind { TRANSLATE, ROTATE }
    public record Projected(double x, double y) {}
    public record Segment(Projected start, Projected end, BoardTransform.Axis axis) {}
    public record Ring(List<Segment> segments, BoardTransform.Axis axis) {}
    public record Hit(BoardTransform.Axis axis, double distance) {}
    private static final int RING_STEPS = 48;
    private static final double HIT_RADIUS = 10;

    private BoardGizmo() {}
    public static List<Segment> axes(Freehand3DModel model, BoardTransform board, double width, double height, double aspect) {
        return axes(model,board,width,height,aspect,new BrushGuides(model,width,height,aspect,width,0,height));
    }
    public static List<Segment> axes(Freehand3DModel model, BoardTransform board, double width, double height, double aspect,
                                    BrushGuides guides) {
        List<Segment> result = new ArrayList<>(3);
        Projected center = project(model, board.center(), width, height, aspect);
        if (center == null) return result;
        double length=scale(model,board,width,height,aspect);
        for (BoardTransform.Axis axis : BoardTransform.Axis.values()) {
            Segment line=clipped(guides,board.center(),board.center().add(board.axisVector(axis).multiply(length)),axis);
            if (line!=null && Math.hypot(line.end().x()-line.start().x(),line.end().y()-line.start().y())>=4)result.add(line);
        }
        return result;
    }
    public static List<Ring> rings(Freehand3DModel model, BoardTransform board, double width, double height, double aspect) {
        return rings(model,board,width,height,aspect,new BrushGuides(model,width,height,aspect,width,0,height));
    }
    public static List<Ring> rings(Freehand3DModel model, BoardTransform board, double width, double height, double aspect,
                                   BrushGuides guides) {
        List<Ring> result = new ArrayList<>(3);
        if(model.project(board.center(),width,height,aspect)==null)return result;
        double radius=scale(model,board,width,height,aspect);
        for (BoardTransform.Axis axis : BoardTransform.Axis.values()) {
            Vec3 first = perpendicular(axis);
            Vec3 second = cross(board.axisVector(axis), first);
            List<Segment> segments = new ArrayList<>(RING_STEPS);
            Vec3 previous=null;
            for (int i = 0; i <= RING_STEPS; i++) {
                double angle = Math.PI * 2 * i / RING_STEPS;
                Vec3 point = board.center().add(first.multiply(Math.cos(angle) * radius))
                    .add(second.multiply(Math.sin(angle) * radius));
                Segment segment=previous==null?null:clipped(guides,previous,point,axis);
                if(segment!=null)segments.add(segment);
                previous=point;
            }
            if (!segments.isEmpty()) result.add(new Ring(List.copyOf(segments), axis));
        }
        return result;
    }
    public static Hit pickAxis(List<Segment> segments, double x, double y) {
        Hit best = null;
        for (Segment segment : segments) {
            double distance = distanceToSegment(x, y, segment.start(), segment.end());
            if (preferred(segment.axis(),distance,best)) {
                best = new Hit(segment.axis(), distance);
            }
        }
        return best;
    }
    public static Hit pickRing(List<Ring> rings, double x, double y) {
        Hit best = null;
        for (Ring ring : rings) {
            for (Segment segment:ring.segments()) {
                double distance = distanceToSegment(x, y, segment.start(),segment.end());
                if (preferred(ring.axis(),distance,best)) {
                    best = new Hit(ring.axis(), distance);
                }
            }
        }
        return best;
    }
    private static boolean preferred(BoardTransform.Axis axis,double distance,Hit best) {
        return distance<=HIT_RADIUS&&(best==null||distance<best.distance()
            ||distance==best.distance()&&axis.ordinal()<best.axis().ordinal());
    }
    private static Segment clipped(BrushGuides guides,Vec3 start,Vec3 end,BoardTransform.Axis axis) {
        BrushGuides.Line line=guides.line(start,end);
        return line==null?null:new Segment(new Projected(line.start().x(),line.start().y()),
            new Projected(line.end().x(),line.end().y()),axis);
    }
    public static double pointerAngle(Freehand3DModel model, BoardTransform board, double x, double y,
                                      double width, double height, double aspect) {
        Projected center = project(model, board.center(), width, height, aspect);
        return center == null ? Double.NaN : Math.atan2(y - center.y(), x - center.x());
    }
    public static Projected project(Freehand3DModel model, Vec3 point, double width, double height, double aspect) {
        Freehand3DModel.Projected projected = model.project(point, width, height, aspect);
        return projected == null ? null : new Projected(projected.x(), projected.y());
    }
    public static double scale(Freehand3DModel model,BoardTransform board,double width,double height,double aspect) {
        Freehand3DModel.Projected center=model.project(board.center(),width,height,aspect);
        return center==null?1:Math.max(.2,Math.min(32,60*2*center.depth()*Math.tan(Math.toRadians(model.fov()/2))/height));
    }
    public static Vec3 perpendicularAxis(BoardTransform.Axis axis){return perpendicular(axis);}
    public static Vec3 crossProduct(Vec3 a,Vec3 b){return cross(a,b);}
    public static double dot(Vec3 a,Vec3 b){return a.x()*b.x()+a.y()*b.y()+a.z()*b.z();}
    private static Vec3 perpendicular(BoardTransform.Axis axis) {
        return switch (axis) {
            case X -> new Vec3(0, 1, 0);
            case Y -> new Vec3(1, 0, 0);
            case Z -> new Vec3(1, 0, 0);
        };
    }
    private static Vec3 cross(Vec3 a, Vec3 b) {
        return new Vec3(a.y() * b.z() - a.z() * b.y(), a.z() * b.x() - a.x() * b.z(),
            a.x() * b.y() - a.y() * b.x());
    }
    private static double distanceToSegment(double x, double y, Projected a, Projected b) {
        double dx = b.x() - a.x(), dy = b.y() - a.y();
        double lengthSquared = dx * dx + dy * dy;
        if (lengthSquared < 1e-9) return Math.hypot(x - a.x(), y - a.y());
        double t = Math.max(0, Math.min(1, ((x - a.x()) * dx + (y - a.y()) * dy) / lengthSquared));
        return Math.hypot(x - (a.x() + dx * t), y - (a.y() + dy * t));
    }
}

