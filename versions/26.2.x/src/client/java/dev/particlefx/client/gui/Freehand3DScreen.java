package dev.particlefx.client.gui;

import dev.particlefx.client.preview.LocalPreview;
import dev.particlefx.client.compat.RenderBootstrap;
import dev.particlefx.effect.ParticleLayer;
import dev.particlefx.math.Vec3;
import dev.particlefx.server.CompiledEffect;
import dev.particlefx.storage.JsonSupport;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.EnumMap;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;

import org.lwjgl.glfw.GLFW;

/** Transparent world editor. Native particles are pinned to a bounded sample of the stored points. */
public final class Freehand3DScreen extends Screen {
    private record Display(int index,Particle particle,Vec3 position){}
    private static Freehand3DScreen active;
    private final Screen parent;
    private final Consumer<List<Vec3>> apply;
    private final ParticleLayer layer;
    private final int maxPoints;
    private final Freehand3DModel model;
    private final net.minecraft.world.phys.Vec3 origin;
    private final ClientLevel level;
    private final SimpleParticleType particleType;
    private final Set<Integer> keys=new HashSet<>();
    private final List<Display> visible=new ArrayList<>();
    private final Map<Integer,Display> indexed=new HashMap<>();
    private String status="";
    private boolean changed;
    private int seenSize=-1,previewCursor;
    private long lastCameraUpdate;
    private int panelLeft,samplesThisTick;
    private double pointerX,pointerY;
    private final BoardTransform board;
    private final boolean safe=RenderBootstrap.SAFE;
    private BoardParticleIcon particleIcon;
    private final Map<Freehand3DModel.Tool,Button> tools=new EnumMap<>(Freehand3DModel.Tool.class);
    private final Map<AbstractWidget,Integer> panelControls=new HashMap<>();
    private final EditBox[] numeric=new EditBox[8];
    private BoardDrag boardDrag;
    private int panelScroll;
    private boolean syncing;
    private final BrushGesture gesture=new BrushGesture();
    private final Map<EditorPreferences.Guide,Boolean> guidesEnabled=new EnumMap<>(EditorPreferences.Guide.class);

