package dev.particlefx.client.gui;

import dev.particlefx.math.Vec3;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;

/** Three-dimensional drawing plane with a configurable offset on the remaining axis. */
public final class DrawingScreen extends Screen {
    private enum Plane {
        XY("XY"), XZ("XZ"), YZ("YZ");
        private final String id;
        Plane(String id){this.id=id;}
        private Plane next(){return values()[(ordinal()+1)%values().length];}
    }

    private final Screen parent;
    private final Consumer<List<Vec3>> apply;
    private final List<Vec3> points;
    private final int maxPoints;
    private int left,top,size;
    private boolean drawing;
    private String status="";
    private Plane plane=Plane.XY;
    private double layer;
    private TextFieldWidget layerField;

    public DrawingScreen(Screen parent,List<Vec3> existing,int maxPoints,Consumer<List<Vec3>> apply) {
        super(EditorI18n.tr("screen.drawing"));
        this.parent=parent;this.apply=apply;this.points=new ArrayList<>(existing);this.maxPoints=maxPoints;
    }
    public DrawingScreen(Screen parent,List<Vec3> existing,Consumer<List<Vec3>> apply) {
        this(parent,existing,EditorLimits.maxCustomPoints(),apply);
    }
    @Override protected void init() {
        size=Math.max(40,Math.min(width-24,height-132));left=(width-size)/2;top=48;
        int topWidth=Math.min(420,width-24),topLeft=(width-topWidth)/2;
        addDrawableChild(ButtonWidget.builder(EditorI18n.tr("button.plane",plane.id),
            b->{rememberLayer();plane=plane.next();clearAndInit();})
            .dimensions(topLeft,22,130,20).build());
        layerField=new TextFieldWidget(textRenderer,topLeft+136,22,130,20,EditorI18n.tr("label.plane_layer"));
        layerField.setText(Double.toString(layer));layerField.setMaxLength(16);addDrawableChild(layerField);
        int bw=Math.min(90,(width-32)/4),x=(width-(bw*4+12))/2,y=height-48;
        addDrawableChild(ButtonWidget.builder(EditorI18n.tr("button.undo"),b->{if(!points.isEmpty())points.remove(points.size()-1);}).dimensions(x,y,bw,20).build());
        addDrawableChild(ButtonWidget.builder(EditorI18n.tr("button.clear"),b->points.clear()).dimensions(x+bw+4,y,bw,20).build());
        addDrawableChild(ButtonWidget.builder(EditorI18n.tr("button.apply"),b->{
            if(!rememberLayer())return;
            if(points.isEmpty()){status=EditorI18n.msg("status.draw_empty");return;}
            apply.accept(List.copyOf(points));client.setScreen(parent);
        }).dimensions(x+2*(bw+4),y,bw,20).build());
        addDrawableChild(ButtonWidget.builder(EditorI18n.tr("button.back"),b->close()).dimensions(x+3*(bw+4),y,bw,20).build());
    }
    private boolean inside(double x,double y){return x>=left&&x<left+size&&y>=top&&y<top+size;}
    private boolean rememberLayer() {
        try {
            layer=Double.parseDouble(layerField==null?Double.toString(layer):layerField.getText().trim());
            if(!Double.isFinite(layer)||layer<-128||layer>128)throw new IllegalArgumentException();
            return true;
        } catch(Exception e) {
            status=EditorI18n.msg("status.invalid_layer");return false;
        }
    }
    private void add(double x,double y) {
        if(!inside(x,y)||points.size()>=maxPoints||!rememberLayer())return;
        double horizontal=Math.round((x-left-size/2.0)*8000/size)/1000.0;
        double vertical=Math.round((top+size/2.0-y)*8000/size)/1000.0;
        Vec3 p=switch(plane) {
            case XY -> new Vec3(horizontal,vertical,layer);
            case XZ -> new Vec3(horizontal,layer,vertical);
            case YZ -> new Vec3(layer,horizontal,vertical);
        };
        if(points.isEmpty()||Math.sqrt(Math.pow(points.get(points.size()-1).x()-p.x(),2)+Math.pow(points.get(points.size()-1).y()-p.y(),2)+Math.pow(points.get(points.size()-1).z()-p.z(),2))>=.08)points.add(p);
    }
    private double horizontal(Vec3 p){return switch(plane){case XY,XZ->p.x();case YZ->p.y();};}
    private double vertical(Vec3 p){return switch(plane){case XY->p.y();case XZ,YZ->p.z();};}
    @Override public boolean mouseClicked(double mouseX,double mouseY,int button) {
        if(button==0&&inside(mouseX,mouseY)){drawing=true;add(mouseX,mouseY);return true;}
        return super.mouseClicked(mouseX,mouseY,button);
    }
    @Override public boolean mouseDragged(double mouseX,double mouseY,int button,double dx,double dy) {
        if(drawing&&button==0){add(mouseX,mouseY);return true;}
        return super.mouseDragged(mouseX,mouseY,button,dx,dy);
    }
    @Override public boolean mouseReleased(double mouseX,double mouseY,int button){if(drawing){drawing=false;return true;}return super.mouseReleased(mouseX,mouseY,button);}
    @Override public void render(DrawContext ctx,int mouseX,int mouseY,float delta) {
        ctx.fill(left,top,left+size,top+size,0xFF17231F);
        ctx.fill(left+size/2,top,left+size/2+1,top+size,0xFF445D54);
        ctx.fill(left,top+size/2,left+size,top+size/2+1,0xFF445D54);
        for(Vec3 p:points) {
            int x=left+(int)((horizontal(p)+4)*size/8),y=top+(int)((4-vertical(p))*size/8);
            if(x>=left&&x<left+size&&y>=top&&y<top+size)ctx.fill(x-2,y-2,x+2,y+2,0xFF69E6D0);
        }
        ctx.drawCenteredTextWithShadow(textRenderer,EditorI18n.tr("screen.drawing"),width/2,8,0xFFFFFFFF);
        ctx.drawTextWithShadow(textRenderer,EditorI18n.tr("label.plane_layer_value",plane.id,layer),left,top+size+8,0xFFCCD0D4);
        ctx.drawCenteredTextWithShadow(textRenderer,EditorI18n.tr("label.points",points.size(),maxPoints),width/2,height-21,0xFFE0E0E0);
        super.render(ctx,mouseX,mouseY,delta);
        if(!status.isEmpty())ctx.drawCenteredTextWithShadow(textRenderer,status,width/2,height-60,0xFFFFD166);
    }
    @Override public void renderBackground(DrawContext ctx){ctx.fill(0,0,width,height,0xFF101512);}
    @Override public void close(){client.setScreen(parent);}
    @Override public boolean shouldPause(){return false;}
}
