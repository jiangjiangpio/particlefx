package dev.particlefx.client.gui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Consumer;
import dev.particlefx.client.compat.RenderCompatibility;

/** Shared client preferences; writes merge with the on-disk document, including unknown fields. */
public final class EditorPreferences {
    public enum Guide {
        AXES("showBrushAxes"), X("showBrushAxisX"), Y("showBrushAxisY"), Z("showBrushAxisZ"),
        ORIGIN("showBrushOrigin"), GROUND("showBrushGroundGrid"), PLANE("showBrushPlaneGrid");
        private final String key;
        Guide(String key){this.key=key;}
        public String key(){return key;}
    }
    private final Path path;
    private JsonObject json=new JsonObject();

    public EditorPreferences(Path path) {
        this.path=path.toAbsolutePath();
        try{json=read();}catch(IOException ignored){}
    }
    private JsonObject read() throws IOException {
        if(!Files.exists(path))return new JsonObject();
        try{return JsonParser.parseString(Files.readString(path,StandardCharsets.UTF_8)).getAsJsonObject();}
        catch(RuntimeException e){throw new IOException("Invalid client preferences",e);}
    }
    public String text(String key,String fallback) {
        var value=json.get(key);
        return value!=null&&value.isJsonPrimitive()&&value.getAsJsonPrimitive().isString()?value.getAsString():fallback;
    }
    public boolean guide(Guide guide) {
        var value=json.get(guide.key());
        return value==null||!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isBoolean()||value.getAsBoolean();
    }
    public void setGuide(Guide guide,boolean enabled) throws IOException {
        update(value->value.addProperty(guide.key(),enabled));
    }
    public void setEditor(String language,String tier) throws IOException {
        update(value->{value.addProperty("language",language);value.addProperty("tier",tier);});
    }
    public RenderCompatibility.Mode renderCompatibility() {
        return RenderCompatibility.Mode.fromId(text("renderCompatibility","auto"));
    }
    public void setRenderCompatibility(RenderCompatibility.Mode mode) throws IOException {
        update(value->value.addProperty("renderCompatibility",mode == null ? "auto" : mode.id()));
    }
    private void update(Consumer<JsonObject> change) throws IOException {
        JsonObject merged=read();
        change.accept(merged);
        Files.createDirectories(path.getParent());
        Path temporary=Files.createTempFile(path.getParent(),"particlefx-client-",".tmp");
        try {
            Files.writeString(temporary,merged.toString(),StandardCharsets.UTF_8);
            try{Files.move(temporary,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException e){Files.move(temporary,path,StandardCopyOption.REPLACE_EXISTING);}
            json=merged;
        } finally {Files.deleteIfExists(temporary);}
    }
}


