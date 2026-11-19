package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "com.johnn.realtimecobblemon.moves.Decoys$Swarm", remap = false)
abstract class DoubleTeamStrikeCadence {
    @ModifyExpressionValue(method = "step", at = @At(value = "INVOKE", target = "Ljava/lang/Integer;intValue()I"))
    private static int rumble$spaceOutStrikes(int ticks) { return Math.max(50, ticks); }
}
