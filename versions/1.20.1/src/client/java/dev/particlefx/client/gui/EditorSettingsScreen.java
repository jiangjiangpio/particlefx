package dev.particlefx.client.gui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.particlefx.network.EditorRequest;
import dev.particlefx.network.EditorResponse;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;

public final class EditorSettingsScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget maxPoints;
    private String status="";
    private boolean requested,canEdit;

    public EditorSettingsScreen(Screen parent){super(EditorI18n.tr("screen.settings"));this.parent=parent;}
    @Override protected void init() {
        int w=Math.min(420,width-24),x=(width-w)/2;
        addDrawableChild(ButtonWidget.builder(EditorI18n.tr("button.language",EditorI18n.languageLabel()),
            b->{EditorI18n.LanguageMode[] modes=EditorI18n.LanguageMode.values();
                EditorI18n.setLanguageMode(modes[(EditorI18n.languageMode().ordinal()+1)%modes.length]);clearAndInit();})
            .dimensions(x,60,w,20).build());
        addDrawableChild(ButtonWidget.builder(EditorI18n.tr("button.tier",EditorI18n.msg("tier."+EditorI18n.tier().name().toLowerCase())),
            b->{EditorI18n.Tier next=EditorI18n.Tier.values()[(EditorI18n.tier().ordinal()+1)%EditorI18n.Tier.values().length];EditorI18n.setTier(next);clearAndInit();})
            .dimensions(x,86,w,20).build());
        maxPoints=new TextFieldWidget(textRenderer,x,126,w-100,20,EditorI18n.tr("label.static_points"));
        maxPoints.setText(Integer.toString(EditorLimits.maxCustomPoints()));maxPoints.setMaxLength(4);maxPoints.active=canEdit;
        addDrawableChild(maxPoints);
        ButtonWidget save=ButtonWidget.builder(EditorI18n.tr("button.save_server_config"),b->saveConfig())
            .dimensions(x+w-92,126,92,20).build();
        save.active=canEdit&&connected();addDrawableChild(save);
        addDrawableChild(ButtonWidget.builder(EditorI18n.tr("button.back"),b->back()).dimensions(x,164,w,20).build());
        if(!requested){requested=true;if(connected())request("config_get","");else status=EditorI18n.msg("status.config_server_missing");}
    }
    private boolean connected(){return client!=null&&client.getNetworkHandler()!=null&&ClientPlayNetworking.canSend(EditorRequest.ID);}
    private void request(String action,String json){
        if(!connected()){status=EditorI18n.msg("status.config_server_missing");return;}
        ClientPlayNetworking.send(EditorRequest.ID,EditorRequest.write(new EditorRequest(action,"",json)));status=EditorI18n.msg("status.config_loading");
    }
    private void saveConfig(){
        try {
            int value=Integer.parseInt(maxPoints.getText().trim());
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
                if(maxPoints!=null){maxPoints.setText(Integer.toString(EditorLimits.maxCustomPoints()));maxPoints.active=canEdit;}
                clearAndInit();
            }
            case "config_saved" -> {
                int value=Integer.parseInt(response.content());EditorLimits.update(value,true);
                if(maxPoints!=null)maxPoints.setText(Integer.toString(value));
                status=EditorI18n.msg("status.config_saved",value);
            }
            case "error" -> status=EditorI18n.msg("status.config_error",response.content());
        }
    }
    private void back() {
        if(parent instanceof EffectEditorScreen editor)editor.languageChanged();
        client.setScreen(parent);
    }
    @Override public void render(DrawContext ctx,int mouseX,int mouseY,float delta) {
        ctx.drawCenteredTextWithShadow(textRenderer,EditorI18n.tr("screen.settings"),width/2,25,0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer,EditorI18n.tr("label.settings_help"),width/2,42,0xFFCCD0D4);
        int w=Math.min(420,width-24),x=(width-w)/2;
        ctx.drawTextWithShadow(textRenderer,EditorI18n.tr("label.static_points"),x,116,0xFFE0E0E0);
        ctx.drawTextWithShadow(textRenderer,EditorI18n.tr(canEdit?"label.config_admin":"label.config_readonly"),x,190,0xFFCCD0D4);
        super.render(ctx,mouseX,mouseY,delta);
        if(!status.isEmpty())ctx.drawCenteredTextWithShadow(textRenderer,status,width/2,214,0xFFFFD166);
    }
    @Override public void renderBackground(DrawContext ctx){ctx.fill(0,0,width,height,0xFF101512);}
    @Override public void close(){back();}
    @Override public boolean shouldPause(){return false;}
}
