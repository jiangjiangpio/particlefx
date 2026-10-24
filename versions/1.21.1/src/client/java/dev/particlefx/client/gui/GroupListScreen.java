package dev.particlefx.client.gui;

import dev.particlefx.network.EditorRequest;
import dev.particlefx.network.EditorResponse;
import dev.particlefx.storage.JsonSupport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Server particle-group browser opened from the editor's Groups tab. */
public final class GroupListScreen extends Screen {
    private final Screen parent;
    private final List<String> groups=new ArrayList<>();
    private String status="";
    private boolean requested;
    private int left,panelWidth,page;

    public GroupListScreen(Screen parent) {
        super(EditorI18n.tr("screen.groups"));
        this.parent=parent;
    }

    @Override protected void init() {
        panelWidth=Math.min(560,width-24);
        left=(width-panelWidth)/2;
        addButton("button.refresh",left,42,130,()->request("group_list",""));
        addButton("button.back",left+panelWidth-130,42,130,this::close);
        int rows=Math.max(1,(height-112)/25);
        page=Math.min(page,Math.max(0,(groups.size()-1)/rows));
        for(int i=0;i<rows&&page*rows+i<groups.size();i++) {
            String name=groups.get(page*rows+i);
            int y=74+i*25;
            Text play=EditorI18n.tr("button.play_group",name);
            addDrawableChild(ButtonWidget.builder(Text.literal(textRenderer.trimToWidth(play.getString(),panelWidth-158)),b->request("group_play",name))
                .dimensions(left,y,panelWidth-150,20).build());
            addDrawableChild(ButtonWidget.builder(EditorI18n.tr("button.stop"),b->request("group_stop",name))
                .dimensions(left+panelWidth-140,y,140,20).build());
        }
        int pagerY=Math.min(height-42,74+rows*25);
        ButtonWidget previous=ButtonWidget.builder(EditorI18n.tr("button.previous"),b->{page--;clearAndInit();})
            .dimensions(left,pagerY,40,20).build();
        previous.active=page>0;
        addDrawableChild(previous);
        ButtonWidget next=ButtonWidget.builder(EditorI18n.tr("button.next"),b->{page++;clearAndInit();})
            .dimensions(left+44,pagerY,40,20).build();
        next.active=(page+1)*rows<groups.size();
        addDrawableChild(next);
        if(!requested){requested=true;request("group_list","");}
    }

    private void addButton(String key,int x,int y,int w,Runnable action) {
        addDrawableChild(ButtonWidget.builder(EditorI18n.tr(key),b->action.run()).dimensions(x,y,w,20).build());
    }

    private boolean connected() {
        return client!=null&&client.getNetworkHandler()!=null&&ClientPlayNetworking.canSend(EditorRequest.ID);
    }

    private void request(String action,String name) {
        if(!connected()){status=EditorI18n.msg("status.server_channel_missing");return;}
        ClientPlayNetworking.send(new EditorRequest(action,name,""));
        status=EditorI18n.msg("status.request",action);
    }

    public void receive(EditorResponse response) {
        switch(response.action()) {
            case "groups" -> {
                String[] values=JsonSupport.GSON.fromJson(response.content(),String[].class);
                groups.clear();
                if(values!=null)groups.addAll(Arrays.asList(values));
                Collections.sort(groups);
                status=EditorI18n.msg("status.server_groups",groups.size());
                clearAndInit();
            }
            case "group_played" -> status=EditorI18n.msg("status.group_played",response.content());
            case "group_stopped" -> status=EditorI18n.msg("status.group_stopped",response.content());
            case "error" -> {
                String error=response.content();
                status=error.startsWith("Missing permission: ")
                    ? EditorI18n.msg("status.permission",error.substring("Missing permission: ".length()))
                    : EditorI18n.msg("status.group_error",error);
            }
            default -> status=response.content();
        }
    }

    @Override public void render(DrawContext ctx,int mouseX,int mouseY,float delta) {
        ctx.drawCenteredTextWithShadow(textRenderer,EditorI18n.tr("screen.groups"),width/2,15,0xFFFFFFFF);
        if(groups.isEmpty())ctx.drawCenteredTextWithShadow(textRenderer,EditorI18n.tr("status.no_groups"),width/2,82,0xFFCCD0D4);
        super.render(ctx,mouseX,mouseY,delta);
        ctx.drawTextWithShadow(textRenderer,textRenderer.trimToWidth(status,panelWidth),left,height-18,0xFFFFD166);
    }

    @Override public void renderBackground(DrawContext ctx,int mouseX,int mouseY,float delta) {
        ctx.fill(0,0,width,height,0xFF101512);
    }

    @Override public void close(){client.setScreen(parent);}
    @Override public boolean shouldPause(){return false;}
}
