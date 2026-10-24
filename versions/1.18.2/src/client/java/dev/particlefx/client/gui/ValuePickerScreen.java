package dev.particlefx.client.gui;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public final class ValuePickerScreen extends Screen {
    private final Screen parent;private final List<String> options;private final Consumer<String> choose;private final String kind;
    private String filter="";private int page;
    public ValuePickerScreen(Screen parent,String title,String kind,List<String> options,Consumer<String> choose){super(EditorI18n.tr(title));this.parent=parent;this.kind=kind;this.options=List.copyOf(options);this.choose=choose;}
    protected void init(){
        int w=Math.min(360,width-24),x=(width-w)/2;
        TextFieldWidget search=new TextFieldWidget(textRenderer,x,30,w,20,EditorI18n.tr("picker.search"));search.setText(filter);
        search.setChangedListener(value->{filter=value;page=0;rebuild();});addDrawableChild(search);setInitialFocus(search);
        int count=Math.max(1,(height-110)/24);List<String> visible=options.stream().filter(s->EditorI18n.matches(kind,s,filter)).toList();
        page=Math.min(page,Math.max(0,(visible.size()-1)/count));
        for(int i=0;i<count&&page*count+i<visible.size();i++){String value=visible.get(page*count+i);
            String label=EditorI18n.name(kind,value)+" ("+value+")";
            addDrawableChild(new ButtonWidget(x,58+i*24,w,20,Text.of(textRenderer.trimToWidth(label,w-12)),b->{choose.accept(value);client.setScreen(parent);}));}
        addDrawableChild(new ButtonWidget(x,height-42,40,20,Text.of("<"),b->{page=Math.max(0,page-1);rebuild();}));
        addDrawableChild(new ButtonWidget(x+44,height-42,40,20,Text.of(">"),b->{if((page+1)*count<visible.size())page++;rebuild();}));
        addDrawableChild(new ButtonWidget(x+w-70,height-42,70,20,EditorI18n.tr("button.back"),b->close()));
    }
    private void rebuild(){clearChildren();init();}
    public void render(MatrixStack matrices,int mouseX,int mouseY,float delta){textRenderer.drawWithShadow(matrices,title,width/2-textRenderer.getWidth(title)/2,12,0xFFFFFFFF);super.render(matrices,mouseX,mouseY,delta);}
    @Override public void renderBackground(MatrixStack matrices){fill(matrices,0,0,width,height,0xFF101512);}
    public void close(){client.setScreen(parent);}
    public boolean shouldPause(){return false;}
}
