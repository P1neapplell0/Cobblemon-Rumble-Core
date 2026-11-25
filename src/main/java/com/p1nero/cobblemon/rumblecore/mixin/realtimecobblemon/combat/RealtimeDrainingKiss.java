package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.johnn.realtimecobblemon.moves.Fighting;
import com.johnn.realtimecobblemon.moves.MoveInfo;
import com.johnn.realtimecobblemon.moves.MoveRegistry;
import com.johnn.realtimecobblemon.moves.MoveStyle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Resolves the RTC 1.0.3alpha.1 draining-kiss role conflict for the UI/runtime.
 *
 * Maintenance: re-check Fighting.role, MoveRegistry.style and the move id after
 * an RTC update; remove this mixin if upstream makes the roles unambiguous.
 */
@Mixin(value = Fighting.class, remap = false)
abstract class RealtimeDrainingKiss {
    @Inject(method = "role", at = @At("RETURN"), cancellable = true)
    private static void rumble$honorDrainingKissBeam(MoveInfo move, CallbackInfoReturnable<Fighting.Role> cir) {
        // RTC 1.0.3alpha.1 defines this move as both beam and grab. Only correct that conflict.
        if (cir.getReturnValue() == Fighting.Role.GRAB && move != null && move.id().equals("drainingkiss")
                && MoveRegistry.style(move).shape() == MoveStyle.Shape.BEAM) {
            cir.setReturnValue(Fighting.Role.STRIKE);
        }
    }
}
