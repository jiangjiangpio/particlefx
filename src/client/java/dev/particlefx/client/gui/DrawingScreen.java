package dev.particlefx.client.gui;

import dev.particlefx.math.Vec3;
import dev.particlefx.storage.EffectValidator;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;

/** XY drawing plane, centered on the effect origin. One stroke samples at most 256 vertices. */
public final class DrawingScreen extends Screen {
    private final Screen parent;
    private final Consumer<List<Vec3>> apply;
    private final List<Vec3> points;
    private int left, top, size;
    private boolean drawing;
    private String status = "";
    public DrawingScreen(Screen parent, List<Vec3> existing, Consumer<List<Vec3>> apply) {
        super(EditorI18n.tr("screen.drawing"));
        this.parent=parent;this.apply=apply;this.points=new ArrayList<>(existing);
    }
    @Override protected void init() {
        size=Math.max(40,Math.min(width-24,height-100));left=(width-size)/2;top=32;
        int bw=Math.min(90,(width-32)/4),x=(width-(bw*4+12))/2,y=height-48;
        addDrawableChild(ButtonWidget.builder(EditorI18n.tr("button.undo"),b->{if(!points.isEmpty())points.removeLast();}).dimensions(x,y,bw,20).build());
        addDrawableChild(ButtonWidget.builder(EditorI18n.tr("button.clear"),b->points.clear()).dimensions(x+bw+4,y,bw,20).build());
        addDrawableChild(ButtonWidget.builder(EditorI18n.tr("button.apply"),b->{
            if(points.isEmpty()){status=EditorI18n.msg("status.draw_empty");return;}
            apply.accept(List.copyOf(points));client.setScreen(parent);
        }).dimensions(x+2*(bw+4),y,bw,20).build());
        addDrawableChild(ButtonWidget.builder(EditorI18n.tr("button.back"),b->close()).dimensions(x+3*(bw+4),y,bw,20).build());
    }
    private boolean inside(double x,double y){return x>=left&&x<left+size&&y>=top&&y<top+size;}
    private void add(double x,double y) {
        if(!inside(x,y)||points.size()>=EffectValidator.MAX_CUSTOM_POINTS)return;
        Vec3 p=new Vec3(Math.round((x-left-size/2.0)*8000/size)/1000.0,
            Math.round((top+size/2.0-y)*8000/size)/1000.0,0);
        if(points.isEmpty()||Math.hypot(points.getLast().x()-p.x(),points.getLast().y()-p.y())>=.08)points.add(p);
    }
    @Override public boolean mouseClicked(Click click,boolean doubled) {
        if(click.button()==0&&inside(click.x(),click.y())){drawing=true;add(click.x(),click.y());return true;}
        return super.mouseClicked(click,doubled);
    }
    @Override public boolean mouseDragged(Click click,double dx,double dy) {
        if(drawing&&click.button()==0){add(click.x(),click.y());return true;}
        return super.mouseDragged(click,dx,dy);
    }
    @Override public boolean mouseReleased(Click click){if(drawing){drawing=false;return true;}return super.mouseReleased(click);}
    @Override public void render(DrawContext ctx,int mouseX,int mouseY,float delta) {
        ctx.fill(left,top,left+size,top+size,0xFF17231F);
        ctx.fill(left+size/2,top,left+size/2+1,top+size,0xFF445D54);
        ctx.fill(left,top+size/2,left+size,top+size/2+1,0xFF445D54);
        for(Vec3 p:points) {
            int x=left+(int)((p.x()+4)*size/8),y=top+(int)((4-p.y())*size/8);
            ctx.fill(x-2,y-2,x+2,y+2,0xFF69E6D0);
        }
        ctx.drawCenteredTextWithShadow(textRenderer,EditorI18n.tr("screen.drawing"),width/2,12,0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer,EditorI18n.tr("label.points",points.size(),EffectValidator.MAX_CUSTOM_POINTS),width/2,height-21,0xFFE0E0E0);
        super.render(ctx,mouseX,mouseY,delta);
        if(!status.isEmpty())ctx.drawCenteredTextWithShadow(textRenderer,status,width/2,height-60,0xFFFFD166);
    }
    @Override public void renderBackground(DrawContext ctx,int mouseX,int mouseY,float delta){ctx.fill(0,0,width,height,0xFF101512);}
    @Override public void close(){client.setScreen(parent);}
    @Override public boolean shouldPause(){return false;}
}
