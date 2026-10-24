package dev.particlefx.client.compat;

import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.LoggerFactory;

/** Immutable startup choice shared by the Mixin plugin and editor; settings take effect next launch. */
public final class RenderBootstrap {
    public static final Path CONFIG = FabricLoader.getInstance().getConfigDir().resolve("particlefx-client.json");
    public static final RenderCompatibility.Mode MODE = RenderCompatibility.configured(CONFIG);
    public static final boolean SAFE = RenderCompatibility.safe(MODE);
    public static final String REASON = MODE != RenderCompatibility.Mode.AUTO ? "configured"
        : RenderCompatibility.detectedMobileLauncher() ? "FCL/Pojav/GL4ES environment" : "desktop/default";
    public static void log() {
        LoggerFactory.getLogger("particlefx").info("Client render compatibility: mode={}, safe={}, reason={}, world-camera Mixins={}",
            MODE.id(), SAFE, REASON, !SAFE);
    }
    private RenderBootstrap() {}
}
