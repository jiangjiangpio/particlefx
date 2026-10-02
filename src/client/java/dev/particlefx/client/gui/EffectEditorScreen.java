package dev.particlefx.client.gui;
import dev.particlefx.animation.Animation;
import dev.particlefx.client.preview.*;
import dev.particlefx.effect.*;
import dev.particlefx.math.Vec3;
import dev.particlefx.network.*;
import dev.particlefx.shape.ShapeRegistry;
import dev.particlefx.storage.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

/** Native, paginated widgets keep the editor usable at Minecraft's minimum GUI size. */
public final class EffectEditorScreen extends Screen {
    private record Input(String label,String value,Consumer<String> write){}
    private record Field(Input input,TextFieldWidget widget){}
    private final Screen parent;
    private ParticleEffect effect;
    private List<String> names=new ArrayList<>();
    private final List<Field> fields=new ArrayList<>();
    private int tab,layer,page;
    private boolean requested,dirty;
    private String status="";
    private PreviewModel preview;
    private double zoom=24,yaw=.6;
    private int left,panelWidth,bodyBottom;
    private static final int[] COLORS={0xFF69E6D0,0xFFFFD166,0xFFFF7A90,0xFF93B6FF,0xFFE0D9FF};
    public EffectEditorScreen(Screen parent,ParticleEffect initial) {
        super(tr("screen.title"));this.parent=parent;this.effect=initial==null?example():JsonSupport.copy(initial);
    }
    private static ParticleEffect example(){
        try(var in=EffectEditorScreen.class.getResourceAsStream("/data/particlefx/presets/magic_circle.json")){
            if(in!=null)return JsonSupport.GSON.fromJson(new String(in.readAllBytes(),StandardCharsets.UTF_8),ParticleEffect.class);
        }catch(Exception ignored){}return new ParticleEffect();
    }
    protected void init(){
        fields.clear();panelWidth=Math.min(720,width-20);left=(width-panelWidth)/2;bodyBottom=height-88;
        String[] tabs={"tab.effects","tab.layers","tab.parameters","tab.tools","tab.preview","tab.groups","tab.settings"};
        int tw=(panelWidth-18)/7;
        for(int i=0;i<tabs.length;i++){int t=i;
            button(tabs[i],left+i*(tw+3),30,tw,()->{if(commit()){
                if(t==5)client.setScreen(new GroupListScreen(this));
                else if(t==6)client.setScreen(new EditorSettingsScreen(this));
                else {tab=t;page=0;refresh();}
            }}).active=tab!=i;
        }
        switch(tab){case 0->effectList();case 1->layerList();case 2->parameters();case 3->toolsPane();case 4->previewPane();default->throw new IllegalStateException();}
        int bw=(panelWidth-10)/3,y=height-68;
        button("button.save_server",left,y,bw,()->action(()->{if(commit())request("save",effect.name,JsonSupport.GSON.toJson(effect));})).active=connected();
        button("button.play_server",left+bw+5,y,bw,()->action(()->{if(commit())request("play",effect.name,"");})).active=connected();
        button("button.stop_server",left+2*(bw+5),y,bw,()->request("stop",effect.name,"")).active=connected();
        button("button.local_preview",left,y+23,bw,()->action(()->{if(commit()){LocalPreview.start(effect);client.setScreen(null);}}));
        button("button.delete",left+bw+5,y+23,bw,()->client.setScreen(new ConfirmScreen(yes->{client.setScreen(this);if(yes)request("delete",effect.name,"");},tr("confirm.delete.title",effect.name),tr("confirm.delete.body")))).active=connected();
        button("button.back",left+2*(bw+5),y+23,bw,this::close);
        if(!requested){requested=true;if(connected())request("list","","");else status=msg("status.local_mode");}
    }
    private void effectList(){
        int controls=4,cw=(panelWidth-(controls-1)*4)/controls;
        button("button.refresh",left,56,cw,()->request("list","","")).active=connected();
        button("button.new",left+cw+4,56,cw,()->discardThen(()->{effect=new ParticleEffect();effect.name="new_effect";layer=0;tab=2;page=0;dirty=true;refresh();}));
        button("button.import_json",left+2*(cw+4),56,cw,()->discardThen(()->action(()->{
            String json=client.keyboard.getClipboard();EffectValidator.require(json.getBytes(StandardCharsets.UTF_8).length<=EffectValidator.MAX_JSON_BYTES,"JSON exceeds 30000 bytes");
            ParticleEffect imported=JsonSupport.GSON.fromJson(json,ParticleEffect.class);EffectValidator.validate(imported);
            effect=imported;layer=0;dirty=true;tab=2;page=0;refresh();
        })));
        button("button.copy_json",left+3*(cw+4),56,cw,()->action(()->{if(commit()){client.keyboard.setClipboard(JsonSupport.GSON.toJson(effect));status=msg("status.copied_json");}}));
        int count=Math.max(1,(bodyBottom-112)/23);page=Math.min(page,Math.max(0,(names.size()-1)/count));
        for(int i=0;i<count&&page*count+i<names.size();i++){String name=names.get(page*count+i);
            button(Text.literal(name),left,84+i*23,panelWidth,()->discardThen(()->request("get",name,"")));
        }
        pager(count,names.size(),bodyBottom-24);
    }
    private void layerList(){
        int cw=(panelWidth-8)/3;
        button("button.add_layer",left,56,cw,()->{if(effect.layers.size()<32){effect.layers.add(new ParticleLayer());layer=effect.layers.size()-1;dirty=true;tab=2;page=0;refresh();}else status=msg("status.max_layers");});
        button("button.duplicate",left+cw+4,56,cw,()->{if(effect.layers.size()<32){ParticleLayer copy=JsonSupport.GSON.fromJson(JsonSupport.GSON.toJson(selected()),ParticleLayer.class);effect.layers.add(copy);layer=effect.layers.size()-1;dirty=true;refresh();}});
        button("button.remove_layer",left+2*(cw+4),56,cw,()->{if(effect.layers.size()>1){effect.layers.remove(layer);layer=Math.min(layer,effect.layers.size()-1);dirty=true;refresh();}else status=msg("status.keep_layer");});
        int count=Math.max(1,(bodyBottom-112)/23);page=Math.min(page,Math.max(0,(effect.layers.size()-1)/count));
        for(int i=0;i<count&&page*count+i<effect.layers.size();i++){int index=page*count+i;ParticleLayer l=effect.layers.get(index);
            button(tr("label.layer_item",index+1,EditorI18n.name("shape",l.shape.type),EditorI18n.name("particle",l.particle)),left,84+i*23,panelWidth,()->{layer=index;tab=2;page=0;refresh();});
        }
        pager(count,effect.layers.size(),bodyBottom-24);
    }
    private void parameters(){
        ParticleLayer l=selected();int cw=(panelWidth-8)/3;
        button("button.particle",left,56,cw,()->{if(commit()){
            List<String> choices=Registries.PARTICLE_TYPE.getIds().stream().filter(id->id.getNamespace().equals("minecraft")&&Registries.PARTICLE_TYPE.get(id) instanceof SimpleParticleType).map(Object::toString).sorted().toList();
            client.setScreen(new ValuePickerScreen(this,"picker.particle_title","particle",choices,value->{l.particle=value;dirty=true;}));
        }}).setTooltip(Tooltip.of(Text.literal(l.particle)));
        button("button.shape",left+cw+4,56,cw,()->{if(commit())client.setScreen(new ValuePickerScreen(this,"picker.shape_title","shape",ShapeRegistry.ids().stream().filter(id->!id.equals("custom")||!l.shape.vertices.isEmpty()).sorted().toList(),value->{l.shape.type=value;if(!value.equals("custom"))l.shape.vertices.clear();dirty=true;}));});
        button("button.mode",left+2*(cw+4),56,cw,()->{if(commit()){effect.mode=PlaybackMode.values()[(effect.mode.ordinal()+1)%3];dirty=true;refresh();}});
        List<Input> inputs=inputs(l);int rows=Math.max(1,(bodyBottom-117)/34),count=rows*2;page=Math.min(page,Math.max(0,(inputs.size()-1)/count));
        int fw=(panelWidth-8)/2;
        for(int i=0;i<count&&page*count+i<inputs.size();i++){Input input=inputs.get(page*count+i);int x=left+(i%2)*(fw+8),y=92+(i/2)*34;
            TextFieldWidget field=new TextFieldWidget(textRenderer,x,y,fw,18,tr(input.label));field.setMaxLength(input.label.equals("label.animations_json")?30000:128);field.setText(input.value);
            field.setChangedListener(v->dirty=true);addDrawableChild(field);fields.add(new Field(input,field));
        }
        pager(count,inputs.size(),bodyBottom-24);
    }
    private List<Input> inputs(ParticleLayer l){
        List<Input> result=new ArrayList<>();
        result.add(new Input("label.effect_name",effect.name,v->effect.name=v));
        result.add(new Input("label.duration",""+effect.durationTicks,v->effect.durationTicks=Integer.parseInt(v)));
        result.add(new Input("label.view_distance",""+effect.viewDistance,v->effect.viewDistance=Double.parseDouble(v)));
        result.add(new Input("label.count",""+l.count,v->l.count=Integer.parseInt(v)));
        result.add(new Input("label.density",""+l.density,v->l.density=Double.parseDouble(v)));
        result.add(new Input("label.speed",""+l.speed,v->l.speed=Double.parseDouble(v)));
        result.add(new Input("label.emission_lifetime",""+l.lifetime,v->l.lifetime=Integer.parseInt(v)));
        result.add(new Input("label.delay",""+l.delay,v->l.delay=Integer.parseInt(v)));
        result.add(new Input("label.interval",""+l.intervalTicks,v->l.intervalTicks=Integer.parseInt(v)));
        for(int i=0;i<3;i++){int axis=i;String a="XYZ".substring(i,i+1);
            result.add(new Input("label.offset."+a,component(l.offset,i),v->l.offset=replace(l.offset,axis,Double.parseDouble(v))));
            result.add(new Input("label.rotation."+a,component(l.rotation,i),v->l.rotation=replace(l.rotation,axis,Double.parseDouble(v))));
            result.add(new Input("label.scale."+a,component(l.scale,i),v->l.scale=replace(l.scale,axis,Double.parseDouble(v))));
        }
        result.add(new Input("label.radius",""+l.shape.radius,v->l.shape.radius=Double.parseDouble(v)));
        result.add(new Input("label.inner_radius",""+l.shape.innerRadius,v->l.shape.innerRadius=Double.parseDouble(v)));
        result.add(new Input("label.height",""+l.shape.height,v->l.shape.height=Double.parseDouble(v)));
        result.add(new Input("label.width",""+l.shape.width,v->l.shape.width=Double.parseDouble(v)));
        result.add(new Input("label.length",""+l.shape.length,v->l.shape.length=Double.parseDouble(v)));
        result.add(new Input("label.turns",""+l.shape.turns,v->l.shape.turns=Double.parseDouble(v)));
        result.add(new Input("label.star_points",""+l.shape.points,v->l.shape.points=Integer.parseInt(v)));
        String animationJson=new com.google.gson.Gson().toJson(l.animations);
        result.add(new Input("label.animations_json",animationJson,v->{Animation[] a=JsonSupport.GSON.fromJson(v,Animation[].class);l.animations=new ArrayList<>(Arrays.asList(a));}));
        return result;
    }
    private static String component(Vec3 v,int i){return Double.toString(i==0?v.x():i==1?v.y():v.z());}
    private static Vec3 replace(Vec3 v,int i,double value){return new Vec3(i==0?value:v.x(),i==1?value:v.y(),i==2?value:v.z());}
    private void toolsPane(){
        ParticleLayer l=selected();int cw=(panelWidth-4)/2;
        button("button.draw",left,56,cw,()->{if(commit())client.setScreen(new DrawingScreen(this,l.shape.type.equals("custom")?l.shape.vertices:List.of(),points->{
            l.shape.type="custom";l.shape.vertices=new ArrayList<>(points);l.count=points.size();dirty=true;
        }));});
        button("button.formula",left+cw+4,56,cw,()->{if(commit())client.setScreen(new FormulaScreen(this,points->{
            l.shape.type="custom";l.shape.vertices=new ArrayList<>(points);l.count=points.size();dirty=true;
        }));}).active=EditorI18n.tier()!=EditorI18n.Tier.SIMPLE;
    }
    private void previewPane(){
        action(()->preview=new PreviewModel(effect));int cw=(panelWidth-16)/5;
        button("button.restart",left,56,cw,()->action(()->preview=new PreviewModel(effect)));
        button("button.zoom_in",left+cw+4,56,cw,()->zoom=Math.min(160,zoom*1.25));
        button("button.zoom_out",left+2*(cw+4),56,cw,()->zoom=Math.max(1,zoom/1.25));
        button("button.rotate_left",left+3*(cw+4),56,cw,()->yaw-=.2);
        button("button.rotate_right",left+4*(cw+4),56,cw,()->yaw+=.2);
    }
    private void pager(int count,int total,int y){
        button("button.previous",left,y,36,()->{if(commit()){page=Math.max(0,page-1);refresh();}}).active=page>0;
        button("button.next",left+40,y,36,()->{if(commit()){page++;refresh();}}).active=(page+1)*count<total;
    }
    private ButtonWidget button(String key,int x,int y,int w,Runnable click){
        Text label=key.equals("button.mode")?tr(key,EditorI18n.msg("mode."+effect.mode.name().toLowerCase(Locale.ROOT))):
            key.equals("button.shape")?tr(key,EditorI18n.name("shape",effect.layers.get(layer).shape.type)):
            key.equals("button.particle")?tr(key,EditorI18n.name("particle",effect.layers.get(layer).particle)):tr(key);
        return button(label,x,y,w,click);
    }
    private ButtonWidget button(Text label,int x,int y,int w,Runnable click){
        ButtonWidget b=ButtonWidget.builder(Text.literal(textRenderer.trimToWidth(label.getString(),Math.max(12,w-8))),button->click.run()).dimensions(x,y,Math.max(20,w),20).build();
        b.setTooltip(Tooltip.of(label));addDrawableChild(b);return b;
    }
    private ParticleLayer selected(){layer=Math.min(layer,effect.layers.size()-1);return effect.layers.get(layer);}
    private boolean commit(){
        ParticleEffect backup=JsonSupport.copy(effect);
        try{for(Field f:fields)f.input.write.accept(f.widget.getText().trim());EffectValidator.validate(effect);return true;}
        catch(Exception e){effect=backup;status=msg("status.invalid_input",Objects.toString(e.getMessage(),msg("status.check_values")));refresh();return false;}
    }
    private void discardThen(Runnable task){
        if(!dirty){task.run();return;}
        client.setScreen(new ConfirmScreen(yes->{client.setScreen(this);if(yes){dirty=false;task.run();}},tr("confirm.discard.title"),tr("confirm.discard.body",effect.name)));
    }
    private void action(Runnable action){try{action.run();}catch(Exception e){status=Objects.toString(e.getMessage(),e.getClass().getSimpleName());}}
    private boolean connected(){return client!=null&&client.getNetworkHandler()!=null&&ClientPlayNetworking.canSend(EditorRequest.ID);}
    private void request(String action,String name,String json){
        if(!connected()){status=msg("status.server_channel_missing");return;}
        ClientPlayNetworking.send(new EditorRequest(action,name,json));status=msg("status.request",action);
    }
    public void receive(EditorResponse response){
        action(()->{switch(response.action()){
            case "list"->{String[] list=JsonSupport.GSON.fromJson(response.content(),String[].class);names=new ArrayList<>(Arrays.asList(list));Collections.sort(names);status=msg("status.server_effects",names.size());if(tab==0)refresh();}
            case "effect"->{ParticleEffect received=JsonSupport.GSON.fromJson(response.content(),ParticleEffect.class);EffectValidator.validate(received);effect=received;layer=0;tab=2;page=0;dirty=false;status=msg("status.loaded",effect.name);refresh();}
            case "saved"->{dirty=false;if(!names.contains(response.content())){names.add(response.content());Collections.sort(names);}status=msg("status.saved",response.content());}
            case "deleted"->{names.remove(response.content());status=msg("status.deleted",response.content());dirty=false;tab=0;page=0;refresh();}
            default->status=localizeServerMessage(response.content());
        }});
    }
    private void refresh(){clearChildren();init();}
    void languageChanged(){refresh();}
    public void tick(){if(tab==4&&preview!=null)preview.tick();}
    public void render(DrawContext ctx,int mouseX,int mouseY,float delta){
        ctx.drawCenteredTextWithShadow(textRenderer,EditorI18n.tr("screen.title"),width/2,8,0xFFFFFFFF);
        String heading=tr("label.heading",effect.name+(dirty?" *":""),layer+1,effect.layers.size()).getString();
        ctx.drawCenteredTextWithShadow(textRenderer,textRenderer.trimToWidth(heading,panelWidth),width/2,20,0xFFCCD0D4);
        if(tab==2){int fw=(panelWidth-8)/2;for(int i=0;i<fields.size();i++)ctx.drawTextWithShadow(textRenderer,textRenderer.trimToWidth(tr(fields.get(i).input.label).getString(),fw),left+(i%2)*(fw+8),82+(i/2)*34,0xFFE0E0E0);}
        if(tab==3){
            ParticleLayer l=selected();ctx.drawTextWithShadow(textRenderer,tr("label.tier",EditorI18n.msg("tier."+EditorI18n.tier().name().toLowerCase(Locale.ROOT))),left,88,0xFFCCD0D4);
            ctx.drawTextWithShadow(textRenderer,tr("label.current_shape",EditorI18n.name("shape",l.shape.type)),left,108,0xFFE0E0E0);
            ctx.drawTextWithShadow(textRenderer,tr("label.points",l.shape.vertices.size(),EffectValidator.MAX_CUSTOM_POINTS),left,128,0xFFE0E0E0);
            ctx.drawTextWithShadow(textRenderer,textRenderer.trimToWidth(msg("label.tools_help"),panelWidth),left,153,0xFFBCCFC6);
        }
        if(tab==4&&preview!=null){
            int top=82,bottom=bodyBottom-4,cx=width/2,cy=(top+bottom)/2;
            ctx.fill(left,top,left+panelWidth,bottom,0xFF18221E);
            ctx.fill(left,cy,left+panelWidth,cy+1,0xFF35463A);ctx.fill(cx,top,cx+1,bottom,0xFF35463A);
            double c=Math.cos(yaw),s=Math.sin(yaw);
            preview.visit(false,(l,x,y,z,index)->{double px=x*c-z*s,pz=x*s+z*c;
                int sx=(int)(cx+px*zoom),sy=(int)(cy+(-y*.85+pz*.4)*zoom);
                if(sx>=left+1&&sx<left+panelWidth-2&&sy>=top+1&&sy<bottom-2)ctx.fill(sx,sy,sx+2,sy+2,COLORS[index%COLORS.length]);
            });
        }
        super.render(ctx,mouseX,mouseY,delta);
        ctx.drawTextWithShadow(textRenderer,textRenderer.trimToWidth(status,panelWidth),left,height-17,0xFFFFD166);
    }
    @Override public void renderBackground(DrawContext ctx,int mouseX,int mouseY,float delta) {
        ctx.fill(0,0,width,height,0xFF101512);
    }
    public void close(){if(!commit())return;discardThen(()->client.setScreen(parent));}
    public boolean shouldPause(){return false;}
    private static Text tr(String key,Object... args){return EditorI18n.tr(key,args);}
    private static String msg(String key,Object... args){return EditorI18n.msg(key,args);}
    private static String localizeServerMessage(String message){
        if(message.startsWith("Missing permission: "))return msg("status.permission",message.substring("Missing permission: ".length()));
        if(message.startsWith("Unknown effect: "))return msg("status.unknown_effect",message.substring("Unknown effect: ".length()));
        if(message.equals("Effect already exists"))return msg("status.effect_exists");
        if(message.startsWith("Name must be "))return msg("status.invalid_name");
        if(message.startsWith("JSON exceeds "))return msg("status.json_too_large");
        return message;
    }
}
