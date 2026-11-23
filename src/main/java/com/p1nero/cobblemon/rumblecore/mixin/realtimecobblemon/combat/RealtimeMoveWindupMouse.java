package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.p1nero.cobblemon.rumblecore.client.combat.RealtimeMoveWindupClient;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
abstract class RealtimeMoveWindupMouse {
    @Shadow private double accumulatedDX;
    @Shadow private double accumulatedDY;

    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void rumble$lockLook(double partialTick, CallbackInfo ci) {
        if (RealtimeMoveWindupClient.active()) {
            accumulatedDX = accumulatedDY = 0;
            ci.cancel();
        }
    }
}
