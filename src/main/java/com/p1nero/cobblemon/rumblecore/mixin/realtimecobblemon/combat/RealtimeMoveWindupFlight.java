package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.johnn.realtimecobblemon.Smash;
import com.johnn.realtimecobblemon.moves.Flight;
import com.p1nero.cobblemon.rumblecore.combat.RealtimeMoveWindup;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Flight.class, remap = false)
abstract class RealtimeMoveWindupFlight {
    // Only a hit that actually forces the flyer down, not ordinary flight or stamina exhaustion.
    @Inject(method = "hit", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/server/level/ServerPlayer;push(DDD)V", shift = At.Shift.AFTER))
    private static void rumble$trackFlightKnockdown(Smash.Session session, DamageSource source, CallbackInfo ci) {
        RealtimeMoveWindup.recordKnockback(session.player, 5, true);
    }
}
