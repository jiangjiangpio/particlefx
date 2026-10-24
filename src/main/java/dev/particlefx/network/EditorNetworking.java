package dev.particlefx.network;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.particlefx.ParticleFX;
import dev.particlefx.effect.ParticleEffect;
import dev.particlefx.permission.ParticleFXPermissions;
import dev.particlefx.server.ParticleFXServer;
import dev.particlefx.storage.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

/** Play-stage, opt-in payloads only. No login handshake and no required client receiver. */
public final class EditorNetworking {
    private static final Map<MinecraftServer,Map<UUID,EditorRateLimit>> LAST_REQUEST=new IdentityHashMap<>();
    private static final Map<MinecraftServer,Map<UUID,EditorTransfer>> UPLOADS=new IdentityHashMap<>();
    public static void register() {
        PayloadTypeRegistry.playC2S().register(EditorRequest.ID,EditorRequest.CODEC);
        PayloadTypeRegistry.playS2C().register(EditorResponse.ID,EditorResponse.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(EditorRequest.ID,(payload,context)->handle(context.server(),context.player(),payload));
    }
    private static void handle(MinecraftServer server,ServerPlayerEntity player,EditorRequest request) {
        if(!ServerPlayNetworking.canSend(player,EditorResponse.ID))return;
        var limits=LAST_REQUEST.computeIfAbsent(server,s->new HashMap<>());
        var uploads=UPLOADS.computeIfAbsent(server,s->new HashMap<>());
        int now=server.getTicks();
        var source=player.getCommandSource();var manager=ParticleFXServer.get(server);
        try {
            EffectValidator.require(request.json().getBytes(StandardCharsets.UTF_8).length<=EditorTransfer.MAX_FRAME_TEXT,"Payload too large");
            if(request.action().equals("save_part")) {
                ParticleFXPermissions.require(source,manager.names().contains(request.name())?"edit":"create");
                EditorTransfer transfer=uploads.computeIfAbsent(player.getUuid(),id->new EditorTransfer());
                if(!transfer.active(now))EffectValidator.require(limits.computeIfAbsent(player.getUuid(),id->new EditorRateLimit()).allow(now),"Too many editor requests");
                String json=transfer.accept(request.name(),request.json(),now);
                if(json==null)return;
                uploads.remove(player.getUuid());
                request=new EditorRequest("save",request.name(),json);
            } else EffectValidator.require(limits.computeIfAbsent(player.getUuid(),id->new EditorRateLimit()).allow(now),"Too many editor requests");
            switch(request.action()) {
                case "list" -> {ParticleFXPermissions.require(source,"use");send(player,"list",JsonSupport.GSON.toJson(manager.names()));}
                case "group_list" -> {ParticleFXPermissions.require(source,"use");send(player,"groups",JsonSupport.GSON.toJson(manager.groupNames()));}
                case "config_get" -> {
                    ParticleFXPermissions.require(source,"use");
                    JsonObject config=new JsonObject();
                    config.addProperty("maxCustomPoints",manager.config().maxCustomPoints);
                    config.addProperty("canEdit",ParticleFXPermissions.settingsAdmin(source));
                    send(player,"config",config.toString());
                }
                case "config_save" -> {
                    if(!ParticleFXPermissions.settingsAdmin(source))throw new IllegalArgumentException("Requires permission level 4");
                    JsonObject config=JsonParser.parseString(request.json()).getAsJsonObject();
                    if(!config.has("maxCustomPoints"))throw new IllegalArgumentException("Missing maxCustomPoints");
                    manager.updateMaxCustomPoints(config.get("maxCustomPoints").getAsInt());
                    send(player,"config_saved",Integer.toString(manager.config().maxCustomPoints));
                }
                case "get" -> {ParticleFXPermissions.require(source,"use");send(player,"effect",JsonSupport.GSON.toJson(manager.definition(request.name())));}
                case "save" -> {
                    ParticleFXPermissions.require(source,manager.names().contains(request.name())?"edit":"create");
                    ParticleEffect effect=JsonSupport.GSON.fromJson(request.json(),ParticleEffect.class);
                    EffectValidator.validate(effect,manager.config().maxCustomPoints);
                    EffectValidator.require(effect.name.equals(request.name()),"Request name and JSON name differ");manager.register(effect,true);send(player,"saved",effect.name);
                }
                case "delete" -> {ParticleFXPermissions.require(source,"delete");manager.delete(request.name());send(player,"deleted",request.name());}
                case "play" -> {ParticleFXPermissions.require(source,"play");manager.play(request.name(),player.getEntityWorld(),player.getEntityPos(),player.getUuid(),null,0);send(player,"status","Playing: "+request.name());}
                case "stop" -> {ParticleFXPermissions.require(source,"play");int n=manager.control(request.name(),ParticleFXPermissions.admin(source)?null:player.getUuid(),"stop");send(player,"status","Stopped: "+n);}
                case "group_play" -> {ParticleFXPermissions.require(source,"play");manager.playGroup(request.name(),player.getEntityWorld(),player.getEntityPos(),player.getUuid(),null);send(player,"group_played",request.name());}
                case "group_stop" -> {ParticleFXPermissions.require(source,"play");int n=manager.controlGroup(request.name(),ParticleFXPermissions.admin(source)?null:player.getUuid(),"stop");send(player,"group_stopped",Integer.toString(n));}
                default -> throw new IllegalArgumentException("Unknown editor action");
            }
        } catch(Exception ex) {
            uploads.remove(player.getUuid());
            ParticleFX.LOGGER.debug("Editor request rejected: {}",ex.toString());send(player,"error",Objects.toString(ex.getMessage(),ex.getClass().getSimpleName()));
        }
    }
    public static boolean open(ServerPlayerEntity player,ParticleEffect effect) {
        if(!ServerPlayNetworking.canSend(player,EditorResponse.ID))return false;
        send(player,"open",JsonSupport.GSON.toJson(effect));return true;
    }
    private static void send(ServerPlayerEntity p,String action,String content) {
        if(content.getBytes(StandardCharsets.UTF_8).length>EffectValidator.MAX_JSON_BYTES) {
            action="error";content="JSON exceeds "+EffectValidator.MAX_JSON_BYTES+" bytes";
        }
        if((action.equals("effect")||action.equals("open"))&&content.getBytes(StandardCharsets.UTF_8).length>EditorTransfer.MAX_FRAME_TEXT) {
            for(String part:EditorTransfer.split(content))ServerPlayNetworking.send(p,new EditorResponse(action+"_part",part));
            return;
        }
        ServerPlayNetworking.send(p,new EditorResponse(action,content));
    }
    public static void forget(MinecraftServer server,UUID id) {
        var m=LAST_REQUEST.get(server);if(m!=null)m.remove(id);
        var uploads=UPLOADS.get(server);if(uploads!=null)uploads.remove(id);
    }
    public static void clear(MinecraftServer server) {LAST_REQUEST.remove(server);UPLOADS.remove(server);}
    private EditorNetworking() {}
}
