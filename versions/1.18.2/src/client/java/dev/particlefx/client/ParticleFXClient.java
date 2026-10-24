package dev.particlefx.client;
import dev.particlefx.client.gui.EffectEditorScreen;
import dev.particlefx.client.gui.EditorSettingsScreen;
import dev.particlefx.client.gui.GroupListScreen;
import dev.particlefx.client.preview.LocalPreview;
import dev.particlefx.effect.ParticleEffect;
import dev.particlefx.network.*;
import dev.particlefx.storage.JsonSupport;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.PacketByteBuf;
import org.lwjgl.glfw.GLFW;

public final class ParticleFXClient implements ClientModInitializer {
    public void onInitializeClient() {
        KeyBinding key=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.particlefx.editor",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_P,"key.category.particlefx.editor"));
        ClientTickEvents.END_CLIENT_TICK.register(client->{while(key.wasPressed())client.setScreen(new EffectEditorScreen(client.currentScreen,null));LocalPreview.tick(client);});
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->LocalPreview.stop());
        ClientPlayNetworking.registerGlobalReceiver(EditorResponse.ID,(client,handler,buf,responseSender)->{
            EditorResponse response=EditorResponse.read(buf);
            // UI work must run on the render thread.
            client.execute(()->{
                MinecraftClient mc=MinecraftClient.getInstance();
                if(response.action().equals("open")) {
                    ParticleEffect effect=JsonSupport.GSON.fromJson(response.content(),ParticleEffect.class);mc.setScreen(new EffectEditorScreen(mc.currentScreen,effect));
                } else if(mc.currentScreen instanceof EffectEditorScreen screen)screen.receive(response);
                else if(mc.currentScreen instanceof EditorSettingsScreen screen)screen.receive(response);
                else if(mc.currentScreen instanceof GroupListScreen screen)screen.receive(response);
            });
        });
    }
}
