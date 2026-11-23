package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.johnn.realtimecobblemon.Smash;
import com.johnn.realtimecobblemon.moves.Conditions;
import com.p1nero.cobblemon.rumblecore.combat.arena.RealtimeProtection;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Conditions.class, remap = false)
abstract class ControlRecovery {
    @Inject(method = {"applyStatus", "applyVolatile"}, at = @At("HEAD"), cancellable = true)
    private static void rumble$protect(LivingEntity target, String effect, Smash.Session source, CallbackInfoReturnable<Boolean> cir) {
        if (RealtimeProtection.rejects(target, effect)) cir.setReturnValue(false);
    }

    @Inject(method = {"applyStatus", "applyVolatile"}, at = @At("RETURN"))
    private static void rumble$applied(LivingEntity target, String effect, Smash.Session source, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) RealtimeProtection.applied(target, effect);
    }

    @Inject(method = "bind", at = @At("HEAD"), cancellable = true)
    private static void rumble$preventBind(LivingEntity target, int ticks, CallbackInfo ci) {
        if (RealtimeProtection.rejects(target, "bind")) ci.cancel();
    }

    @Inject(method = "bind", at = @At("RETURN"))
    private static void rumble$recordBind(LivingEntity target, int ticks, CallbackInfo ci) {
        RealtimeProtection.applied(target, "bind");
    }
}
