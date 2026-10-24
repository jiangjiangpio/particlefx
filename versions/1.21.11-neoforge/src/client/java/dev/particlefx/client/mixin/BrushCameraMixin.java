package dev.particlefx.client.mixin;

import dev.particlefx.client.gui.Freehand3DScreen;
import net.minecraft.client.Camera;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class BrushCameraMixin {
    @Shadow protected abstract void setPosition(net.minecraft.world.phys.Vec3 pos);
    @Shadow protected abstract void setRotation(float yaw,float pitch);
    @Inject(method="setup",at=@At("TAIL"))
    private void particlefx$brushCamera(CallbackInfo ci) {
        Freehand3DScreen brush=Freehand3DScreen.active();
        if(brush!=null){brush.updateCamera();setPosition(brush.cameraPosition());setRotation(brush.cameraYaw(),brush.cameraPitch());}
    }
}

