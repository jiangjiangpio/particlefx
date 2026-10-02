package dev.particlefx.client.gui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/** Client-only editor language and tool-level preferences. */
public final class EditorI18n {
    public enum Tier { SIMPLE, ADVANCED, EXPERT }
    public enum LanguageMode {
        AUTO("auto"),
        CHINESE("zh_cn"),
        ENGLISH("en_us");

        private final String id;

        LanguageMode(String id) { this.id = id; }
        public String id() { return id; }

        static LanguageMode fromId(String id) {
            for (LanguageMode mode : values()) if (mode.id.equals(id)) return mode;
            return AUTO;
        }
    }

    private static final Map<String, String> EN = load("en_us");
    private static final Map<String, String> ZH = load("zh_cn");
    private static final Path CONFIG = FabricLoader.getInstance().getConfigDir().resolve("particlefx-client.json");
    private static LanguageMode languageMode = LanguageMode.AUTO;
    private static Tier tier = Tier.SIMPLE;

    static {
        try {
            if (Files.isRegularFile(CONFIG)) {
                JsonObject json = JsonParser.parseString(Files.readString(CONFIG)).getAsJsonObject();
                if (json.has("language")) languageMode = LanguageMode.fromId(json.get("language").getAsString());
                if (json.has("tier")) tier = Tier.valueOf(json.get("tier").getAsString().toUpperCase(Locale.ROOT));
            }
        } catch (Exception ignored) {
            languageMode = LanguageMode.AUTO;
            tier = Tier.SIMPLE;
        }
    }

    private static Map<String, String> load(String language) {
        Map<String, String> result = new HashMap<>();
        try (var stream = EditorI18n.class.getResourceAsStream("/assets/particlefx/lang/" + language + ".json")) {
            if (stream == null) return result;
            JsonObject json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var entry : json.entrySet()) result.put(entry.getKey(), entry.getValue().getAsString());
        } catch (Exception ignored) {}
        return result;
    }

    public static Text tr(String key, Object... args) { return Text.literal(msg(key, args)); }
    public static String msg(String key, Object... args) {
        String full = "particlefx." + key;
        String pattern = active().getOrDefault(full, EN.getOrDefault(full, ZH.getOrDefault(full, full)));
        try { return String.format(Locale.ROOT, pattern, args); }
        catch (Exception ignored) { return pattern; }
    }
    public static String name(String kind, String id) {
        String key = "particlefx." + kind + "." + (kind.equals("particle") ? id.replace("minecraft:", "") : id);
        return active().getOrDefault(key, EN.getOrDefault(key, ZH.getOrDefault(key, id)));
    }
    public static boolean matches(String kind, String id, String query) {
        String q = query.toLowerCase(Locale.ROOT);
        String key = "particlefx." + kind + "." + (kind.equals("particle") ? id.replace("minecraft:", "") : id);
        return id.toLowerCase(Locale.ROOT).contains(q)
            || EN.getOrDefault(key, "").toLowerCase(Locale.ROOT).contains(q)
            || ZH.getOrDefault(key, "").toLowerCase(Locale.ROOT).contains(q);
    }
    private static Map<String, String> active() { return language().equals("zh_cn") ? ZH : EN; }
    public static String language() { return effectiveLanguage(); }
    public static LanguageMode languageMode() { return languageMode; }
    public static Tier tier() { return tier; }
    public static String languageLabel() {
        return msg("language." + languageMode.name().toLowerCase(Locale.ROOT));
    }
    public static void setLanguageMode(LanguageMode value) {
        languageMode = value == null ? LanguageMode.AUTO : value;
        save();
    }
    public static void setTier(Tier value) { tier = value; save(); }
    private static String effectiveLanguage() {
        if (languageMode != LanguageMode.AUTO) return languageMode.id();
        String minecraftLanguage = "en_us";
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.getLanguageManager() != null) {
                minecraftLanguage = client.getLanguageManager().getLanguage();
            }
        } catch (Exception ignored) {}
        return isChinese(minecraftLanguage) ? "zh_cn" : "en_us";
    }
    private static boolean isChinese(String language) {
        if (language == null) return false;
        String normalized = language.toLowerCase(Locale.ROOT).replace('-', '_');
        return normalized.equals("lzh") || normalized.startsWith("lzh_")
            || normalized.equals("zh") || normalized.startsWith("zh_");
    }
    private static void save() {
        try {
            Files.createDirectories(CONFIG.getParent());
            JsonObject json = new JsonObject();
            json.addProperty("language", languageMode.id());
            json.addProperty("tier", tier.name().toLowerCase(Locale.ROOT));
            Files.writeString(CONFIG, json.toString(), StandardCharsets.UTF_8);
        } catch (Exception ignored) {}
    }
    private EditorI18n() {}
}
