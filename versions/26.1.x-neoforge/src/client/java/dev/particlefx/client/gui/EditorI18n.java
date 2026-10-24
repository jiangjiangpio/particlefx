package dev.particlefx.client.gui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.neoforged.fml.loading.FMLPaths;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import dev.particlefx.client.compat.RenderCompatibility;

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
    private static final EditorPreferences PREFERENCES = new EditorPreferences(
        FMLPaths.CONFIGDIR.get().resolve("particlefx-client.json"));
    private static LanguageMode languageMode = LanguageMode.AUTO;
    private static Tier tier = Tier.SIMPLE;

    static {
        try {
            languageMode = LanguageMode.fromId(PREFERENCES.text("language","auto"));
            tier = Tier.valueOf(PREFERENCES.text("tier","simple").toUpperCase(Locale.ROOT));
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

    public static Component tr(String key, Object... args) { return Component.literal(msg(key, args)); }
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
    public static boolean guideEnabled(EditorPreferences.Guide guide) { return PREFERENCES.guide(guide); }
    public static boolean setGuideEnabled(EditorPreferences.Guide guide,boolean enabled) {
        try{PREFERENCES.setGuide(guide,enabled);return true;}
        catch(java.io.IOException ignored){return false;}
    }
    public static RenderCompatibility.Mode renderCompatibility() { return PREFERENCES.renderCompatibility(); }
    public static boolean setRenderCompatibility(RenderCompatibility.Mode mode) {
        try { PREFERENCES.setRenderCompatibility(mode); return true; }
        catch (java.io.IOException ignored) { return false; }
    }
    private static String effectiveLanguage() {
        if (languageMode != LanguageMode.AUTO) return languageMode.id();
        String minecraftLanguage = "en_us";
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null && minecraft.getLanguageManager() != null) {
                minecraftLanguage = minecraft.getLanguageManager().getSelected();
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
            PREFERENCES.setEditor(languageMode.id(),tier.name().toLowerCase(Locale.ROOT));
        } catch (Exception ignored) {}
    }
    private EditorI18n() {}
}

