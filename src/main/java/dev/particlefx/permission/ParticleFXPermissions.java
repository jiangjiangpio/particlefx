package dev.particlefx.permission;
import dev.particlefx.server.ParticleFXServer;
import dev.particlefx.storage.EngineConfig;
import net.minecraft.command.permission.Permission;
import net.minecraft.command.permission.PermissionLevel;
import net.minecraft.server.command.ServerCommandSource;
public final class ParticleFXPermissions {
    private static PermissionProvider provider=(source,node,fallback)->fallback;
    public static void setProvider(PermissionProvider p) {provider=java.util.Objects.requireNonNull(p);}
    public static boolean admin(ServerCommandSource s) {
        EngineConfig c=ParticleFXServer.get(s.getServer()).config();
        return s.getPermissions().hasPermission(new Permission.Level(PermissionLevel.fromLevel(c.opPermissionLevel)));
    }
    public static boolean allowed(ServerCommandSource s,String action) {
        EngineConfig c=ParticleFXServer.get(s.getServer()).config();
        boolean fallback=admin(s)||switch(action) {case "use"->c.allowPlayerUse;case "play"->c.allowPlayerPlay;default->false;};
        return provider.hasPermission(s,"particlefx."+action,fallback);
    }
    public static void require(ServerCommandSource s,String action) {
        if(!allowed(s,action))throw new IllegalArgumentException("Missing permission: particlefx."+action);
    }
    private ParticleFXPermissions() {}
}
