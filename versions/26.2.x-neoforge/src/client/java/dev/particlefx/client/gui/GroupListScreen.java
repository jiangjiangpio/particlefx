package dev.particlefx.client.gui;

import dev.particlefx.network.EditorRequest;
import dev.particlefx.network.EditorResponse;
import dev.particlefx.storage.JsonSupport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import dev.particlefx.client.ClientNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

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
        addButton("button.back",left+panelWidth-130,42,130,this::onClose);
        int rows=Math.max(1,(height-112)/25);
        page=Math.min(page,Math.max(0,(groups.size()-1)/rows));
        for(int i=0;i<rows&&page*rows+i<groups.size();i++) {
            String name=groups.get(page*rows+i);
            int y=74+i*25;
            Component play=EditorI18n.tr("button.play_group",name);
            addRenderableWidget(Button.builder(Component.literal(font.plainSubstrByWidth(play.getString(),panelWidth-158)),b->request("group_play",name))
                .bounds(left,y,panelWidth-150,20).build());
            addRenderableWidget(Button.builder(EditorI18n.tr("button.stop"),b->request("group_stop",name))
                .bounds(left+panelWidth-140,y,140,20).build());
        }
        int pagerY=Math.min(height-42,74+rows*25);
        Button previous=Button.builder(EditorI18n.tr("button.previous"),b->{page--;rebuildWidgets();})
            .bounds(left,pagerY,40,20).build();
        previous.active=page>0;
        addRenderableWidget(previous);
        Button next=Button.builder(EditorI18n.tr("button.next"),b->{page++;rebuildWidgets();})
            .bounds(left+44,pagerY,40,20).build();
        next.active=(page+1)*rows<groups.size();
        addRenderableWidget(next);
        if(!requested){requested=true;request("group_list","");}
    }

    private void addButton(String key,int x,int y,int w,Runnable action) {
        addRenderableWidget(Button.builder(EditorI18n.tr(key),b->action.run()).bounds(x,y,w,20).build());
    }

    private boolean connected() {
        return minecraft!=null&&minecraft.getConnection()!=null&&ClientNetworking.canSend(EditorRequest.ID);
    }

    private void request(String action,String name) {
        if(!connected()){status=EditorI18n.msg("status.server_channel_missing");return;}
        ClientNetworking.send(new EditorRequest(action,name,""));
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
                rebuildWidgets();
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

    @Override public void extractRenderState(GuiGraphicsExtractor ctx,int mouseX,int mouseY,float delta) {
        ctx.centeredText(font,EditorI18n.tr("screen.groups"),width/2,15,0xFFFFFFFF);
        if(groups.isEmpty())ctx.centeredText(font,EditorI18n.tr("status.no_groups"),width/2,82,0xFFCCD0D4);
        super.extractRenderState(ctx,mouseX,mouseY,delta);
        ctx.text(font,font.plainSubstrByWidth(status,panelWidth),left,height-18,0xFFFFD166);
    }

    @Override public void extractBackground(GuiGraphicsExtractor ctx,int mouseX,int mouseY,float delta) {
        ctx.fill(0,0,width,height,0xFF101512);
    }

    @Override public void onClose(){minecraft.setScreenAndShow(parent);}
    @Override public boolean isPauseScreen(){return false;}
}


