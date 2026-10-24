package dev.particlefx.client.mixin;

import dev.particlefx.client.gui.Freehand3DScreen;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class BrushCameraMixin {
    @Shadow protected abstract void setPos(Vec3d pos);
    @Shadow protected abstract void setRotation(float yaw,float pitch);
    @Inject(method="update",at=@At("TAIL"))
    private void particlefx$brushCamera(CallbackInfo ci) {
        Freehand3DScreen brush=Freehand3DScreen.active();
        if(brush!=null){brush.updateCamera();setPos(brush.cameraPosition());setRotation(brush.cameraYaw(),brush.cameraPitch());}
    }
}
