package dev.particlefx.client;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.particlefx.client.gui.EffectEditorScreen;
/** Loaded only through Mod Menu's optional entrypoint, never by the dedicated server. */
public final class ParticleFXModMenu implements ModMenuApi {
    public ConfigScreenFactory<?> getModConfigScreenFactory(){return parent->new EffectEditorScreen(parent,null);}
}
