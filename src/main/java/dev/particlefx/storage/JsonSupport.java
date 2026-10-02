package dev.particlefx.storage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.particlefx.effect.ParticleEffect;
public final class JsonSupport {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static ParticleEffect copy(ParticleEffect e) { return GSON.fromJson(GSON.toJson(e),ParticleEffect.class); }
    private JsonSupport() {}
}
