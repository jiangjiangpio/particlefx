package dev.particlefx.storage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import dev.particlefx.effect.ParticleEffect;
import dev.particlefx.math.Vec3;
import java.io.IOException;
public final class JsonSupport {
    /**
     * Minecraft 1.19.2 ships Gson 2.8.9, which has no record support: its reflective adapter tries to
     * Field.set the final components of a record, which the JDK rejects. Registering an explicit adapter
     * keeps the on-disk shape ({"x":..,"y":..,"z":..}) identical to the 1.21 baseline without a dependency.
     */
    private static final TypeAdapter<Vec3> VEC3=new TypeAdapter<>() {
        @Override public void write(JsonWriter out,Vec3 value) throws IOException {
            if(value==null){out.nullValue();return;}
            out.beginObject();out.name("x").value(value.x());out.name("y").value(value.y());out.name("z").value(value.z());out.endObject();
        }
        @Override public Vec3 read(JsonReader in) throws IOException {
            if(in.peek()==com.google.gson.stream.JsonToken.NULL){in.nextNull();return null;}
            double x=0,y=0,z=0;in.beginObject();
            while(in.hasNext()){String name=in.nextName();
                if(name.equals("x"))x=in.nextDouble();
                else if(name.equals("y"))y=in.nextDouble();
                else if(name.equals("z"))z=in.nextDouble();
                else in.skipValue();
            }
            in.endObject();return new Vec3(x,y,z);
        }
    };
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping()
        .registerTypeAdapter(Vec3.class,VEC3).create();
    public static ParticleEffect copy(ParticleEffect e) { return GSON.fromJson(GSON.toJson(e),ParticleEffect.class); }
    private JsonSupport() {}
}
