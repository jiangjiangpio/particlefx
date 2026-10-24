package dev.particlefx;

import dev.particlefx.command.ParticleFXCommand;
import dev.particlefx.network.EditorNetworking;
import dev.particlefx.server.ParticleFXServer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(ParticleFX.ID)
public final class ParticleFX {
    public static final String ID="particlefx";
    public static final Logger LOGGER=LoggerFactory.getLogger(ID);
    public ParticleFX(IEventBus bus) {
        bus.addListener(EditorNetworking::register);
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event)->ParticleFXCommand.register(event.getDispatcher()));
        NeoForge.EVENT_BUS.addListener((ServerStartingEvent event)->ParticleFXServer.start(event.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event)->ParticleFXServer.get(event.getServer()).tick());
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event)->{
            if(event.getEntity() instanceof ServerPlayer player) {
                ParticleFXServer.get(player.level().getServer()).forget(player.getUUID());
                EditorNetworking.forget(player.level().getServer(),player.getUUID());
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event)->{
            EditorNetworking.clear(event.getServer());ParticleFXServer.stop(event.getServer());
        });
    }
}
