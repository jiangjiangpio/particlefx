package dev.particlefx.permission;
import net.minecraft.server.command.ServerCommandSource;
/** Extensions receive the vanilla/config fallback; no permission mod is required. */
@FunctionalInterface
public interface PermissionProvider {
    boolean hasPermission(ServerCommandSource source,String node,boolean fallback);
}