    public Freehand3DScreen(Screen parent,ParticleLayer source,int maxPoints,Consumer<List<Vec3>> apply) {
        super(EditorI18n.tr("screen.freehand_3d"));
        Minecraft mc=Minecraft.getInstance();
        if(mc.level==null||mc.player==null)throw new IllegalArgumentException(EditorI18n.msg("status.brush_world_missing"));
        if(source.scale.x()==0||source.scale.y()==0||source.scale.z()==0)
            throw new IllegalArgumentException(EditorI18n.msg("status.brush_zero_scale"));
        this.parent=parent;this.apply=apply;this.maxPoints=maxPoints;
        layer=JsonSupport.GSON.fromJson(JsonSupport.GSON.toJson(source),ParticleLayer.class);
        level=mc.level;origin=mc.player.position();particleType=CompiledEffect.particle(layer.particle);
        net.minecraft.world.phys.Vec3 eye=mc.player.getEyePosition().subtract(origin);
        model=new Freehand3DModel(layer.shape.type.equalsIgnoreCase("custom")?layer.shape.vertices:List.of(),maxPoints,layer,
            new Vec3(eye.x,eye.y,eye.z),mc.player.getYRot(),mc.player.getXRot());
        model.setBaseFov(mc.options.fov().get());
        model.setTool(Freehand3DModel.Tool.OBSERVE);
        board=new BoardTransform(model.cursor(),model.right(),model.up(),model.forward(),8,8);
        for(EditorPreferences.Guide guide:EditorPreferences.Guide.values())guidesEnabled.put(guide,EditorI18n.guideEnabled(guide));
    }
    public static Freehand3DScreen active() {
        Minecraft mc=Minecraft.getInstance();
        return active!=null&&!active.safe&&mc.gui.screen()==active&&mc.level==active.level&&mc.player!=null?active:null;
    }
    public net.minecraft.world.phys.Vec3 cameraPosition(){Vec3 p=model.camera();return origin.add(p.x(),p.y(),p.z());}
    public float cameraYaw(){return model.yaw();}
    public float cameraPitch(){return model.pitch();}
    public float cameraFov(){return (float)model.fov();}
    @Override protected void init() {
        LocalPreview.stop();active=this;lastCameraUpdate=System.nanoTime();keys.clear();clearMouseButtons();stopGesture();
        tools.clear();panelControls.clear();
        int panelWidth=Math.min(184,Math.max(112,width/3));panelLeft=width-panelWidth-8;
        int half=(panelWidth-12)/2,third=(panelWidth-16)/3;
        Freehand3DModel.Tool[] modes={Freehand3DModel.Tool.OBSERVE,Freehand3DModel.Tool.BRUSH,Freehand3DModel.Tool.TRANSLATE,Freehand3DModel.Tool.ROTATE};
        String[] labels={"mode.brush_observe","mode.brush_paint","mode.board_translate","mode.board_rotate"};
        for(int i=0;i<modes.length;i++) {
            Freehand3DModel.Tool mode=modes[i];
            Button button=Button.builder(net.minecraft.network.chat.Component.literal(font.plainSubstrByWidth(EditorI18n.msg(labels[i]),half-8)),b->setTool(mode))
                .bounds(panelLeft+4+(i%2)*(half+4),68+(i/2)*24,half,20).build();
            button.setTooltip(Tooltip.create(EditorI18n.tr(labels[i])));
            tools.put(mode,button);addPanel(button);
        }
        for(int i=0;i<8;i++) {
            int index=i;
            int x,y,w;
            if(i<2){x=panelLeft+4+i*(half+4);y=132;w=half;}
            else {int component=(i-2)%3;int row=(i-2)/3;x=panelLeft+4+component*(third+4);y=178+row*46;w=third;}
            numeric[i]=new EditBox(font,x,y,w,20,EditorI18n.tr(i<2?"label.board_size":i<5?"label.board_position":"label.board_rotation"));
            numeric[i].setMaxLength(12);numeric[i].setResponder(value->editNumeric(index));addPanel(numeric[i]);
        }
        syncNumeric();refreshTools();
        addPanel(Button.builder(EditorI18n.tr("button.board_reset"),b->{stopGesture();board.reset();syncNumeric();})
            .bounds(panelLeft+4,254,panelWidth-8,20).build());
        addPanel(Button.builder(EditorI18n.tr("button.view_reset"),b->{stopGesture();keys.clear();model.resetCamera();})
            .bounds(panelLeft+4,278,panelWidth-8,20).build());
        addPanel(guideToggle(EditorI18n.tr("label.brush_axes"),EditorPreferences.Guide.AXES,306));
        addPanel(guideToggle(EditorI18n.tr("label.brush_ground_grid"),EditorPreferences.Guide.GROUND,330));
        addPanel(guideToggle(EditorI18n.tr("label.brush_plane_grid"),EditorPreferences.Guide.PLANE,354));
        addPanel(guideToggle(EditorI18n.tr("label.brush_axis_x"),EditorPreferences.Guide.X,390));
        addPanel(guideToggle(EditorI18n.tr("label.brush_axis_y"),EditorPreferences.Guide.Y,414));
        addPanel(guideToggle(EditorI18n.tr("label.brush_axis_z"),EditorPreferences.Guide.Z,438));
        addPanel(guideToggle(EditorI18n.tr("label.brush_origin"),EditorPreferences.Guide.ORIGIN,462));
        addPanel(Button.builder(EditorI18n.tr("button.view_origin"),b->{stopGesture();keys.clear();model.faceOrigin();})
            .bounds(panelLeft+4,486,panelWidth-8,20).build());
        if(safe)particleIcon=new BoardParticleIcon(minecraft,layer.particle);
        positionPanel();
        int bw=Math.min(80,(width-32)/5),x=(width-(bw*5+16))/2,y=height-28;
        addRenderableWidget(Button.builder(EditorI18n.tr("button.undo"),b->{model.undo();changed=true;clearPreview();}).bounds(x,y,bw,20).build());
        addRenderableWidget(Button.builder(EditorI18n.tr("button.clear"),b->{model.clear();changed=true;clearPreview();}).bounds(x+bw+4,y,bw,20).build());
        addRenderableWidget(Button.builder(EditorI18n.tr("button.brush_reset"),b->{stopGesture();keys.clear();model.resetCamera();}).bounds(x+2*(bw+4),y,bw,20).build());
        addRenderableWidget(Button.builder(EditorI18n.tr("button.apply"),b->save()).bounds(x+3*(bw+4),y,bw,20).build());
        addRenderableWidget(Button.builder(EditorI18n.tr("button.back"),b->onClose()).bounds(x+4*(bw+4),y,bw,20).build());
    }
    private <T extends AbstractWidget> T addPanel(T widget) {
        panelControls.put(widget,widget.getY());return addRenderableWidget(widget);
    }
    private void positionPanel() {
        panelScroll=Math.max(0,Math.min(Math.max(0,510-(height-36)),panelScroll));
        panelControls.forEach((widget,y)->{widget.setY(y-panelScroll);widget.visible=widget.getY()>=54&&widget.getY()+widget.getHeight()<=height-36;});
        if(inputFocused()&&numericInputHidden()){setFocused(null);keys.clear();stopGesture();}
    }
    private boolean numericInputHidden(){for(var input:numeric)if(input!=null&&input.isFocused()&&!input.visible)return true;return false;}
    private boolean inputFocused(){for(var input:numeric)if(input!=null&&input.isFocused())return true;return false;}
    private void editNumeric(int index) {
        if(syncing)return;
        stopGesture();keys.clear();
        try {
            if(index<2)board.setSize(Double.parseDouble(numeric[0].getValue()),Double.parseDouble(numeric[1].getValue()));
            else if(index<5)board.setCenter(new Vec3(Double.parseDouble(numeric[2].getValue()),Double.parseDouble(numeric[3].getValue()),Double.parseDouble(numeric[4].getValue())));
            else board.setRotation(Double.parseDouble(numeric[5].getValue()),Double.parseDouble(numeric[6].getValue()),Double.parseDouble(numeric[7].getValue()));
            status="";
        }catch(IllegalArgumentException e){status=EditorI18n.msg("status.board_invalid");}
    }
    private void syncNumeric() {
        syncing=true;
        try {
            double[] values={board.width(),board.height(),board.center().x(),board.center().y(),board.center().z(),board.rotationX(),board.rotationY(),board.rotationZ()};
            for(int i=0;i<values.length;i++)if(numeric[i]!=null)numeric[i].setValue(String.format(java.util.Locale.ROOT,"%.3f",values[i]));
        }finally{syncing=false;}
    }
    private Checkbox guideToggle(net.minecraft.network.chat.Component label,EditorPreferences.Guide guide,int y) {
        return Checkbox.builder(label,font).pos(panelLeft+4,y)
            .selected(guidesEnabled.get(guide)).maxWidth(Math.max(1,width-panelLeft-16))
            .onValueChange((box,selected)->{
                keys.clear();stopGesture();guidesEnabled.put(guide,selected);
                status=EditorI18n.setGuideEnabled(guide,selected)?"":EditorI18n.msg("status.brush_preferences_failed");
            }).build();
    }
    private void stopGesture(){gesture.stop();boardDrag=null;model.endStroke();}
    private void clearMouseButtons(){gesture.resetInput();}
    private void setTool(Freehand3DModel.Tool tool) {
        updateCamera();keys.clear();stopGesture();model.setTool(tool);setFocused(null);refreshTools();
    }
    private void refreshTools(){tools.forEach((tool,button)->button.active=model.tool()!=tool);}
    private boolean canvas(double x,double y){return x>=0&&x<panelLeft&&y>=48&&y<height-36;}
    private void updatePointer() {
        pointerX=minecraft.mouseHandler.getScaledXPos(minecraft.getWindow());
        pointerY=minecraft.mouseHandler.getScaledYPos(minecraft.getWindow());
    }
    private void pollGesture() {
        updatePointer();
        boolean valid;
        if(safe)valid=gesture.checkEvents(minecraft.isWindowActive(),canvas(pointerX,pointerY),inputFocused());
        else {
            boolean left=GLFW.glfwGetMouseButton(minecraft.getWindow().handle(),GLFW.GLFW_MOUSE_BUTTON_LEFT)==GLFW.GLFW_PRESS;
            boolean right=GLFW.glfwGetMouseButton(minecraft.getWindow().handle(),GLFW.GLFW_MOUSE_BUTTON_RIGHT)==GLFW.GLFW_PRESS;
            valid=gesture.check(minecraft.isWindowActive(),canvas(pointerX,pointerY),inputFocused(),left,right);
        }
        if(!valid){boardDrag=null;model.endStroke();}
    }
    private void samplePointer(){samplePointer(false);}
    private void samplePointer(boolean finishing) {
        if(!model.drawing()||!canvas(pointerX,pointerY)||samplesThisTick>=(finishing?8:7))return;
        int size=model.size();samplesThisTick++;
        double aspect=(double)minecraft.getWindow().getWidth()/Math.max(1,minecraft.getWindow().getHeight());
        BoardTransform.Hit hit=board.intersect(model.camera(),model.rayFromScreen(pointerX,pointerY,width,height,aspect));
        if(hit==null||model.project(hit.point(),width,height,aspect)==null) {
            model.breakStrokePath();status=EditorI18n.msg("status.board_miss");return;
        }
        Freehand3DModel.Result result=model.sampleWorld(hit.point());
        changed|=model.size()!=size;
        status=result==Freehand3DModel.Result.LIMIT?EditorI18n.msg("status.brush_limit"):
            result==Freehand3DModel.Result.OUTSIDE?EditorI18n.msg("status.brush_bounds"):"";
    }
    private void save() {
        stopGesture();keys.clear();
        if(model.size()==0){status=EditorI18n.msg("status.draw_empty");return;}
        apply.accept(model.points());changed=false;minecraft.setScreenAndShow(parent);
    }
    @Override public void tick() {
        if(minecraft.level!=level||minecraft.player==null||!minecraft.player.isAlive()){minecraft.setScreenAndShow(parent);return;}
        updateCamera();
        if(!minecraft.isWindowActive())return;
        samplesThisTick=0;samplePointer();
        if(!safe)previewParticles();
    }
    // Shared by render, ticks and input events: each elapsed interval is integrated only once.
    public void updateCamera() {
        long now=System.nanoTime(),elapsed=now-lastCameraUpdate;
        lastCameraUpdate=now;
        if(!minecraft.isWindowActive()||elapsed>500_000_000L){keys.clear();clearMouseButtons();stopGesture();return;}
        pollGesture();
        if(!safe)keys.removeIf(key->GLFW.glfwGetKey(minecraft.getWindow().handle(),key)!=GLFW.GLFW_PRESS);
        if(inputFocused()){keys.clear();stopGesture();return;}
        model.move(axis(GLFW.GLFW_KEY_W,GLFW.GLFW_KEY_S),axis(GLFW.GLFW_KEY_D,GLFW.GLFW_KEY_A),
            axis(GLFW.GLFW_KEY_SPACE,GLFW.GLFW_KEY_LEFT_SHIFT),keys.contains(GLFW.GLFW_KEY_LEFT_CONTROL),elapsed/1_000_000_000.0);
    }
    private int axis(int positive,int negative){return (keys.contains(positive)?1:0)-(keys.contains(negative)?1:0);}
    private void previewParticles() {
        // Keep native lifespan and texture, but stop motion so the saved location stays inspectable.
        if(model.size()<seenSize)clearPreview();
        boolean appended=model.size()>seenSize;
        seenSize=model.size();
        visible.removeIf(display->{if(display.particle().isAlive())return false;indexed.remove(display.index());return true;});
        for(Display display:visible)pin(display);
        int samples=Math.min(512,model.size()),created=0;
        if(samples==0)return;
        if(appended&&model.drawing()) {
            int index=model.size()-1;
            if(!indexed.containsKey(index)) {
                if(visible.size()>=512){Display old=visible.removeFirst();old.particle().remove();indexed.remove(old.index());}
                createDisplay(index);created++;
            }
        }
        for(int attempt=0;attempt<Math.min(samples,64)&&created<64;attempt++) {
            int index=(int)((long)(previewCursor++%samples)*model.size()/samples);
            if(indexed.containsKey(index))continue;
            if(visible.size()>=512)break;
            createDisplay(index);
            created++;
        }
    }
    private void createDisplay(int index) {
        Vec3 p=model.displayPoint(index);net.minecraft.world.phys.Vec3 pos=origin.add(p.x(),p.y(),p.z());
        Particle particle=minecraft.particleEngine.createParticle(particleType,pos.x,pos.y,pos.z,0,0,0);
        if(particle!=null){particle.setLifetime(Math.min(40,particle.getLifetime()));Display display=new Display(index,particle,p);visible.add(display);indexed.put(index,display);pin(display);}
    }
    private void pin(Display display) {
        Vec3 p=display.position();
        display.particle().setPos(origin.x+p.x(),origin.y+p.y(),origin.z+p.z());
        display.particle().setParticleSpeed(0,0,0);
    }
    private void clearPreview(){for(Display display:visible)display.particle().remove();visible.clear();indexed.clear();previewCursor=0;seenSize=-1;}
    @Override public boolean keyPressed(KeyEvent input) {
        updateCamera();
        int key=input.key();
        if(key==GLFW.GLFW_KEY_ESCAPE){onClose();return true;}
        if(inputFocused()) {
            if(key==GLFW.GLFW_KEY_ENTER){setFocused(null);return true;}
            return super.keyPressed(input);
        }
        if(key==GLFW.GLFW_KEY_ENTER){save();return true;}
        if(key==GLFW.GLFW_KEY_Z&&(input.modifiers()&GLFW.GLFW_MOD_CONTROL)!=0){stopGesture();keys.clear();model.undo();changed=true;clearPreview();return true;}
        if(key==GLFW.GLFW_KEY_R){stopGesture();keys.clear();model.resetCamera();return true;}
        if(isCameraKey(key)){stopGesture();keys.add(key);return true;}
        return super.keyPressed(input);
    }
    private static boolean isCameraKey(int key) {
        return key==GLFW.GLFW_KEY_W||key==GLFW.GLFW_KEY_A||key==GLFW.GLFW_KEY_S||
            key==GLFW.GLFW_KEY_D||key==GLFW.GLFW_KEY_SPACE||key==GLFW.GLFW_KEY_LEFT_SHIFT||key==GLFW.GLFW_KEY_LEFT_CONTROL;
    }
    @Override public boolean keyReleased(KeyEvent input) {
        updateCamera();
        keys.remove(input.key());return super.keyReleased(input);
    }
    @Override public boolean mouseClicked(MouseButtonEvent click,boolean doubled) {
        updateCamera();
        if(!canvas(click.x(),click.y())){keys.clear();clearMouseButtons();stopGesture();return super.mouseClicked(click,doubled);}
        setFocused(null);pointerX=click.x();pointerY=click.y();
        if(click.button()!=0&&click.button()!=1)return super.mouseClicked(click,doubled);
        stopGesture();
        if(safe) {
            if(!gesture.eventPress(click.button(),click.x(),click.y()))return true;
        } else gesture.begin(click.button(),click.x(),click.y());
        if(model.tool()==Freehand3DModel.Tool.BRUSH&&click.button()!=0){stopGesture();return true;}
        // GUI events are authoritative in safe mode; polling never starts a gesture.
        pointerX=click.x();pointerY=click.y();
        if(model.tool()==Freehand3DModel.Tool.BRUSH){model.beginStroke();samplePointer();}
        else if(model.tool()==Freehand3DModel.Tool.TRANSLATE||model.tool()==Freehand3DModel.Tool.ROTATE) {
            if(click.button()!=0){stopGesture();return true;}
            double aspect=aspect();
            boolean rotation=model.tool()==Freehand3DModel.Tool.ROTATE;
            BoardGizmo.Hit hit=rotation?BoardGizmo.pickRing(BoardGizmo.rings(model,board,width,height,aspect,projection()),click.x(),click.y())
                :BoardGizmo.pickAxis(BoardGizmo.axes(model,board,width,height,aspect,projection()),click.x(),click.y());
            if(hit!=null)boardDrag=BoardDrag.begin(board,model,hit.axis(),rotation,model.rayFromScreen(click.x(),click.y(),width,height,aspect));
            if(boardDrag==null){status=EditorI18n.msg("status.board_drag_degenerate");stopGesture();}
        }
        return true;
    }
    @Override public boolean mouseDragged(MouseButtonEvent click,double dx,double dy) {
        if(gesture.button()!=click.button())return super.mouseDragged(click,dx,dy);
        updateCamera();pointerX=click.x();pointerY=click.y();
        BrushGesture.Delta delta=gesture.drag(click.button(),pointerX,pointerY,width,height);
        if(delta==null){stopGesture();return true;}
        if(!canvas(pointerX,pointerY)){stopGesture();return true;}
        if(model.tool()==Freehand3DModel.Tool.BRUSH)samplePointer();
        else if(model.tool()==Freehand3DModel.Tool.OBSERVE) {
            if(!keys.isEmpty()){stopGesture();return true;}
            if(click.button()==0)model.orbitAround(board.center(),delta.x(),delta.y());
            else model.look(delta.x(),delta.y());
        } else if(boardDrag!=null) {
            if(boardDrag.update(model,model.rayFromScreen(pointerX,pointerY,width,height,aspect()))) {syncNumeric();status="";}
            else status=EditorI18n.msg("status.board_drag_degenerate");
        }
        return true;
    }
    @Override public boolean mouseReleased(MouseButtonEvent click) {
        boolean handled=gesture.button()==click.button();
        if(handled){pointerX=click.x();pointerY=click.y();samplePointer(true);}
        gesture.eventRelease(click.button());stopGesture();
        return handled||super.mouseReleased(click);
    }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical) {
        stopGesture();
        if(x>=panelLeft&&y>=54&&y<height-36){keys.clear();panelScroll-=(int)(vertical*28);positionPanel();return true;}
        if(!canvas(x,y))return super.mouseScrolled(x,y,horizontal,vertical);
        updateCamera();stopGesture();model.zoom(vertical);return true;
    }
    @Override public void extractRenderState(GuiGraphicsExtractor ctx,int mouseX,int mouseY,float delta) {
        updateCamera();
        if(safe)ctx.fill(0,0,width,height,0xFF202426);
        int w=Math.max(1,width-16);
        ctx.fill(0,0,width,48,0x88000000);
        ctx.centeredText(font,title,width/2,6,0xFFFFFFFF);
        ctx.text(font,font.plainSubstrByWidth(EditorI18n.msg("label.board_state",
            EditorI18n.name("particle",layer.particle),model.size(),maxPoints,EditorI18n.msg(safe
                ?particleIcon!=null&&particleIcon.available()?"label.board_safe":"label.board_safe_fallback":"label.board_world")),w),8,20,0xFFFFFFFF);
        ctx.text(font,font.plainSubstrByWidth(EditorI18n.msg("label.brush_camera",
            String.format(java.util.Locale.ROOT,"%.1f / %.1f / %.1f",model.camera().x(),model.camera().y(),model.camera().z())),w),8,34,0xFFCCCCCC);
        drawGuides(ctx);
        if(safe)drawStaticPreview(ctx);
        ctx.enableScissor(0,48,panelLeft,height-36);
        try {drawGizmo(ctx);}finally{ctx.disableScissor();}
        ctx.fill(panelLeft,54,width-4,height-36,0xAA101512);
        drawPanelLabels(ctx);
        int cx=model.tool()==Freehand3DModel.Tool.BRUSH?mouseX:width/2,cy=model.tool()==Freehand3DModel.Tool.BRUSH?mouseY:height/2;
        int color=model.drawing()?0xFFFFD166:0xFFFFFFFF;
        if(canvas(cx,cy)){ctx.fill(cx-4,cy,cx+5,cy+1,color);ctx.fill(cx,cy-4,cx+1,cy+5,color);}
        if(!status.isEmpty())ctx.text(font,font.plainSubstrByWidth(status,Math.max(1,panelLeft-16)),8,height-44,0xFFFFD166);
        super.extractRenderState(ctx,mouseX,mouseY,delta);
    }
    private void drawGuides(GuiGraphicsExtractor ctx) {
        double aspect=(double)minecraft.getWindow().getWidth()/Math.max(1,minecraft.getWindow().getHeight());
        BrushGuides guides=new BrushGuides(model,width,height,aspect,panelLeft,48,height-36);
        ctx.enableScissor(0,48,panelLeft,height-36);
        try {
            Vec3[] corners=board.corners();
            for(BrushGuides.Span span:guides.spans(guides.polygon(List.of(corners))))ctx.fill(span.start(),span.y(),span.end(),span.y()+1,0x40FFFFFF);
            for(int i=0;i<4;i++)drawGuideLine(ctx,guides.line(corners[i],corners[(i+1)%4]),0xDDFFFFFF,1);
            if(guidesEnabled.get(EditorPreferences.Guide.GROUND))
                for(BrushGuides.Segment segment:BrushGuides.ground())drawGuideLine(ctx,guides.line(segment.start(),segment.end()),
                    segment.major()?0x887F9BA6:0x384A6068,segment.major()?2:1);
            if(guidesEnabled.get(EditorPreferences.Guide.PLANE)) {
                for(BrushGuides.Segment segment:BrushGuides.boardGrid(board))drawGuideLine(ctx,guides.line(segment.start(),segment.end()),
                    segment.major()?0x99404040:0x44505050,segment.major()?2:1);
            }
            if(guidesEnabled.get(EditorPreferences.Guide.AXES))drawAxes(ctx,guides);
            if(guidesEnabled.get(EditorPreferences.Guide.ORIGIN)) {
                drawMarker(ctx,guides,Vec3.ZERO,EditorI18n.msg("label.brush_origin"),0xFFFFFFFF);
                if(!layer.offset.equals(Vec3.ZERO))
                    drawMarker(ctx,guides,layer.offset,EditorI18n.msg("label.brush_layer_origin"),0xFFFFD166);
            }
        } finally {ctx.disableScissor();}
    }
    private double aspect(){return (double)minecraft.getWindow().getWidth()/Math.max(1,minecraft.getWindow().getHeight());}
    private BrushGuides projection(){return new BrushGuides(model,width,height,aspect(),panelLeft,48,height-36);}
    private void drawStaticPreview(GuiGraphicsExtractor ctx) {
        if(particleIcon==null)return;
        ctx.enableScissor(0,48,panelLeft,height-36);
        try {
            int samples=Math.min(512,model.size());
            for(int i=0;i<samples;i++) {
                Vec3 point=model.displayPoint((int)((long)i*model.size()/samples));
                var p=model.project(point,width,height,aspect());
                if(p!=null&&canvas(p.x(),p.y()))particleIcon.draw(ctx,(int)Math.round(p.x()),(int)Math.round(p.y()));
            }
        }finally{ctx.disableScissor();}
    }
    private void drawPanelLabels(GuiGraphicsExtractor ctx) {
        ctx.enableScissor(panelLeft,54,width-4,height-36);
        try {
            panelLabel(ctx,"label.brush_tools",56);
            panelLabel(ctx,"label.board_size",120);
            panelLabel(ctx,"label.board_position",160);
            panelLabel(ctx,"label.board_rotation",206);
            panelLabel(ctx,"label.board_dimensions",378);
        }finally{ctx.disableScissor();}
    }
    private void panelLabel(GuiGraphicsExtractor ctx,String key,int y) {
        ctx.text(font,font.plainSubstrByWidth(EditorI18n.msg(key),width-panelLeft-12),panelLeft+4,y-panelScroll,0xFFDDDDDD);
    }
    private void drawGizmo(GuiGraphicsExtractor ctx) {
        double aspect=aspect();
        if(model.tool()==Freehand3DModel.Tool.TRANSLATE) {
            var axes=BoardGizmo.axes(model,board,width,height,aspect,projection());
            var hover=canvas(pointerX,pointerY)?BoardGizmo.pickAxis(axes,pointerX,pointerY):null;
            for(var segment:axes) {
                int color=gizmoColor(segment.axis(),hover);
                drawProjected(ctx,segment,color);
                var a=segment.start();var b=segment.end();
                double dx=b.x()-a.x(),dy=b.y()-a.y(),length=Math.hypot(dx,dy);
                if(length>=4)for(int sign:new int[]{-1,1})
                    drawProjected(ctx,new BoardGizmo.Segment(b,new BoardGizmo.Projected(b.x()-dx/length*7-dy/length*4*sign,
                        b.y()-dy/length*7+dx/length*4*sign),segment.axis()),color);
                drawGuideLabel(ctx,axisLabel(segment.axis()),(int)b.x()+5,(int)b.y(),color);
            }
        } else if(model.tool()==Freehand3DModel.Tool.ROTATE) {
            var rings=BoardGizmo.rings(model,board,width,height,aspect,projection());
            var hover=canvas(pointerX,pointerY)?BoardGizmo.pickRing(rings,pointerX,pointerY):null;
            for(var ring:rings) {
                int color=gizmoColor(ring.axis(),hover);
                for(var segment:ring.segments())drawProjected(ctx,segment,color);
                if(!ring.segments().isEmpty()) {
                    int labelIndex=switch(ring.axis()){case X->6;case Y->16;case Z->36;};
                    var tip=ring.segments().get(Math.min(labelIndex,ring.segments().size()-1)).start();
                    drawGuideLabel(ctx,axisLabel(ring.axis()),(int)tip.x()+4,(int)tip.y()-4,color);
                }
            }
        }
    }
    private String axisLabel(BoardTransform.Axis axis) {
        return EditorI18n.msg(switch(axis){case X->"label.brush_axis_x";case Y->"label.brush_axis_y";case Z->"label.brush_axis_z";});
    }
    private int gizmoColor(BoardTransform.Axis axis,BoardGizmo.Hit hover) {
        if(boardDrag!=null?boardDrag.axis()==axis:hover!=null&&hover.axis()==axis)return 0xFFFFD166;
        return switch(axis){case X->0xFFFF4D5A;case Y->0xFF55E878;case Z->0xFF5C9DFF;};
    }
    private void drawProjected(GuiGraphicsExtractor ctx,BoardGizmo.Segment segment,int color) {
        drawGuideLine(ctx,new BrushGuides.Line(new BrushGuides.Point(segment.start().x(),segment.start().y()),new BrushGuides.Point(segment.end().x(),segment.end().y())),color,2);
    }
    private void drawAxes(GuiGraphicsExtractor ctx,BrushGuides guides) {
        if(guidesEnabled.get(EditorPreferences.Guide.X))drawAxis(ctx,guides,new Vec3(4,0,0),0xFFFF4D5A,EditorI18n.msg("label.brush_axis_x"));
        if(guidesEnabled.get(EditorPreferences.Guide.Y))drawAxis(ctx,guides,new Vec3(0,4,0),0xFF55E878,EditorI18n.msg("label.brush_axis_y"));
        if(guidesEnabled.get(EditorPreferences.Guide.Z))drawAxis(ctx,guides,new Vec3(0,0,4),0xFF5C9DFF,EditorI18n.msg("label.brush_axis_z"));
    }
    private void drawMarker(GuiGraphicsExtractor ctx,BrushGuides guides,Vec3 position,String label,int color) {
        BrushGuides.Point p=guides.point(position);
        if(p==null)return;
        int x=(int)Math.round(p.x()),y=(int)Math.round(p.y());
        ctx.outline(x-3,y-3,7,7,color);
        drawGuideLabel(ctx,label,x+5,y+5,color);
    }
    private void drawAxis(GuiGraphicsExtractor ctx,BrushGuides guides,Vec3 endpoint,int color,String label) {
        BrushGuides.Line line=guides.line(Vec3.ZERO,endpoint);
        if(line==null)return;
        drawGuideLine(ctx,line,color,2);
        BrushGuides.Point tip=line.end();
        double dx=tip.x()-line.start().x(),dy=tip.y()-line.start().y(),length=Math.hypot(dx,dy);
        // Screen-sized arrowheads remain readable even when the positive axis end is clipped.
        if(length>1) {
            double size=Math.min(6,length*.4),ux=dx/length,uy=dy/length;
            for(int sign:new int[]{-1,1})drawGuideLine(ctx,new BrushGuides.Line(tip,
                new BrushGuides.Point(tip.x()-ux*size-uy*size*.5*sign,tip.y()-uy*size+ux*size*.5*sign)),color,2);
        }
        int x=(int)Math.round(tip.x()),y=(int)Math.round(tip.y());
        drawGuideLabel(ctx,label,x+4,y-5,color);
    }
    private void drawGuideLabel(GuiGraphicsExtractor ctx,String label,int x,int y,int color) {
        String fitted=font.plainSubstrByWidth(label,Math.max(1,panelLeft-8));
        x=Math.max(4,Math.min(panelLeft-font.width(fitted)-4,x));
        y=Math.max(50,Math.min(height-38-font.lineHeight,y));
        ctx.text(font,fitted,x,y,color);
    }
    private void drawGuideLine(GuiGraphicsExtractor ctx,BrushGuides.Line line,int color,int thickness) {
        if(line==null)return;
        double dx=line.end().x()-line.start().x(),dy=line.end().y()-line.start().y();
        var matrices=ctx.pose();
        matrices.pushMatrix();
        try {
            matrices.translate((float)line.start().x(),(float)line.start().y());
            matrices.rotate((float)Math.atan2(dy,dx));
            ctx.fill(0,-thickness/2,(int)Math.ceil(Math.hypot(dx,dy)),thickness-thickness/2,color);
        } finally {matrices.popMatrix();}
    }
    @Override public void extractBackground(GuiGraphicsExtractor ctx,int mouseX,int mouseY,float delta){}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void removed(){if(active==this)active=null;keys.clear();clearMouseButtons();stopGesture();clearPreview();}
    @Override public void onClose() {
        keys.clear();stopGesture();
        if(!changed){minecraft.setScreenAndShow(parent);return;}
        minecraft.setScreenAndShow(new ConfirmScreen(yes->minecraft.setScreenAndShow(yes?parent:this),
            EditorI18n.tr("confirm.brush_discard"),EditorI18n.tr("confirm.discard.body",title.getString())));
    }
}


