package dev.particlefx.storage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializer;
import dev.particlefx.effect.ParticleEffect;
import dev.particlefx.math.Vec3;
/**
 * Minecraft 1.18.2 ships Gson 2.8.9, which predates record support, so every record used in
 * persisted JSON needs an explicit adapter instead of reflective field access.
 */
public final class JsonSupport {
    private static final JsonSerializer<Vec3> VEC3_SERIALIZER=(src,type,context)->{
        JsonObject json=new JsonObject();
        json.addProperty("x",src.x());json.addProperty("y",src.y());json.addProperty("z",src.z());
        return json;
    };
    private static final JsonDeserializer<Vec3> VEC3_DESERIALIZER=(json,type,context)->{
        JsonObject object=json.getAsJsonObject();
        return new Vec3(
            object.has("x")?object.get("x").getAsDouble():0,
            object.has("y")?object.get("y").getAsDouble():0,
            object.has("z")?object.get("z").getAsDouble():0);
    };
    public static final Gson GSON=new GsonBuilder().setPrettyPrinting().disableHtmlEscaping()
        .registerTypeAdapter(Vec3.class,VEC3_SERIALIZER)
        .registerTypeAdapter(Vec3.class,VEC3_DESERIALIZER)
        .create();
    public static ParticleEffect copy(ParticleEffect e) { return GSON.fromJson(GSON.toJson(e),ParticleEffect.class); }
    private JsonSupport() {}
}
