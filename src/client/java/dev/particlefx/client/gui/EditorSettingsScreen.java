package dev.particlefx.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;

public final class EditorSettingsScreen extends Screen {
    private final Screen parent;
    public EditorSettingsScreen(Screen parent){super(EditorI18n.tr("screen.settings"));this.parent=parent;}
    @Override protected void init() {
        int w=Math.min(360,width-24),x=(width-w)/2;
        addDrawableChild(ButtonWidget.builder(EditorI18n.tr("button.language",EditorI18n.languageLabel()),
            b->{EditorI18n.LanguageMode[] modes=EditorI18n.LanguageMode.values();
                EditorI18n.setLanguageMode(modes[(EditorI18n.languageMode().ordinal()+1)%modes.length]);clearAndInit();})
            .dimensions(x,60,w,20).build());
        addDrawableChild(ButtonWidget.builder(EditorI18n.tr("button.tier",EditorI18n.msg("tier."+EditorI18n.tier().name().toLowerCase())),
            b->{EditorI18n.Tier next=EditorI18n.Tier.values()[(EditorI18n.tier().ordinal()+1)%EditorI18n.Tier.values().length];EditorI18n.setTier(next);clearAndInit();})
            .dimensions(x,86,w,20).build());
        addDrawableChild(ButtonWidget.builder(EditorI18n.tr("button.back"),b->back()).dimensions(x,118,w,20).build());
    }
    private void back() {
        if(parent instanceof EffectEditorScreen editor)editor.languageChanged();
        client.setScreen(parent);
    }
    @Override public void render(DrawContext ctx,int mouseX,int mouseY,float delta) {
        ctx.drawCenteredTextWithShadow(textRenderer,EditorI18n.tr("screen.settings"),width/2,25,0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer,EditorI18n.tr("label.settings_help"),width/2,42,0xFFCCD0D4);
        super.render(ctx,mouseX,mouseY,delta);
    }
    @Override public void renderBackground(DrawContext ctx,int mouseX,int mouseY,float delta){ctx.fill(0,0,width,height,0xFF101512);}
    @Override public void close(){back();}
    @Override public boolean shouldPause(){return false;}
}
