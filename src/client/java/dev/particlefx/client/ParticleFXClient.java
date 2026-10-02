package dev.particlefx.client;
import dev.particlefx.client.gui.EffectEditorScreen;
import dev.particlefx.client.gui.GroupListScreen;
import dev.particlefx.client.preview.LocalPreview;
import dev.particlefx.effect.ParticleEffect;
import dev.particlefx.network.*;
import dev.particlefx.storage.JsonSupport;
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

public final class ParticleFXClient implements ClientModInitializer {
    public void onInitializeClient() {
        KeyBinding key=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.particlefx.editor",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_P,KeyBinding.Category.create(Identifier.of("particlefx","editor"))));
        ClientTickEvents.END_CLIENT_TICK.register(client->{while(key.wasPressed())client.setScreen(new EffectEditorScreen(client.currentScreen,null));LocalPreview.tick(client);});
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->LocalPreview.stop());
        ClientCommandRegistrationCallback.EVENT.register((dispatcher,access)->dispatcher.register(ClientCommandManager.literal("particlefx-editor").executes(ctx->{
            MinecraftClient client=MinecraftClient.getInstance();client.send(()->client.setScreen(new EffectEditorScreen(null,null)));return 1;
        })));
        ClientPlayNetworking.registerGlobalReceiver(EditorResponse.ID,(response,context)->{
            MinecraftClient client=context.client();
            if(response.action().equals("open")) {
                ParticleEffect effect=JsonSupport.GSON.fromJson(response.content(),ParticleEffect.class);client.setScreen(new EffectEditorScreen(client.currentScreen,effect));
            } else if(client.currentScreen instanceof EffectEditorScreen screen)screen.receive(response);
            else if(client.currentScreen instanceof GroupListScreen screen)screen.receive(response);
        });
    }
}
