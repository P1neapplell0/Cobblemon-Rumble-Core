package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.johnn.realtimecobblemon.Smash;
import com.p1nero.cobblemon.rumblecore.combat.RealtimeMoveWindup;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Smash.class, remap = false)
abstract class RealtimeMoveWindupDamage {
    @Inject(method = "hurtPokemon", at = @At("HEAD"), cancellable = true)
    private static void rumble$preventMoveWindupDamage(Smash.Session session, float damage, DamageSource source, CallbackInfo ci) {
        if (RealtimeMoveWindup.locked(session.pokemon)) ci.cancel();
    }

    @Inject(method = "receive", at = @At("HEAD"), cancellable = true)
    private static void rumble$preventHitReactions(Smash.Session session, DamageSource source, float damage,
            CallbackInfoReturnable<Float> cir) {
        if (RealtimeMoveWindup.locked(session.pokemon)) cir.setReturnValue(0.0F);
    }
}
