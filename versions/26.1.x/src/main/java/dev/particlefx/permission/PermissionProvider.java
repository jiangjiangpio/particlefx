package dev.particlefx.permission;
import net.minecraft.commands.CommandSourceStack;
/** Extensions receive the vanilla/config fallback; no permission mod is required. */
@FunctionalInterface
public interface PermissionProvider {
    boolean hasPermission(CommandSourceStack source,String node,boolean fallback);
}
