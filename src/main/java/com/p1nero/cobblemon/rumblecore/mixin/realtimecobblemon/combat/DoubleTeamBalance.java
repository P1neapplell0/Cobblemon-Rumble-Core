package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.johnn.realtimecobblemon.Smash;
import com.johnn.realtimecobblemon.moves.Decoys;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Decoys.class, remap = false)
abstract class DoubleTeamBalance {
    @Inject(method = "spawn", at = @At("HEAD"), cancellable = true)
    private static void rumble$oneBatchAtATime(Smash.Session session, int count, CallbackInfoReturnable<Integer> result) {
        // Recasting cannot add to the wave or extend it, even after one copy has been hit.
        if (session.combat.decoys.stream().anyMatch(PokemonEntity::isAlive)) result.setReturnValue(0);
    }

    @ModifyVariable(method = "spawn", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static int rumble$limitCopies(int count) { return Math.min(count, 2); }

    @ModifyConstant(method = "spawn", constant = @Constant(longValue = 600L))
    private static long rumble$shortLivedCopies(long ticks) { return 160L; }

    @ModifyExpressionValue(method = "strike", at = @At(value = "INVOKE", target = "Ljava/lang/Double;doubleValue()D"))
    private static double rumble$reduceStrikePower(double power) { return Math.min(power, 3.0); }

    @ModifyVariable(method = "absorb", at = @At("STORE"), ordinal = 0)
    private static double rumble$reduceAbsorbChance(double oldChance, Smash.Session session) {
        // RTC has already pruned dead copies here. Its normal hit/popping path is unchanged.
        return Math.min(.30, .15 * session.combat.decoys.size());
    }
}
