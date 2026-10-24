package dev.particlefx.client.mixin;

import dev.particlefx.client.gui.Freehand3DScreen;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class BrushViewMixin {
    // Cursor projection requires a stable view without player bob, hurt tilt or held items.
    @Inject(method={"bobView","bobHurt","renderItemInHand"},at=@At("HEAD"),cancellable=true)
    private void particlefx$stableBrushView(CallbackInfo ci) {
        if(Freehand3DScreen.active()!=null)ci.cancel();
    }
}


