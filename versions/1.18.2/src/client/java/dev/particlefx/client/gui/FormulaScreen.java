package dev.particlefx.client.gui;

import dev.particlefx.math.Vec3;
import dev.particlefx.storage.EffectValidator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;

public final class FormulaScreen extends Screen {
    private final Screen parent;
    private final Consumer<List<Vec3>> apply;
    private final int maxPoints;
    private TextFieldWidget x,y,z,count;
    private String xs="2*cos(2*pi*t)",ys="sin(4*pi*t)",zs="2*sin(2*pi*t)",countText="128";
    private boolean surface;
    private String status="";
    private int left,w;
    public FormulaScreen(Screen parent,Consumer<List<Vec3>> apply){this(parent,EditorLimits.maxCustomPoints(),apply);}
    public FormulaScreen(Screen parent,int maxPoints,Consumer<List<Vec3>> apply){super(EditorI18n.tr("screen.formula"));this.parent=parent;this.maxPoints=maxPoints;this.apply=apply;}
    @Override protected void init() {
        w=Math.min(460,width-24);left=(width-w)/2;
        int cw=(w-8)/3;
        addDrawableChild(new ButtonWidget(left,32,cw,20,EditorI18n.tr("button.templates"),b->{
            remember();
            var ids=FormulaGenerator.PRESETS.stream().filter(p->p.tier().ordinal()<=EditorI18n.tier().ordinal()).map(FormulaGenerator.Preset::id).toList();
            client.setScreen(new ValuePickerScreen(this,"picker.template_title","template",ids,id->{
                var preset=FormulaGenerator.PRESETS.stream().filter(p->p.id().equals(id)).findFirst().orElseThrow();
                xs=preset.x();ys=preset.y();zs=preset.z();surface=preset.surface();initWidgets();
            }));
        }));
        addDrawableChild(new ButtonWidget(left+cw+4,32,cw,20,EditorI18n.tr("button.surface",surface?EditorI18n.msg("mode.surface"):EditorI18n.msg("mode.curve")),b->{
            remember();surface=!surface;initWidgets();
        }));
        addDrawableChild(new ButtonWidget(left+2*(cw+4),32,cw,20,EditorI18n.tr("button.back"),b->close()));
        x=field(xs,66);y=field(ys,96);z=field(zs,126);count=field(countText,156);
        int bw=(w-4)/2;
        addDrawableChild(new ButtonWidget(left,184,bw,20,EditorI18n.tr("button.apply"),b->{
            try {
                int n=Integer.parseInt(count.getText().trim());
                var points=FormulaGenerator.generate(x.getText(),y.getText(),z.getText(),n,surface,maxPoints);
                apply.accept(points);client.setScreen(parent);
            } catch(Exception ex) {status=EditorI18n.msg("status.invalid_input",Objects.toString(ex.getMessage(),"")); }
        }));
        addDrawableChild(new ButtonWidget(left+bw+4,184,bw,20,EditorI18n.tr("button.clear"),b->{
            xs="t";ys="0";zs="0";countText="128";surface=false;initWidgets();
        }));
    }
    private TextFieldWidget field(String value,int top){
        var field=new TextFieldWidget(textRenderer,left,top,w,20,EditorI18n.tr("screen.formula"));
        field.setMaxLength(160);field.setText(value);addDrawableChild(field);return field;
    }
    private void remember(){if(x!=null){xs=x.getText();ys=y.getText();zs=z.getText();countText=count.getText();}}
    private void initWidgets(){clearChildren();init();}
    @Override public void render(MatrixStack matrices,int mouseX,int mouseY,float delta) {
        textRenderer.drawWithShadow(matrices,EditorI18n.tr("screen.formula").getString(),width/2-textRenderer.getWidth(EditorI18n.tr("screen.formula").getString())/2,12,0xFFFFFFFF);
        String[] labels={"label.formula_x","label.formula_y","label.formula_z","label.formula_count"};
        for(int i=0;i<4;i++)textRenderer.drawWithShadow(matrices,EditorI18n.tr(labels[i]).getString(),left,55+i*30,0xFFE0E0E0);
        super.render(matrices,mouseX,mouseY,delta);
        textRenderer.drawWithShadow(matrices,textRenderer.trimToWidth(EditorI18n.msg("label.formula_help"),w),left,210,0xFFBCCFC6);
        textRenderer.drawWithShadow(matrices,textRenderer.trimToWidth(status,w),left,224,0xFFFFD166);
    }
    @Override public void renderBackground(MatrixStack matrices){fill(matrices,0,0,width,height,0xFF101512);}
    @Override public void close(){client.setScreen(parent);}
    @Override public boolean shouldPause(){return false;}
}
