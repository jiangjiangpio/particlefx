package dev.particlefx.shape;
import dev.particlefx.math.Vec3;
import java.util.ArrayList;
import java.util.List;
public final class ShapeSettings {
    public String type = "circle";
    public double radius = 2;
    public double innerRadius = 1.5;
    public double height = 2;
    public double width = 2;
    public double length = 2;
    public double turns = 3;
    public int points = 5;
    /** Client tools bake geometry here; the server never evaluates user expressions. */
    public List<Vec3> vertices = new ArrayList<>();
}
