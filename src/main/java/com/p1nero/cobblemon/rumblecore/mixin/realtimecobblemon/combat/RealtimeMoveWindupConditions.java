package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.johnn.realtimecobblemon.moves.Conditions;
import com.p1nero.cobblemon.rumblecore.combat.RealtimeMoveWindup;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Conditions.class, remap = false)
abstract class RealtimeMoveWindupConditions {
    @Inject(method = {"applyStatus", "applyVolatile"}, at = @At("HEAD"), cancellable = true)
    private static void rumble$preventWindupControl(LivingEntity target, String effect,
            com.johnn.realtimecobblemon.Smash.Session source, CallbackInfoReturnable<Boolean> cir) {
        if (RealtimeMoveWindup.locked(target)) cir.setReturnValue(false);
    }

    @Inject(method = {"stun", "juggle", "launch"}, at = @At("HEAD"), cancellable = true)
    private static void rumble$preventWindupHitControl(LivingEntity target, int ticks, CallbackInfo ci) {
        if (RealtimeMoveWindup.locked(target)) ci.cancel();
    }
}
