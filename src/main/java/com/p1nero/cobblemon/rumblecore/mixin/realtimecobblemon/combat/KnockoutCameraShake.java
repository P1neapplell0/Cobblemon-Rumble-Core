package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.p1nero.cobblemon.rumblecore.client.combat.FrontierKnockoutOverlay;
import com.p1nero.cobblemon.rumblecore.client.combat.RealtimeImpactCamera;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = "com.johnn.realtimecobblemon.client.ClientSmash", remap = false)
abstract class KnockoutCameraShake {
    @WrapMethod(method = "onCamera")
    private static void rumble$softenKnockoutShake(ViewportEvent.ComputeCameraAngles event, Operation<Void> original) {
        float scale = RealtimeImpactCamera.cinematic() ? Math.min(.15F, FrontierKnockoutOverlay.battleShakeScale()) : 1;
        float yaw = event.getYaw(), pitch = event.getPitch(), roll = event.getRoll();
        original.call(event);
        if (scale != 1) {
            event.setYaw(Mth.lerp(scale, yaw, event.getYaw()));
            event.setPitch(Mth.lerp(scale, pitch, event.getPitch()));
            event.setRoll(Mth.lerp(scale, roll, event.getRoll()));
        }
    }
}
