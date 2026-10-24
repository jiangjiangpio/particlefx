package dev.particlefx.client.gui;

import dev.particlefx.math.Vec3;
import dev.particlefx.storage.EffectValidator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;

public final class FormulaScreen extends Screen {
    private final Screen parent;
    private final Consumer<List<Vec3>> apply;
    private final int maxPoints;
    private EditBox x,y,z,count;
    private String xs="2*cos(2*pi*t)",ys="sin(4*pi*t)",zs="2*sin(2*pi*t)",countText="128";
    private boolean surface;
    private String status="";
    private int left,w;
    public FormulaScreen(Screen parent,Consumer<List<Vec3>> apply){this(parent,EditorLimits.maxCustomPoints(),apply);}
    public FormulaScreen(Screen parent,int maxPoints,Consumer<List<Vec3>> apply){super(EditorI18n.tr("screen.formula"));this.parent=parent;this.maxPoints=maxPoints;this.apply=apply;}
    @Override protected void init() {
        w=Math.min(460,width-24);left=(width-w)/2;
        int cw=(w-8)/3;
        addRenderableWidget(Button.builder(EditorI18n.tr("button.templates"),b->{
            remember();
            var ids=FormulaGenerator.PRESETS.stream().filter(p->p.tier().ordinal()<=EditorI18n.tier().ordinal()).map(FormulaGenerator.Preset::id).toList();
            minecraft.setScreen(new ValuePickerScreen(this,"picker.template_title","template",ids,id->{
                var preset=FormulaGenerator.PRESETS.stream().filter(p->p.id().equals(id)).findFirst().orElseThrow();
                xs=preset.x();ys=preset.y();zs=preset.z();surface=preset.surface();initWidgets();
            }));
        }).bounds(left,32,cw,20).build());
        addRenderableWidget(Button.builder(EditorI18n.tr("button.surface",surface?EditorI18n.msg("mode.surface"):EditorI18n.msg("mode.curve")),b->{
            remember();surface=!surface;initWidgets();
        }).bounds(left+cw+4,32,cw,20).build());
        addRenderableWidget(Button.builder(EditorI18n.tr("button.back"),b->onClose()).bounds(left+2*(cw+4),32,cw,20).build());
        x=field(xs,66);y=field(ys,96);z=field(zs,126);count=field(countText,156);
        int bw=(w-4)/2;
        addRenderableWidget(Button.builder(EditorI18n.tr("button.apply"),b->{
            try {
                int n=Integer.parseInt(count.getValue().trim());
                var points=FormulaGenerator.generate(x.getValue(),y.getValue(),z.getValue(),n,surface,maxPoints);
                apply.accept(points);minecraft.setScreen(parent);
            } catch(Exception ex) {status=EditorI18n.msg("status.invalid_input",Objects.toString(ex.getMessage(),"")); }
        }).bounds(left,184,bw,20).build());
        addRenderableWidget(Button.builder(EditorI18n.tr("button.clear"),b->{
            xs="t";ys="0";zs="0";countText="128";surface=false;initWidgets();
        }).bounds(left+bw+4,184,bw,20).build());
    }
    private EditBox field(String value,int top){
        var field=new EditBox(font,left,top,w,20,EditorI18n.tr("screen.formula"));
        field.setMaxLength(160);field.setValue(value);addRenderableWidget(field);return field;
    }
    private void remember(){if(x!=null){xs=x.getValue();ys=y.getValue();zs=z.getValue();countText=count.getValue();}}
    private void initWidgets(){clearWidgets();init();}
    @Override public void extractRenderState(GuiGraphicsExtractor ctx,int mouseX,int mouseY,float delta) {
        ctx.centeredText(font,EditorI18n.tr("screen.formula"),width/2,12,0xFFFFFFFF);
        String[] labels={"label.formula_x","label.formula_y","label.formula_z","label.formula_count"};
        for(int i=0;i<4;i++)ctx.text(font,EditorI18n.tr(labels[i]),left,55+i*30,0xFFE0E0E0);
        super.extractRenderState(ctx,mouseX,mouseY,delta);
        ctx.text(font,font.plainSubstrByWidth(EditorI18n.msg("label.formula_help"),w),left,210,0xFFBCCFC6);
        ctx.text(font,font.plainSubstrByWidth(status,w),left,224,0xFFFFD166);
    }
    @Override public void extractBackground(GuiGraphicsExtractor ctx,int mouseX,int mouseY,float delta){ctx.fill(0,0,width,height,0xFF101512);}
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}

