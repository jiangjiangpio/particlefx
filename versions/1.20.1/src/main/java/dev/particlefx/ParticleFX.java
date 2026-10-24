package dev.particlefx;
import dev.particlefx.command.ParticleFXCommand;
import dev.particlefx.network.EditorNetworking;
import dev.particlefx.server.ParticleFXServer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ParticleFX implements ModInitializer {
    public static final String ID="particlefx";
    public static final Logger LOGGER=LoggerFactory.getLogger(ID);
    public void onInitialize() {
        EditorNetworking.register();
        CommandRegistrationCallback.EVENT.register((dispatcher,access,environment)->ParticleFXCommand.register(dispatcher));
        ServerLifecycleEvents.SERVER_STARTING.register(ParticleFXServer::start);
        ServerTickEvents.END_SERVER_TICK.register(server->ParticleFXServer.get(server).tick());
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->{
            ParticleFXServer.get(server).forget(handler.player.getUuid());EditorNetworking.forget(server,handler.player.getUuid());
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server->{EditorNetworking.clear(server);ParticleFXServer.stop(server);});
        LOGGER.info("ParticleFX initialized: vanilla server particles; client editor, Geyser and Mod Menu are optional");
    }
}
