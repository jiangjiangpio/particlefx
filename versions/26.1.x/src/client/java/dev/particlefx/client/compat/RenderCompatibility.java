package dev.particlefx.client.compat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import com.google.gson.JsonParser;

/** Renderer-safe decision that does not touch Minecraft, GLFW, OpenGL, or Fabric client classes. */
public final class RenderCompatibility {
    public enum Mode {
        AUTO("auto"), SAFE("safe"), NORMAL("normal");
        private final String id;
        Mode(String id) { this.id = id; }
        public String id() { return id; }
        public static Mode fromId(String value) {
            for (Mode mode : values()) if (mode.id.equalsIgnoreCase(value)) return mode;
            return AUTO;
        }
    }

    private RenderCompatibility() {}
    public static boolean safe(Mode mode) {
        return safe(mode,System.getenv(),System.getProperty("org.lwjgl.opengl.libname"));
    }
    public static boolean safe(Mode mode,Map<String,String> environment,String glLibrary) {
        boolean mobile=false;
        for(String key:new String[]{"FCL_VERSION_CODE","FCL_NATIVEDIR","POJAV_NATIVEDIR","POJAV_RENDERER","POJAVEXEC_EGL"})
            mobile|=nonEmpty(environment.get(key));
        return mode==Mode.SAFE||(mode!=Mode.NORMAL&&(mobile||contains(glLibrary)));
    }
    public static boolean shouldApplyMixin(boolean safe,String mixinClassName) {
        return !safe||!(mixinClassName.equals("dev.particlefx.client.mixin.BrushCameraMixin")
            ||mixinClassName.equals("dev.particlefx.client.mixin.BrushViewMixin"));
    }
    public static Mode configured(Path path) {
        if (path == null) return Mode.AUTO;
        try {
            if (!Files.isRegularFile(path)) return Mode.AUTO;
            var root = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
            return Mode.fromId(root.has("renderCompatibility") ? root.get("renderCompatibility").getAsString() : "auto");
        } catch (IOException | RuntimeException ignored) {
            return Mode.AUTO;
        }
    }
    public static boolean detectedMobileLauncher() {
        return safe(Mode.AUTO,System.getenv(),System.getProperty("org.lwjgl.opengl.libname"));
    }
    private static boolean contains(String value) {
        if (!nonEmpty(value)) return false;
        String normalized = value.toLowerCase(java.util.Locale.ROOT);
        return normalized.contains("gl4es") || normalized.contains("pojav") || normalized.contains("opengles");
    }
    private static boolean nonEmpty(String value) { return value != null && !value.isBlank(); }
}

