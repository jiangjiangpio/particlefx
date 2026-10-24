package dev.particlefx.client.gui;

import dev.particlefx.math.Vec3;

/** A drag captures a fixed world-axis constraint and start transform, not accumulated screen deltas. */
public final class BoardDrag {
    private final BoardTransform board;
    private final BoardTransform.Snapshot start;
    private final BoardTransform.Axis axis;
    private final boolean rotation;
    private final Vec3 planeNormal,axisDirection,first,second;
    private final double initial;
    private double previousAngle,unwrapped;

    private BoardDrag(BoardTransform board,BoardTransform.Axis axis,boolean rotation,Vec3 planeNormal,double initial) {
        this.board=board;this.axis=axis;this.rotation=rotation;this.planeNormal=planeNormal;this.initial=initial;
        start=board.snapshot();axisDirection=board.axisVector(axis);
        first=BoardGizmo.perpendicularAxis(axis);second=BoardGizmo.crossProduct(axisDirection,first);
        previousAngle=initial;
    }
    public BoardTransform.Axis axis(){return axis;}
    public static BoardDrag begin(BoardTransform board,Freehand3DModel model,BoardTransform.Axis axis,boolean rotation,Vec3 ray) {
        Vec3 direction=board.axisVector(axis),normal;
        if(rotation)normal=direction;
        else {
            Vec3 toward=board.center().add(model.camera().multiply(-1));
            normal=toward.add(direction.multiply(-BoardGizmo.dot(toward,direction)));
            double length=Math.sqrt(BoardGizmo.dot(normal,normal));
            if(length<1e-6)return null;
            normal=normal.multiply(1/length);
        }
        Vec3 hit=intersection(model.camera(),ray,board.center(),normal);
        if(hit==null)return null;
        Vec3 relative=hit.add(board.center().multiply(-1));
        Vec3 first=BoardGizmo.perpendicularAxis(axis),second=BoardGizmo.crossProduct(direction,first);
        double initial=rotation?angle(relative,first,second):BoardGizmo.dot(relative,direction);
        return Double.isFinite(initial)?new BoardDrag(board,axis,rotation,normal,initial):null;
    }
    public boolean update(Freehand3DModel model,Vec3 ray) {
        Vec3 hit=intersection(model.camera(),ray,start.center(),planeNormal);
        if(hit==null)return false;
        Vec3 relative=hit.add(start.center().multiply(-1));
        double amount;
        if(rotation) {
            double next=angle(relative,first,second);
            if(!Double.isFinite(next))return false;
            double step=Math.atan2(Math.sin(next-previousAngle),Math.cos(next-previousAngle));
            unwrapped+=step;previousAngle=next;
            amount=Math.toDegrees(unwrapped);
        } else amount=BoardGizmo.dot(relative,axisDirection)-initial;
        if(!Double.isFinite(amount))return false;
        board.restore(start);
        if(rotation)board.rotate(axis,amount);else board.translate(axis,amount);
        return true;
    }
    private static double angle(Vec3 vector,Vec3 first,Vec3 second) {
        double x=BoardGizmo.dot(vector,first),y=BoardGizmo.dot(vector,second);
        return x*x+y*y<1e-6?Double.NaN:Math.atan2(y,x);
    }
    private static Vec3 intersection(Vec3 origin,Vec3 ray,Vec3 center,Vec3 normal) {
        double denominator=BoardGizmo.dot(ray,normal);
        if(!Double.isFinite(denominator)||Math.abs(denominator)<.01)return null;
        double distance=BoardGizmo.dot(center.add(origin.multiply(-1)),normal)/denominator;
        if(!Double.isFinite(distance)||distance<.05||distance>2048)return null;
        return origin.add(ray.multiply(distance));
    }
}

