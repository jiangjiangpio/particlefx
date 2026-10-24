package dev.particlefx.client.gui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.particlefx.network.EditorRequest;
import dev.particlefx.network.EditorResponse;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import dev.particlefx.client.compat.RenderCompatibility;
import dev.particlefx.client.compat.RenderBootstrap;

public final class EditorSettingsScreen extends Screen {
    private final Screen parent;
    private EditBox maxPoints;
    private String status="";
    private boolean requested,canEdit;

    public EditorSettingsScreen(Screen parent){super(EditorI18n.tr("screen.settings"));this.parent=parent;}
    @Override protected void init() {
        int w=Math.min(420,width-24),x=(width-w)/2;
        addRenderableWidget(Button.builder(EditorI18n.tr("button.language",EditorI18n.languageLabel()),
            b->{EditorI18n.LanguageMode[] modes=EditorI18n.LanguageMode.values();
                EditorI18n.setLanguageMode(modes[(EditorI18n.languageMode().ordinal()+1)%modes.length]);rebuildWidgets();})
            .bounds(x,60,w,20).build());
        addRenderableWidget(Button.builder(EditorI18n.tr("button.tier",EditorI18n.msg("tier."+EditorI18n.tier().name().toLowerCase())),
            b->{EditorI18n.Tier next=EditorI18n.Tier.values()[(EditorI18n.tier().ordinal()+1)%EditorI18n.Tier.values().length];EditorI18n.setTier(next);rebuildWidgets();})
            .bounds(x,86,w,20).build());
        maxPoints=new EditBox(font,x,126,w-100,20,EditorI18n.tr("label.static_points"));
        maxPoints.setValue(Integer.toString(EditorLimits.maxCustomPoints()));maxPoints.setMaxLength(4);maxPoints.active=canEdit;
        addRenderableWidget(maxPoints);
        Button save=Button.builder(EditorI18n.tr("button.save_server_config"),b->saveConfig())
            .bounds(x+w-92,126,92,20).build();
        save.active=canEdit&&connected();addRenderableWidget(save);
        addRenderableWidget(Button.builder(EditorI18n.tr("button.render_compatibility",
            EditorI18n.msg("render."+EditorI18n.renderCompatibility().id())),b->{
                var modes=RenderCompatibility.Mode.values();
                var next=modes[(EditorI18n.renderCompatibility().ordinal()+1)%modes.length];
                boolean saved=EditorI18n.setRenderCompatibility(next);
                rebuildWidgets();
                status=EditorI18n.msg(saved?"status.render_restart":"status.brush_preferences_failed");
            }).bounds(x,167,w,20).build());
        addRenderableWidget(Button.builder(EditorI18n.tr("button.back"),b->back()).bounds(x,204,w,20).build());
        if(!requested){requested=true;if(connected())request("config_get","");else status=EditorI18n.msg("status.config_server_missing");}
    }
    private boolean connected(){return minecraft!=null&&minecraft.getConnection()!=null&&ClientPlayNetworking.canSend(EditorRequest.ID);}
    private void request(String action,String json){
        if(!connected()){status=EditorI18n.msg("status.config_server_missing");return;}
        ClientPlayNetworking.send(new EditorRequest(action,"",json));status=EditorI18n.msg("status.config_loading");
    }
    private void saveConfig(){
        try {
            int value=Integer.parseInt(maxPoints.getValue().trim());
            if(value<1||value>4096)throw new IllegalArgumentException();
            JsonObject json=new JsonObject();json.addProperty("maxCustomPoints",value);request("config_save",json.toString());
        } catch(Exception e) {status=EditorI18n.msg("status.config_invalid");}
    }
    public void receive(EditorResponse response){
        switch(response.action()) {
            case "config" -> {
                JsonObject json=JsonParser.parseString(response.content()).getAsJsonObject();
                canEdit=json.get("canEdit").getAsBoolean();
                EditorLimits.update(json.get("maxCustomPoints").getAsInt(),canEdit);
                if(maxPoints!=null){maxPoints.setValue(Integer.toString(EditorLimits.maxCustomPoints()));maxPoints.active=canEdit;}
                rebuildWidgets();
            }
            case "config_saved" -> {
                int value=Integer.parseInt(response.content());EditorLimits.update(value,true);
                if(maxPoints!=null)maxPoints.setValue(Integer.toString(value));
                status=EditorI18n.msg("status.config_saved",value);
            }
            case "error" -> status=EditorI18n.msg("status.config_error",response.content());
        }
    }
    private void back() {
        if(parent instanceof EffectEditorScreen editor)editor.languageChanged();
        minecraft.setScreen(parent);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor ctx,int mouseX,int mouseY,float delta) {
        ctx.centeredText(font,EditorI18n.tr("screen.settings"),width/2,25,0xFFFFFFFF);
        ctx.centeredText(font,EditorI18n.tr("label.settings_help"),width/2,42,0xFFCCD0D4);
        int w=Math.min(420,width-24),x=(width-w)/2;
        ctx.text(font,EditorI18n.tr("label.static_points"),x,116,0xFFE0E0E0);
        ctx.text(font,EditorI18n.tr(canEdit?"label.config_admin":"label.config_readonly"),x,153,0xFFCCD0D4);
        ctx.text(font,font.plainSubstrByWidth(EditorI18n.msg("label.render_active",
            EditorI18n.msg(RenderBootstrap.SAFE?"render.safe":"render.normal")),w),x,191,0xFFCCD0D4);
        super.extractRenderState(ctx,mouseX,mouseY,delta);
        if(!status.isEmpty())ctx.centeredText(font,font.plainSubstrByWidth(status,w),width/2,Math.min(height-10,230),0xFFFFD166);
    }
    @Override public void extractBackground(GuiGraphicsExtractor ctx,int mouseX,int mouseY,float delta){ctx.fill(0,0,width,height,0xFF101512);}
    @Override public void onClose(){back();}
    @Override public boolean isPauseScreen(){return false;}
}

