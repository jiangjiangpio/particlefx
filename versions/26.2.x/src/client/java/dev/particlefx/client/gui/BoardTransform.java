package dev.particlefx.client.gui;

import dev.particlefx.math.Vec3;
import org.joml.Quaterniond;
import org.joml.Vector3d;

/** Session-only transform for the 3D drawing board. It never enters effect JSON. */
public final class BoardTransform {
    public enum Axis { X, Y, Z }
    public record Hit(Vec3 point, double distance, double localX, double localY) {}
    public record Snapshot(Vec3 center, double qx, double qy, double qz, double qw, double width, double height) {}

    private static final double MIN_SIZE = 1, MAX_SIZE = 64;
    private final Vec3 baseX, baseY, baseNormal;
    private Vec3 center, xAxis, yAxis, normal;
    private double width, height;
    private final Quaterniond rotation = new Quaterniond();
    private final Vec3 initialCenter;
    private final Quaterniond initialRotation;

    public BoardTransform(Vec3 center, Vec3 xAxis, Vec3 yAxis, Vec3 normal, double width, double height) {
        this.baseX = new Vec3(-1, 0, 0);
        this.baseY = new Vec3(0, 1, 0);
        this.baseNormal = new Vec3(0, 0, 1);
        Vec3 n = normalize(normal);
        rotation.rotationY(Math.atan2(n.x(), n.z())).rotateX(Math.asin(-n.y()));
        initialRotation = new Quaterniond(rotation);
        this.center = center;
        this.initialCenter = center;
        this.width = clampSize(width);
        this.height = clampSize(height);
        rebuild();
    }

    public Vec3 center() { return center; }
    public Vec3 xAxis() { return xAxis; }
    public Vec3 yAxis() { return yAxis; }
    public Vec3 normal() { return normal; }
    public double width() { return width; }
    public double height() { return height; }
    public double rotationX() { return Math.toDegrees(rotation.getEulerAnglesZYX(new Vector3d()).x); }
    public double rotationY() { return Math.toDegrees(rotation.getEulerAnglesZYX(new Vector3d()).y); }
    public double rotationZ() { return Math.toDegrees(rotation.getEulerAnglesZYX(new Vector3d()).z); }

    public Snapshot snapshot() {
        return new Snapshot(center, rotation.x, rotation.y, rotation.z, rotation.w, width, height);
    }

    public void restore(Snapshot snapshot) {
        center = snapshot.center();
        width = clampSize(snapshot.width());
        height = clampSize(snapshot.height());
        rotation.set(snapshot.qx(), snapshot.qy(), snapshot.qz(), snapshot.qw()).normalize();
        rebuild();
    }

    public void reset() {
        center = initialCenter;
        width = 8;
        height = 8;
        rotation.set(initialRotation);
        rebuild();
    }

    public void setCenter(Vec3 value) {
        if (value == null || !value.finite() || Math.abs(value.x()) > 128 || Math.abs(value.y()) > 128 || Math.abs(value.z()) > 128)
            throw new IllegalArgumentException("Invalid board position");
        center = value;
    }
    public void setSize(double width, double height) {
        double checkedWidth = clampSize(width), checkedHeight = clampSize(height);
        this.width = checkedWidth;
        this.height = checkedHeight;
    }
    public void setRotation(double x, double y, double z) {
        if (!Double.isFinite(x + y + z)) throw new IllegalArgumentException("Invalid board rotation");
        rotation.rotationZYX(Math.toRadians(clampAngle(z)), Math.toRadians(clampAngle(y)), Math.toRadians(clampAngle(x)));
        rebuild();
    }
    public void rotate(Axis axis, double degrees) {
        if (axis == null || !Double.isFinite(degrees)) return;
        Vec3 direction = axisVector(axis);
        rotation.premul(new Quaterniond().rotationAxis(Math.toRadians(degrees), direction.x(), direction.y(), direction.z())).normalize();
        rebuild();
    }
    public void translate(Axis axis, double amount) {
        if (axis == null || !Double.isFinite(amount)) return;
        Vec3 next = center.add(axisVector(axis).multiply(amount));
        setCenter(new Vec3(Math.max(-128, Math.min(128, next.x())), Math.max(-128, Math.min(128, next.y())),
            Math.max(-128, Math.min(128, next.z()))));
    }
    public Vec3 axisVector(Axis axis) {
        return switch (axis) {
            case X -> new Vec3(1, 0, 0);
            case Y -> new Vec3(0, 1, 0);
            case Z -> new Vec3(0, 0, 1);
        };
    }
    public Vec3 world(double localX, double localY) {
        return center.add(xAxis.multiply(localX)).add(yAxis.multiply(localY));
    }
    public Vec3 world(double localX, double localY, double localNormal) {
        return world(localX, localY).add(normal.multiply(localNormal));
    }
    public Vec3[] corners() {
        double halfWidth = width / 2, halfHeight = height / 2;
        return new Vec3[] {
            world(-halfWidth, -halfHeight),
            world(halfWidth, -halfHeight),
            world(halfWidth, halfHeight),
            world(-halfWidth, halfHeight)
        };
    }
    public Vec3 local(Vec3 worldPoint) {
        Vec3 delta = worldPoint.add(center.multiply(-1));
        return new Vec3(dot(delta, xAxis), dot(delta, yAxis), dot(delta, normal));
    }
    public Hit intersect(Vec3 rayOrigin, Vec3 rayDirection) {
        if (rayOrigin == null || rayDirection == null || !rayOrigin.finite() || !rayDirection.finite()) return null;
        double denominator = dot(rayDirection, normal);
        if (!Double.isFinite(denominator) || Math.abs(denominator) < .01) return null;
        double distance = dot(center.add(rayOrigin.multiply(-1)), normal) / denominator;
        if (!Double.isFinite(distance) || distance <= 1e-5) return null;
        Vec3 point = rayOrigin.add(rayDirection.multiply(distance));
        Vec3 local = local(point);
        if (Math.abs(local.x()) > width / 2 + 1e-7 || Math.abs(local.y()) > height / 2 + 1e-7) return null;
        return new Hit(point, distance, local.x(), local.y());
    }

    private void rebuild() {
        xAxis = apply(baseX);
        yAxis = apply(baseY);
        normal = apply(baseNormal);
    }
    private Vec3 apply(Vec3 value) {
        Vector3d result = rotation.transform(new Vector3d(value.x(), value.y(), value.z()));
        return new Vec3(result.x, result.y, result.z);
    }
    private static double dot(Vec3 a, Vec3 b) { return a.x() * b.x() + a.y() * b.y() + a.z() * b.z(); }
    private static Vec3 normalize(Vec3 value) {
        double length = Math.sqrt(dot(value, value));
        if (!Double.isFinite(length) || length < 1e-8) throw new IllegalArgumentException("Invalid board axis");
        return value.multiply(1 / length);
    }
    private static double clampSize(double value) {
        if (!Double.isFinite(value) || value < MIN_SIZE || value > MAX_SIZE) throw new IllegalArgumentException("Invalid board size");
        return value;
    }
    private static double clampAngle(double value) {
        double result = value % 360;
        return result < -180 ? result + 360 : result > 180 ? result - 360 : result;
    }
}


