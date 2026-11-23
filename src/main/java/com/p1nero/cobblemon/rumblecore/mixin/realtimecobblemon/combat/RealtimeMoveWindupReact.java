package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.johnn.realtimecobblemon.Smash;
import com.johnn.realtimecobblemon.moves.React;
import com.p1nero.cobblemon.rumblecore.combat.RealtimeMoveWindup;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = React.class, remap = false)
abstract class RealtimeMoveWindupReact {
    @Inject(method = "lift", at = @At("HEAD"), cancellable = true)
    private static void rumble$preventWindupLift(Smash.Session source, LivingEntity target, CallbackInfo ci) {
        if (RealtimeMoveWindup.locked(target)) ci.cancel();
    }

    // RTC applies a direct displacement here; the player branch never sets Conditions.juggle.
    @Inject(method = "lift", at = @At("RETURN"))
    private static void rumble$trackHitLift(Smash.Session source, LivingEntity target, CallbackInfo ci) {
        RealtimeMoveWindup.recordKnockback(target, 6, true);
    }
}
