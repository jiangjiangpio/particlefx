package dev.particlefx.permission;
import dev.particlefx.server.ParticleFXServer;
import dev.particlefx.storage.EngineConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
public final class ParticleFXPermissions {
    private static PermissionProvider provider=(source,node,fallback)->fallback;
    public static void setProvider(PermissionProvider p) {provider=java.util.Objects.requireNonNull(p);}
    public static boolean admin(CommandSourceStack s) {
        EngineConfig c=ParticleFXServer.get(s.getServer()).config();
        return s.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(c.opPermissionLevel)));
    }
    public static boolean settingsAdmin(CommandSourceStack s) {
        return s.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(4)));
    }
    public static boolean allowed(CommandSourceStack s,String action) {
        EngineConfig c=ParticleFXServer.get(s.getServer()).config();
        boolean fallback=admin(s)||switch(action) {case "use"->c.allowPlayerUse;case "play"->c.allowPlayerPlay;default->false;};
        return provider.hasPermission(s,"particlefx."+action,fallback);
    }
    public static void require(CommandSourceStack s,String action) {
        if(!allowed(s,action))throw new IllegalArgumentException("Missing permission: particlefx."+action);
    }
    private ParticleFXPermissions() {}
}
