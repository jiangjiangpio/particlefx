package dev.particlefx.client;
import dev.particlefx.ParticleFX;
import dev.particlefx.client.gui.EditorI18n;
import dev.particlefx.client.gui.EffectEditorScreen;
import com.mojang.blaze3d.platform.InputConstants;
import dev.particlefx.client.gui.EditorSettingsScreen;
import dev.particlefx.client.gui.GroupListScreen;
import dev.particlefx.client.gui.EditorLimits;
import dev.particlefx.client.gui.Freehand3DScreen;
import dev.particlefx.client.preview.LocalPreview;
import dev.particlefx.effect.ParticleEffect;
import dev.particlefx.network.*;
import dev.particlefx.storage.JsonSupport;
import dev.particlefx.storage.EffectValidator;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class ParticleFXClient implements ClientModInitializer {
    private final EditorResponseTransfer downloads = new EditorResponseTransfer();
    private long ticks;

    public void onInitializeClient() {
        dev.particlefx.client.compat.RenderBootstrap.log();
        KeyMapping key=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.particlefx.editor",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_P,KeyMapping.Category.register(Identifier.fromNamespaceAndPath("particlefx","editor"))));
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            if(downloads.expire(++ticks))notifyError(client,"status.download_expired");
            while(key.consumeClick())if(!(client.gui.screen() instanceof Freehand3DScreen))client.setScreenAndShow(new EffectEditorScreen(client.gui.screen(),null));
            LocalPreview.tick(client);
        });
        ClientPlayConnectionEvents.JOIN.register((handler,sender,client)->{downloads.clear();EditorLimits.reset();});
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->{downloads.clear();LocalPreview.stop();EditorLimits.reset();});
        ClientCommandRegistrationCallback.EVENT.register((dispatcher,access)->dispatcher.register(ClientCommands.literal("particlefx-editor").executes(ctx->{
            Minecraft client=Minecraft.getInstance();client.setScreenAndShow(new EffectEditorScreen(null,null));return 1;
        })));
        ClientPlayNetworking.registerGlobalReceiver(EditorResponse.ID,(response,context)->{
            Minecraft client=context.client();
            try {
                var message=downloads.accept(response.action(),response.content(),ticks);
                if(message==null)return;
                receive(client,new EditorResponse(message.action(),message.content()));
            } catch(RuntimeException ex) {
                downloads.clear();
                ParticleFX.LOGGER.warn("Invalid editor response: {}",ex.toString());
                notifyError(client,"status.response_invalid");
            }
        });
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
                client.setScreenAndShow(new EffectEditorScreen(client.gui.screen(),effect));
            } else if(client.gui.screen() instanceof EffectEditorScreen screen)screen.receive(response);
            else if(client.gui.screen() instanceof EditorSettingsScreen screen)screen.receive(response);
            else if(client.gui.screen() instanceof GroupListScreen screen)screen.receive(response);
    }
    private static void notifyError(Minecraft client,String key) {
        if(client.player!=null)client.player.sendSystemMessage(EditorI18n.tr(key));
    }
}


