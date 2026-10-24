package dev.particlefx.client.gui;

import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.data.AtlasIds;

/** Safe mode samples a static sprite, not a fake native particle or an offscreen world render. */
public final class BoardParticleIcon {
    private TextureAtlasSprite sprite;
    public BoardParticleIcon(Minecraft minecraft,String particle) {
        try {
            Identifier id=Identifier.parse(particle);
            var resource=minecraft.getResourceManager().getResource(Identifier.fromNamespaceAndPath(id.getNamespace(),"particles/"+id.getPath()+".json"));
            if(resource.isEmpty())return;
            try(var reader=resource.get().openAsReader()) {
                var textures=JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("textures");
                if(textures==null||textures.isEmpty())return;
                Identifier texture=Identifier.parse(textures.get(0).getAsString());
                var atlas=minecraft.getAtlasManager().getAtlasOrThrow(AtlasIds.PARTICLES);
                if(atlas!=null) {
                    TextureAtlasSprite resolved=atlas.getSprite(texture);
                    if(resolved!=atlas.missingSprite())sprite=resolved;
                }
            }
        } catch(java.io.IOException|RuntimeException ignored) {}
    }
    public boolean available(){return sprite!=null;}
    public void draw(GuiGraphicsExtractor context,int x,int y) {
        if(sprite!=null)context.blitSprite(RenderPipelines.GUI_TEXTURED,sprite,x-3,y-3,6,6);
        else context.outline(x-2,y-2,5,5,0xFFFFD166);
    }
}

