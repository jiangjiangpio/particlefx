package dev.particlefx.client;

import dev.particlefx.ParticleFX;
import dev.particlefx.client.gui.*;
import dev.particlefx.client.compat.RenderBootstrap;
import dev.particlefx.client.preview.LocalPreview;
import dev.particlefx.effect.ParticleEffect;
import dev.particlefx.network.*;
import dev.particlefx.storage.*;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import org.lwjgl.glfw.GLFW;

@Mod(value=ParticleFX.ID,dist=Dist.CLIENT)
public final class ParticleFXClient {
    private final EditorResponseTransfer downloads=new EditorResponseTransfer();
    private long ticks;
    private KeyMapping key;
    public ParticleFXClient(IEventBus bus,net.neoforged.fml.ModContainer container) {
        container.registerExtensionPoint(net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,
            (mod,parent)->new EffectEditorScreen(parent,null));
        RenderBootstrap.log();
        bus.addListener((RegisterKeyMappingsEvent event)->{
            var category=new KeyMapping.Category(Identifier.fromNamespaceAndPath("particlefx","editor"));
            event.registerCategory(category);
            key=new KeyMapping("key.particlefx.editor",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_P,category);
            event.register(key);
        });
        bus.addListener((RegisterClientPayloadHandlersEvent event)->event.register(EditorResponse.ID,(response,context)->{
            Minecraft client=Minecraft.getInstance();
            try {
                var message=downloads.accept(response.action(),response.content(),ticks);
                if(message!=null)receive(client,new EditorResponse(message.action(),message.content()));
            } catch(RuntimeException ex) {
                downloads.clear();ParticleFX.LOGGER.warn("Invalid editor response: {}",ex.toString());
                notifyError(client,"status.response_invalid");
            }
        }));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event)->{
            Minecraft client=Minecraft.getInstance();
            if(downloads.expire(++ticks))notifyError(client,"status.download_expired");
            while(key!=null&&key.consumeClick())if(!(client.screen instanceof Freehand3DScreen))
                client.setScreen(new EffectEditorScreen(client.screen,null));
            LocalPreview.tick(client);
        });
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn event)->{downloads.clear();EditorLimits.reset();});
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event)->{downloads.clear();LocalPreview.stop();EditorLimits.reset();});
        NeoForge.EVENT_BUS.addListener((RegisterClientCommandsEvent event)->event.getDispatcher().register(
            LiteralArgumentBuilder.<net.minecraft.commands.CommandSourceStack>literal("particlefx-editor").executes(context->{
                Minecraft client=Minecraft.getInstance();
                client.execute(()->client.setScreen(new EffectEditorScreen(null,null)));return 1;
            })));
    }
    private void receive(Minecraft client,EditorResponse response) {
            if(response.action().equals("config")) {
                var config=com.google.gson.JsonParser.parseString(response.content()).getAsJsonObject();
                EditorLimits.update(config.get("maxCustomPoints").getAsInt(),config.get("canEdit").getAsBoolean());
            }
            if(response.action().equals("open")) {
                ParticleEffect effect=JsonSupport.GSON.fromJson(response.content(),ParticleEffect.class);
                // Command-open may arrive before config_get; validate against the absolute safety limit.
                EffectValidator.validate(effect,EffectValidator.MAX_CUSTOM_POINTS_LIMIT);
                client.setScreen(new EffectEditorScreen(client.screen,effect));
            } else if(client.screen instanceof EffectEditorScreen screen)screen.receive(response);
            else if(client.screen instanceof EditorSettingsScreen screen)screen.receive(response);
            else if(client.screen instanceof GroupListScreen screen)screen.receive(response);
    }
    private static void notifyError(Minecraft client,String key) {
        if(client.player!=null)client.player.sendSystemMessage(EditorI18n.tr(key));
    }
}

