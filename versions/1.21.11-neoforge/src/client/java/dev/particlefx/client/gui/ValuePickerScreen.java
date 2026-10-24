package dev.particlefx.client.gui;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.Tooltip;

public final class ValuePickerScreen extends Screen {
    private final Screen parent;private final List<String> options;private final Consumer<String> choose;private final String kind;
    private String filter="";private int page;
    public ValuePickerScreen(Screen parent,String title,String kind,List<String> options,Consumer<String> choose){super(EditorI18n.tr(title));this.parent=parent;this.kind=kind;this.options=List.copyOf(options);this.choose=choose;}
    protected void init(){
        int w=Math.min(360,width-24),x=(width-w)/2;
        EditBox search=new EditBox(font,x,30,w,20,EditorI18n.tr("picker.search"));search.setValue(filter);
        search.setResponder(value->{filter=value;page=0;rebuild();});addRenderableWidget(search);setInitialFocus(search);
        int count=Math.max(1,(height-110)/24);List<String> visible=options.stream().filter(s->EditorI18n.matches(kind,s,filter)).toList();
        page=Math.min(page,Math.max(0,(visible.size()-1)/count));
        for(int i=0;i<count&&page*count+i<visible.size();i++){String value=visible.get(page*count+i);
            String label=EditorI18n.name(kind,value)+" ("+value+")";
            addRenderableWidget(Button.builder(Component.literal(font.plainSubstrByWidth(label,w-12)),b->{choose.accept(value);minecraft.setScreen(parent);}).bounds(x,58+i*24,w,20).tooltip(Tooltip.create(Component.literal(label))).build());}
        addRenderableWidget(Button.builder(Component.literal("<"),b->{page=Math.max(0,page-1);rebuild();}).bounds(x,height-42,40,20).build());
        addRenderableWidget(Button.builder(Component.literal(">"),b->{if((page+1)*count<visible.size())page++;rebuild();}).bounds(x+44,height-42,40,20).build());
        addRenderableWidget(Button.builder(EditorI18n.tr("button.back"),b->onClose()).bounds(x+w-70,height-42,70,20).build());
    }
    private void rebuild(){clearWidgets();init();}
    public void render(GuiGraphics ctx,int x,int y,float delta){ctx.drawCenteredString(font,title,width/2,12,0xFFFFFFFF);super.render(ctx,x,y,delta);}
    @Override public void renderBackground(GuiGraphics ctx,int mouseX,int mouseY,float delta){ctx.fill(0,0,width,height,0xFF101512);}
    public void onClose(){minecraft.setScreen(parent);}
    public boolean isPauseScreen(){return false;}
}

