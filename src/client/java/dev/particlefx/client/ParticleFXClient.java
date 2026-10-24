package dev.particlefx.client;
import dev.particlefx.ParticleFX;
import dev.particlefx.client.gui.EditorI18n;
import dev.particlefx.client.gui.EffectEditorScreen;
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
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import dev.particlefx.client.compat.RenderBootstrap;
import com.mojang.blaze3d.systems.RenderSystem;

public final class ParticleFXClient implements ClientModInitializer {
    private final EditorResponseTransfer downloads = new EditorResponseTransfer();
    private long ticks;
    private boolean rendererLogged;

    public void onInitializeClient() {
        RenderBootstrap.log();
        KeyBinding key=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.particlefx.editor",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_P,KeyBinding.Category.create(Identifier.of("particlefx","editor"))));
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            if(!rendererLogged) {
                var device=RenderSystem.tryGetDevice();
                if(device!=null){rendererLogged=true;ParticleFX.LOGGER.info("Active render backend: {}, version={}, renderer={}",
                    device.getBackendName(),device.getVersion(),device.getRenderer());}
            }
            if(downloads.expire(++ticks))notifyError(client,"status.download_expired");
            while(key.wasPressed())if(!(client.currentScreen instanceof Freehand3DScreen))client.setScreen(new EffectEditorScreen(client.currentScreen,null));
            LocalPreview.tick(client);
        });
        ClientPlayConnectionEvents.JOIN.register((handler,sender,client)->{downloads.clear();EditorLimits.reset();});
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->{downloads.clear();LocalPreview.stop();EditorLimits.reset();});
        ClientCommandRegistrationCallback.EVENT.register((dispatcher,access)->dispatcher.register(ClientCommandManager.literal("particlefx-editor").executes(ctx->{
            MinecraftClient client=MinecraftClient.getInstance();client.send(()->client.setScreen(new EffectEditorScreen(null,null)));return 1;
        })));
        ClientPlayNetworking.registerGlobalReceiver(EditorResponse.ID,(response,context)->{
            MinecraftClient client=context.client();
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
    private void receive(MinecraftClient client,EditorResponse response) {
            if(response.action().equals("config")) {
                var config=com.google.gson.JsonParser.parseString(response.content()).getAsJsonObject();
                EditorLimits.update(config.get("maxCustomPoints").getAsInt(),config.get("canEdit").getAsBoolean());
            }
            if(response.action().equals("open")) {
                ParticleEffect effect=JsonSupport.GSON.fromJson(response.content(),ParticleEffect.class);
                // Command-open may arrive before config_get; validate against the absolute safety limit.
                EffectValidator.validate(effect,EffectValidator.MAX_CUSTOM_POINTS_LIMIT);
                client.setScreen(new EffectEditorScreen(client.currentScreen,effect));
            } else if(client.currentScreen instanceof EffectEditorScreen screen)screen.receive(response);
            else if(client.currentScreen instanceof EditorSettingsScreen screen)screen.receive(response);
            else if(client.currentScreen instanceof GroupListScreen screen)screen.receive(response);
    }
    private static void notifyError(MinecraftClient client,String key) {
        if(client.player!=null)client.player.sendMessage(EditorI18n.tr(key),false);
    }
}
