package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.johnn.realtimecobblemon.moves.Motion;
import com.p1nero.cobblemon.rumblecore.combat.RealtimeMoveWindup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Motion.class, remap = false)
abstract class RealtimeMoveWindupMotion {
    @Inject(method = "knock", at = @At("HEAD"), cancellable = true)
    private static void rumble$preventWindupKnockback(LivingEntity target, Vec3 velocity, CallbackInfo ci) {
        if (RealtimeMoveWindup.locked(target)) ci.cancel();
    }

    @Inject(method = "knock", at = @At("RETURN"))
    private static void rumble$trackHitKnockback(LivingEntity target, Vec3 velocity, CallbackInfo ci) {
        if (velocity.lengthSqr() > 0.0) RealtimeMoveWindup.recordKnockback(target, 5, velocity.y > 0.0);
    }
}
