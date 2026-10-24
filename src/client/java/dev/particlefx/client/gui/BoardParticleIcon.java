package dev.particlefx.client.gui;

import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.Identifier;
import net.minecraft.util.Atlases;

/** Safe mode samples a static sprite, not a fake native particle or an offscreen world render. */
public final class BoardParticleIcon {
    private Sprite sprite;
    public BoardParticleIcon(MinecraftClient client,String particle) {
        try {
            Identifier id=Identifier.of(particle);
            var resource=client.getResourceManager().getResource(Identifier.of(id.getNamespace(),"particles/"+id.getPath()+".json"));
            if(resource.isEmpty())return;
            try(var reader=resource.get().getReader()) {
                var textures=JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("textures");
                if(textures==null||textures.isEmpty())return;
                Identifier texture=Identifier.of(textures.get(0).getAsString());
                var atlas=client.getAtlasManager().getAtlasTexture(Atlases.PARTICLES);
                if(atlas!=null) {
                    Sprite resolved=atlas.getSprite(texture);
                    if(resolved!=atlas.getMissingSprite())sprite=resolved;
                }
            }
        } catch(java.io.IOException|RuntimeException ignored) {}
    }
    public boolean available(){return sprite!=null;}
    public void draw(DrawContext context,int x,int y) {
        if(sprite!=null)context.drawSpriteStretched(RenderPipelines.GUI_TEXTURED,sprite,x-3,y-3,6,6);
        else context.drawStrokedRectangle(x-2,y-2,5,5,0xFFFFD166);
    }
}
