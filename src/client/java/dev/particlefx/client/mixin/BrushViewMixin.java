package dev.particlefx.client.mixin;

import dev.particlefx.client.gui.Freehand3DScreen;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class BrushViewMixin {
    @Inject(method="getFov",at=@At("HEAD"),cancellable=true)
    private void particlefx$brushFov(Camera camera,float tickProgress,boolean changingFov,CallbackInfoReturnable<Float> cir) {
        Freehand3DScreen brush=Freehand3DScreen.active();
        if(brush!=null)cir.setReturnValue(brush.cameraFov());
    }
    // Cursor projection requires a stable view without player bob, hurt tilt or held items.
    @Inject(method={"bobView","tiltViewWhenHurt","renderHand"},at=@At("HEAD"),cancellable=true)
    private void particlefx$stableBrushView(CallbackInfo ci) {
        if(Freehand3DScreen.active()!=null)ci.cancel();
    }
}
