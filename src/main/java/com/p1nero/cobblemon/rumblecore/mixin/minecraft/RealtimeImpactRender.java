package com.p1nero.cobblemon.rumblecore.mixin.minecraft;

import com.p1nero.cobblemon.rumblecore.client.combat.RealtimeImpactCamera;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
abstract class RealtimeImpactRender {
    @Shadow @Final private Camera mainCamera;

    // RTC's shoulder/duel camera replaces the rotation at the tail of Camera.setup,
    // after ComputeCameraAngles. Apply the impulse once to its finished pose.
    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/Camera;setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V",
            shift = At.Shift.AFTER))
    private void rumble$applyImpact(DeltaTracker delta, CallbackInfo ci) {
        var offset = RealtimeImpactCamera.offset(mainCamera);
        if (offset != null) ((KnockoutCamera.RotationAccess) mainCamera).rumble$setRenderRotation(
                mainCamera.getYRot() + offset.yaw(), mainCamera.getXRot() + offset.pitch(), mainCamera.getRoll() + offset.roll());
    }
}
